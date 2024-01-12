package tl.extra.patcher

import kio.LERandomAccessFile
import kio.util.WINDOWS_932
import kio.util.child
import kio.util.getSubArrayPos
import kio.util.nullStreamHandler
import kio.util.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tl.extra.file.PakFile
import tl.extra.file.getDataLocationInFile
import tl.extra.file.getEntry
import tl.file.CpkFile
import tl.file.CpkFileDescriptor
import tl.file.decompressLayla
import tl.util.createFinePatch
import java.io.File
import java.nio.charset.Charset

class NameReplacer(
  private val outCpkFile: File,
  private val fineTool: File,
  private val patchesDirectory: File,
  private val charset: Charset = Charsets.WINDOWS_932,
) {
  private val descriptors = CpkFile(outCpkFile).getFileDescriptors()
    .filter { it.relPath.startsWith("day/") || it.relPath.startsWith("pack/") }
  private val replacementMap = mutableMapOf<String, NameReplacement>()

  init {
    patchesDirectory.mkdir()
  }

  fun processReplacementGroup(
    groupId: String,
    patterns: List<NameReplacementPattern>,
    pakFiles: List<NameReplacementPakFile> = emptyList(),
  ) {
    if (replacementMap.containsKey(groupId)) {
      error("Replacement group $groupId was already processed")
    }
    val patternResults = mutableListOf<NameReplacementPatternResults>()
    val cpkFile = LERandomAccessFile(outCpkFile, "r")
    val cpkMutex = Mutex()

    println("Creating replacements map for $groupId...")
    patterns.forEach { pattern ->
      println("Processing replacement pattern: '${pattern.searchFor}' -> '${pattern.replaceWith}'...")
      val searchForBytes = pattern.searchFor.toByteArray(charset)
      val replaceWithBytes = pattern.replaceWith.toByteArray(charset)
      if (searchForBytes.size != replaceWithBytes.size) {
        error("Pattern length mismatch, patterns must have same length!")
      }
      val patternOffsets = mutableListOf<Long>()

      runBlocking {
        descriptors
          .filter(pattern.descriptorFilter)
          .map { descriptor ->
            async(Dispatchers.Default) {
              println("Scanning file ${descriptor.relPath}...")
              val compressed = descriptor.size != descriptor.extractSize
              val cpkBytes = cpkMutex.withLock {
                cpkFile.seek(descriptor.offset)
                cpkFile.readBytes(descriptor.size)
              }
              val bytes = if (compressed) {
                decompressLayla(cpkBytes, descriptor.extractSize)
              } else {
                cpkBytes
              }
              val offsets = mutableListOf<Long>()
              var offset = getSubArrayPos(bytes, searchForBytes)
              while (offset != -1) {
                offsets.add(descriptor.offset + offset)
                offset = getSubArrayPos(bytes, searchForBytes, offset + 1)
              }
              if (offsets.isNotEmpty() && compressed) {
                error("Target file for name replacement is still compressed: ${descriptor.relPath}")
              }
              println("Found ${offsets.size} offsets in ${descriptor.relPath}")
              offsets
            }
          }
          .map { it.await() }
          .map { offsets -> patternOffsets.addAll(offsets) }
      }

      if (patternOffsets.size == 0) {
        error("Pattern produced 0 replacement results. This is most likely mistake in the configuration or the text has changed.")
      }
      patternResults.add(NameReplacementPatternResults(pattern.replaceWith, patternOffsets.sorted()))
    }

    val pakFileResults = mutableListOf<NameReplacementPakFileResults>()
    val groupIdDir = patchesDirectory.child(groupId).apply { mkdir() }
    pakFiles.forEach { replacementFile ->
      descriptors
        .filter { it.relPath == replacementFile.relPath }
        .forEach { descriptor ->
          println("Processing replacement file descriptor ${descriptor.relPath}...")
          val compressed = descriptor.size != descriptor.extractSize
          if (compressed) {
            error("Replacement PAK file is compressed in CPK.")
          }
          cpkFile.seek(descriptor.offset)
          val pakBytes = cpkFile.readBytes(descriptor.size)
          val pakFile = PakFile(pakBytes)
          val pakEntries = pakFile.entries
          val pakEntryOffset = pakEntries.getDataLocationInFile(descriptor.offset, pakFile.headerSize, replacementFile.pakEntryPath)

          val oldFile = groupIdDir.child("OLD.FILE")
          val newFile = groupIdDir.child("NEW.FILE")
          val patchFile = groupIdDir.child(pakEntryOffset.toString())
          val oldBytes = pakEntries.getEntry(replacementFile.pakEntryPath).bytes
          val newBytes = replacementFile.replaceWith.readBytes()
          if (newBytes.size != oldBytes.size) {
            error("Can't replace file that has different size in PAK!")
          }
          oldFile.writeBytes(oldBytes)
          newFile.writeBytes(newBytes)
          createFinePatch(fineTool, oldFile, newFile, patchFile, nullStreamHandler())
          oldFile.delete()
          newFile.delete()
          pakFileResults.add(
            NameReplacementPakFileResults(
              pakEntryOffset, oldBytes.size,
              "patch-alt://$groupId-${patchFile.name}",
              patchFile
            )
          )
        }
    }

    replacementMap[groupId] = NameReplacement(patternResults, pakFileResults)
    cpkFile.close()
  }

  fun getReplacements(): NameReplacements = replacementMap
}

// Input

data class NameReplacementPattern(
  val searchFor: String,
  val replaceWith: String,
  val descriptorFilter: (CpkFileDescriptor) -> Boolean = { true },
)

data class NameReplacementPakFile(
  val relPath: String,
  val pakEntryPath: String,
  val replaceWith: File,
)

// Output

typealias NameReplacements = Map<String, NameReplacement>

data class NameReplacement(
  val patterns: List<NameReplacementPatternResults>,
  val pakFiles: List<NameReplacementPakFileResults>,
)

data class NameReplacementPatternResults(val replaceWith: String, val offsets: List<Long>, val clearSize: Int = replaceWith.length)

data class NameReplacementPakFileResults(
  val offset: Long,
  val size: Int,
  val patchPath: String,
  @Transient val patchFile: File,
)
