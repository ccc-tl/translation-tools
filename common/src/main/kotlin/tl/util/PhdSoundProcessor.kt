package tl.util

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.child
import kio.util.execute
import kio.util.nullStreamHandler
import kio.util.toHex
import kio.util.writeJson
import java.io.File

class PhdSoundProcessor(private val vgmstreamTool: File) {
  private val idMap = mutableMapOf<String, Map<Int, Int>>()

  fun process(srcDir: File, outDir: File, writeIdMap: Boolean = true) {
    srcDir.listFiles()?.forEach {
      processFile(it, outDir)
    }
    if (writeIdMap) {
      outDir.child("idMap.json").writeJson(idMap)
    }
  }

  private fun processFile(file: File, outDir: File) {
    if (!file.extension.equals("phd", ignoreCase = true) || file.length() == 0L) {
      return
    }
    println("Processing ${file.name}...")
    val ids = mutableMapOf<Int, Int>()
    idMap[file.nameWithoutExtension] = ids
    with(KioInputStream(file)) {
      val ppvaEntries = mutableListOf<PpvaEntry>()
      while (!eof()) {
        val sectName = readString(4, Charsets.US_ASCII)
        val sectSize = readInt()
        val sectPos = pos()
        when (sectName) {
          "PPTN" -> {
            skip(0x8)
            val startId = readInt()
            val endId = readInt()
            skip(0x8)
            val entrySubSectSize = sectSize - 0x18
            val entrySize = 0x60
            repeat(entrySubSectSize / entrySize) { index ->
              val ppvaIndex = readInt()
              ids[startId + index] = ppvaIndex
              skip(0x5C)
            }
          }
          "PPVA" -> {
            skip(0x18)
            val entrySubSectSize = sectSize - 0x18
            val entrySize = 0x10
            repeat(entrySubSectSize / entrySize) { index ->
              val entry = PpvaEntry(index, readInt(), readInt(), readInt(), readInt())
              ppvaEntries.add(entry)
            }
          }
          in arrayOf("PPHD", "PPVG", "PPPG") -> {
            // ignore
          }
          else -> {
            error("Unknown section '$sectName' at ${pos().toHex()}")
          }
        }
        setPos(sectPos + sectSize)
      }
      val pbdData = listOf(".pbd", ".PBD")
        .map { file.resolveSibling(file.nameWithoutExtension + it) }
        .firstOrNull { it.exists() }
        ?.readBytes()
        ?: error("Could not find matching PBD file")
      val pbdStream = KioInputStream(pbdData)
      ppvaEntries.forEach { entry ->
        if (entry.isUnused()) {
          return@forEach
        }
        val vagFile = outDir.child("${file.nameWithoutExtension}.${entry.index}.vag")
        val wavFile = outDir.child("${vagFile.nameWithoutExtension}.wav")
        pbdStream.setPos(entry.offset)
        val sampleData = pbdStream.readBytes(entry.length)
        with(KioOutputStream(vagFile, littleEndian = false)) {
          writeString("VAGp")
          writeInt(0x20)
          writeInt(0)
          writeInt(sampleData.size)
          writeInt(entry.samplingRate)
          writeInt(0)
          writeInt(0)
          writeInt(0)
          writeString("BLANK", length = 0x10)
          writeBytes(sampleData)
          close()
        }
        execute(
          vgmstreamTool,
          args = listOf("-o", wavFile, vagFile),
          streamHandler = nullStreamHandler(),
        )
        vagFile.delete()
      }
    }
  }

  data class PpvaEntry(val index: Int, val offset: Int, val samplingRate: Int, val length: Int, val unk: Int) {
    fun isUnused(): Boolean {
      return offset == -1 && samplingRate == -1 && length == -1 && unk == -1
    }
  }
}
