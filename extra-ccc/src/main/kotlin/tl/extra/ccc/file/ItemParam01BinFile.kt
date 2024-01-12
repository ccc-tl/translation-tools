package tl.extra.ccc.file

import kio.KioInputStream
import kio.util.appendLine
import tl.util.readDatString
import java.io.File

class ItemParam01BinFile(bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  val entries: Array<ItemParam01Entry>

  init {
    val itemEntries = mutableListOf<ItemParam01Entry>()

    with(KioInputStream(bytes)) {
      val entryCount = readInt()
      readInt()
      readInt()
      readInt()
      repeat(entryCount) {
        val entryBytes = readBytes(0xE4)
        with(KioInputStream(entryBytes)) itemParse@{
          val name = readDatString(maintainStreamPos = true)
          skip(0x54)
          @Suppress("UNUSED_VARIABLE") val buyValue = readInt()
          @Suppress("UNUSED_VARIABLE") val sellValue = readInt()
          val text1 = readDatString(maintainStreamPos = true)
          skip(0x30)
          val text2 = readDatString(maintainStreamPos = true)
          skip(0x30)
          val text3 = readDatString(maintainStreamPos = true)
          skip(0x27)
          itemEntries.add(ItemParam01Entry(entryBytes, name, text1, text2, text3))
        }
      }
      close()
    }

    entries = itemEntries.toTypedArray()
  }

  fun writeToFile(outFile: File) {
    val builder = StringBuilder()
    entries.forEach { entry ->
      builder.appendLine(entry.name)
      builder.appendLine(entry.text1)
      builder.appendLine(entry.text2)
      builder.appendLine(entry.text3)
      builder.appendLine()
    }
    outFile.writeText(builder.toString())
  }
}

class ItemParam01Entry(val bytes: ByteArray, val name: String, val text1: String, val text2: String, val text3: String)
