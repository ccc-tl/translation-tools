package tl.extra.file

import kio.KioInputStream
import tl.util.readDatString
import java.io.File

internal class ExtraItemParam04BinFile(bytes: ByteArray, jpSize: Boolean) {
  constructor(file: File, jpSize: Boolean) : this(file.readBytes(), jpSize)

  val entries: List<ExtraItemParam04Entry>

  init {
    val itemEntries = mutableListOf<ExtraItemParam04Entry>()

    with(KioInputStream(bytes)) {
      val entries = readInt()
      readInt()
      readInt()
      readInt()
      repeat(entries) {
        val entryBytes = readBytes(if (jpSize) 0xB8 else 0xC0)
        with(KioInputStream(entryBytes)) itemParse@{
          val name = readDatString(maintainStreamPos = true)
          skip(0x48)
          val buyValue = readInt()
          val sellValue = readInt()
          val desc = readDatString(maintainStreamPos = true)
          skip(if (jpSize) 0x30 else 0x34)
          val trivia = readDatString(maintainStreamPos = true)
          skip(if (jpSize) 0x28 else 0x34)
          itemEntries.add(
            ExtraItemParam04Entry(
              bytes = entryBytes,
              name = name,
              buyValue = buyValue,
              sellValue = sellValue,
              description = desc,
              trivia = trivia
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
class ExtraItemParam04Entry(
  val bytes: ByteArray,
  val name: String,
  val buyValue: Int,
  val sellValue: Int,
  val description: String,
  val trivia: String,
) {
  fun isUnused(): Boolean = name in listOf("（予備）", "-")
}
