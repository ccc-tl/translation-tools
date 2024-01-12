package tl.extra.file

import kio.KioInputStream
import tl.util.readDatString
import java.io.File

internal class ExtraItemParam01BinFile(bytes: ByteArray, jpSize: Boolean) {
  constructor(file: File, jpSize: Boolean) : this(file.readBytes(), jpSize)

  val entries: List<ExtraItemParam01Entry>

  init {
    val itemEntries = mutableListOf<ExtraItemParam01Entry>()

    with(KioInputStream(bytes)) {
      val entries = readInt()
      readInt()
      readInt()
      readInt()
      repeat(entries) {
        val entryBytes = readBytes(if (jpSize) 0xE4 else 0x100)
        with(KioInputStream(entryBytes)) itemParse@{
          val name = readDatString(maintainStreamPos = true)
          skip(if (jpSize) 0x54 else 0x54)
          val buyValue = readInt()
          val sellValue = readInt()
          val description = readDatString(maintainStreamPos = true)
          skip(if (jpSize) 0x30 else 0x34)
          val trivia1 = readDatString(maintainStreamPos = true)
          skip(if (jpSize) 0x30 else 0x34)
          val trivia2 = readDatString(maintainStreamPos = true)
          skip(if (jpSize) 0x28 else 0x34)
          itemEntries.add(
            ExtraItemParam01Entry(
              bytes = entryBytes,
              name = name,
              buyValue = buyValue,
              sellValue = sellValue,
              description = description,
              trivia1 = trivia1,
              trivia2 = trivia2
            )
          )
        }
      }
      close()
    }

    entries = itemEntries
  }
}

@Suppress("unused")
class ExtraItemParam01Entry(
  val bytes: ByteArray,
  val name: String,
  val buyValue: Int,
  val sellValue: Int,
  val description: String,
  val trivia1: String,
  val trivia2: String,
) {
  fun isUnused(): Boolean = name == "（予備）"
}
