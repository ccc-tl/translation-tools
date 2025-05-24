package tl.extra.patcher

import com.google.common.hash.Hashing
import kio.util.child
import kio.util.padArray
import kio.util.readJson
import kio.util.relativizePath
import kio.util.writeJson
import tl.extra.file.CmpFile
import tl.extra.file.PakFile
import tl.extra.file.PakFileEntry
import tl.extra.util.PakReferenceMap
import java.io.File

class PakUpdater(
  private val cpkExtract: File,
  private val auxPatchBytes: ByteArray,
  private val pakRefs: PakReferenceMap,
  outDir: File,
  private val pakReplacements: Map<String, File>,
) {
  private val paksOutDir = outDir.child("patched")
  private val hashFile = outDir.child("hash.json")
  private val updateHashes by lazy {
    if (hashFile.exists()) hashFile.readJson() else mutableMapOf<String, ByteArray>()
  }

  fun buildPaks() {
    paksOutDir.mkdir()
    val pakUpdates = getPakUpdates()
    getExistingPatchedPaks()
      .filterNot { pakUpdates.contains(it.key) }
      .forEach { (relPath, file) ->
        println("Remove no longer modified PAK $relPath")
        if (!file.delete()) {
          error("Unable to delete old PAK")
        }
      }
    pakUpdates.forEach { (relPath, update) ->
      val lastHash = updateHashes[relPath]
      val currentHash = update.contentHash()
      if (lastHash != null && lastHash.contentEquals(currentHash) && !relPath.contains("PRELOAD.pak")) {
        println("PAK $relPath is up to date")
        return@forEach
      }
      applyPakUpdate(relPath, update.updates)
      updateHashes[relPath] = currentHash
    }
    hashFile.writeJson(updateHashes)
  }

  private fun applyPakUpdate(relPath: String, updates: List<PakFileUpdate>) {
    val sourcePakFile = cpkExtract.child(relPath)
    val sourcePakBytes = if (sourcePakFile.extension == "cmp") {
      CmpFile(sourcePakFile).getData()
    } else {
      sourcePakFile.readBytes()
    }
    val newEntries = PakFile(sourcePakBytes).entries.toMutableList()
    updates.forEach { update ->
      val entryIndex = newEntries.indexOfFirst { it.path == update.relPath }
      if (entryIndex == -1) {
        error("Failed to replace entry ${update.relPath} in PAK $relPath")
      }
      val oldEntry = newEntries[entryIndex]
      newEntries[entryIndex] = PakFileEntry(oldEntry.index, oldEntry.path, padArray(update.updateWith.readBytes()))
    }
    if (sourcePakFile.name == "PRELOAD.pak") {
      println("Adding aux entry to PRELOAD.pak")
      newEntries.add(
        PakFileEntry(
          newEntries.last().index + 1,
          "hax/patch.o",
          padArray(auxPatchBytes),
        ),
      )
    }
    println("Writing modified PAK $relPath")
    val outFile = paksOutDir.child(relPath)
    outFile.parentFile.mkdirs()
    PakWriter(newEntries, true).writeTo(outFile)
  }

  private fun getPakUpdates(): Map<String, PakUpdate> {
    println("Building PAK update list...")
    val pakUpdates = mutableMapOf<String, PakUpdate>()
    pakReplacements.forEach { (relPath, updateWith) ->
      (pakRefs.files[relPath] ?: error("Missing PAK for $relPath")).forEach { pakRelPath ->
        pakUpdates.getOrPut(pakRelPath) { PakUpdate() }
          .addUpdate(relPath, updateWith)
      }
    }
    return pakUpdates
  }

  private fun getExistingPatchedPaks(): Map<String, File> {
    return paksOutDir.walk()
      .toList()
      .filter { it.isFile }
      .associateBy({ it.relativizePath(paksOutDir) }, { it })
  }
}

private class PakUpdate {
  val updates = mutableListOf<PakFileUpdate>()

  @Suppress("UnstableApiUsage")
  fun contentHash(): ByteArray {
    val hashSize = updates.sumOf { it.updateWith.length().toInt() }
    val hasher = Hashing.murmur3_128().newHasher(hashSize)
    updates.forEach { hasher.putBytes(it.updateWith.readBytes()) }
    return hasher.hash().asBytes()
  }

  fun addUpdate(relPath: String, updateWith: File) {
    updates.add(PakFileUpdate(relPath, updateWith))
  }
}

private data class PakFileUpdate(val relPath: String, val updateWith: File)
