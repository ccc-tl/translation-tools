package tl.extra.ccc.file

import kio.KioInputStream
import kio.util.appendLine
import tl.util.readDatString
import java.io.File

class ItemParam04BinFile(bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  val entries: Array<ItemParam04Entry>

  init {
    val itemEntries = mutableListOf<ItemParam04Entry>()

    with(KioInputStream(bytes)) {
      val entryCount = readInt()
      readInt()
      readInt()
      readInt()
      repeat(entryCount) {
        val entryBytes = readBytes(0xB8)
        with(KioInputStream(entryBytes)) itemParse@{
          val name = readDatString(maintainStreamPos = true)
          skip(0x50)
          val text1 = readDatString(maintainStreamPos = true)
          skip(0x30)
          val text2 = readDatString(maintainStreamPos = true)
          skip(0x38)
          itemEntries.add(ItemParam04Entry(entryBytes, name, text1, text2))
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
      builder.appendLine()
    }
    outFile.writeText(builder.toString())
  }
}

class ItemParam04Entry(val bytes: ByteArray, val name: String, val text1: String, val text2: String)
