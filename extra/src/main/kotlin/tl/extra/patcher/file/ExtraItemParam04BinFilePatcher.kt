package tl.extra.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import kio.util.toWHex
import tl.extra.file.ExtraItemParam04Entry
import tl.extra.patcher.translation.ExtraTranslation
import tl.util.readDatString
import java.io.File
import java.nio.charset.Charset

internal class ExtraItemParam04BinFilePatcher(
  origBytes: ByteArray,
  outFile: File,
  translation: ExtraTranslation,
  translationOffset: Int,
  charset: Charset = Charsets.WINDOWS_932,
) {
  init {
    val out = KioOutputStream(outFile)
    var entryCount = 0

    with(KioInputStream(origBytes)) {
      val entries = readInt()
      val u1 = readInt()
      val u2 = readInt()
      val u3 = readInt()
      out.writeInt(entries)
      out.writeInt(u1)
      out.writeInt(u2)
      out.writeInt(u3)

      repeat(entries) {
        val origEntryBytes = readBytes(0xC0)
        var origEntry: ExtraItemParam04Entry
        with(KioInputStream(origEntryBytes)) itemParse@{
          val name = readDatString(maintainStreamPos = true, charset = charset)
          skip(0x48)
          val buyValue = readInt()
          val sellValue = readInt()
          val desc = readDatString(maintainStreamPos = true, charset = charset)
          skip(0x34)
          val trivia = readDatString(maintainStreamPos = true, charset = charset)
          skip(0x34)
          origEntry = ExtraItemParam04Entry(origEntryBytes, name, buyValue, sellValue, desc, trivia)
        }
        if (origEntry.isUnused()) {
          out.writeBytes(origEntryBytes)
        } else {
          with(KioInputStream(origEntryBytes)) {
            run {
              val newText = translation.getTranslation(entryCount, translationOffset, allowBlank = true)
              entryCount++
              if (newText.length > 0x17) {
                error("Max length for item name exceeded: $newText at ${out.pos().toWHex()}")
              }
              out.writeString(newText, 0x18, charset = charset)
              skip(0x18)
            }
            out.writeBytes(readBytes(0x38))
            run {
              val newText = translation.getTranslation(entryCount, translationOffset, allowBlank = true)
              entryCount++
              if (newText.length > 0x33) {
                error("Max length for item name exceeded: $newText at ${out.pos().toWHex()}")
              }
              out.writeString(newText, 0x34, charset = charset)
              skip(0x34)
            }
            run {
              val newText = translation.getTranslation(entryCount, translationOffset, allowBlank = true)
              entryCount++
              if (newText.length > 0x33) {
                error("Max length for item name exceeded: $newText at ${out.pos().toWHex()}")
              }
              out.writeString(newText, 0x34, charset = charset)
              skip(0x34)
            }
            out.writeBytes(readBytes(0x8))
            skip(0x8)
          }
        }
      }
      close()
    }

    out.align(16)
    out.close()
  }
}
