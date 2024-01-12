package tl.extra.ccc.file

import kio.KioInputStream
import kio.util.toWHex
import tl.extra.file.tex.TexFile
import java.io.File

class ImptFile(file: File) {
  val textures = mutableListOf<TexFile>()

  init {
    val input = KioInputStream(file.readBytes())

    with(input) {
      val groupCount = readInt()
      val fileCount = readInt()
      val length = readFloat()
      val unk1 = readInt()

      val unk2 = readInt()
      val textureBlockEnd = pos() + readInt()
      val unk3 = readInt()
      val unk4 = readInt()

      val header = ImptHeader(groupCount, fileCount, length, unk1, unk2, textureBlockEnd, unk3, unk4)

      val entries = mutableListOf<ImptHeaderFileEntry>()

      repeat(fileCount) {
        val name = readStringAndTrim(0x20)
        val offset = pos() + readInt()
        val unkF1 = readInt()
        val unkF2 = readInt()
        val unkF3 = readInt()

        entries.add(ImptHeaderFileEntry(name, offset, unkF1, unkF2, unkF3))
      }

      align(0x10)

      for (i in 0 until entries.size) {
        val offsets = entries.map { it.offset }
        val size = (offsets.getOrElse(i + 1) { textureBlockEnd }) - offsets[i]
        setPos(offsets[i])
        val entryBytes = readBytes(size)
        textures.add(TexFile(entryBytes))
      }

      setPos(textureBlockEnd)

      val dataBlockEntries = mutableListOf<ImptDataBlockEntry>()
      repeat(groupCount) {
        val name = readStringAndTrim(0x20)
        val count = readInt()
        val unkG2 = readInt()
        val unkG3 = readInt()
        val unkG4 = readInt()

        dataBlockEntries.add(ImptDataBlockEntry(name, count, unkG2, unkG3, unkG4))
      }

      val frameEntries = mutableListOf<ImptFrameEntry>()

      dataBlockEntries.forEach {
        repeat(it.count) {
          val time = readFloat()
          val index = readInt()
          val unk = readInt()
          frameEntries.add(ImptFrameEntry(time, index, unk))
        }
      }

      println(header)
      entries.forEach { println(it) }
      dataBlockEntries.forEach { println(it) }
      frameEntries.forEach { println(it) }

      close()
    }
  }
}

class ImptHeader(
  val groupCount: Int,
  val fileCount: Int,
  val length: Float,
  val unk1: Int,
  val unk2: Int,
  val textureBlockEnd: Int,
  val unk3: Int,
  val unk4: Int,
) {
  override fun toString(): String {
    return "ImptHeader(groupCount=$groupCount, fileCount=$fileCount, length=$length, unk2=$unk1, unk3=$unk2, " +
      "textureBlockEnd=${textureBlockEnd.toWHex()}, unk4=$unk3, unk5=$unk4)"
  }
}

class ImptHeaderFileEntry(val name: String, val offset: Int, val unk1: Int, val unk2: Int, val unk3: Int) {
  override fun toString(): String {
    return "ImptFileEntry(name='$name', offset=0x${offset.toWHex()}, unk=0x${unk1.toWHex()}, unk2=$unk2, unk3=$unk3)"
  }
}

class ImptDataBlockEntry(val name: String, val count: Int, val unk2: Int, val unk3: Int, val unk4: Int) {
  override fun toString(): String {
    return "ImptDataBlockEntry(name='$name', count=$count, unk2=$unk2, unk3=$unk3, unk4=$unk4)"
  }
}

class ImptFrameEntry(val time: Float, val index: Int, val unk: Int) {
  override fun toString(): String {
    return "ImptFrameEntry(time=$time, index=$index, unk=$unk)"
  }
}
