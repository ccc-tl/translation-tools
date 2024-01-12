package tl.extra.ccc.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.util.readDatString
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

class ExtendedTextBinFilePatcher(
  origBytes: ByteArray,
  outFile: File,
  translation: CccTranslation,
  translationOffset: Int,
  charset: Charset = Charsets.WINDOWS_932,
) {
  init {
    val out = KioOutputStream(outFile)
    with(KioInputStream(origBytes)) {
      var entryCount = 0

      val fileEntryCount = readInt()
      val unkInt = readInt()
      out.writeInt(fileEntryCount)
      out.writeInt(unkInt)

      repeat(fileEntryCount) {
        val index = readInt()
        val text = readDatString(maintainStreamPos = true, charset = charset)
        readBytes(0x40)

        val newText = translation.getTranslation(entryCount, translationOffset)
        entryCount++

        out.writeInt(index)
        val len = out.writeDatString(newText, charset = charset)
        if (len > 0x40) {
          error("Too long text: '$newText' for '$text', max length 0x40 bytes")
        }
        out.writeNullBytes(0x40 - len)
      }
      close()
    }
    out.align(16)
    if (out.pos() % 16 != 0) {
      error("Data not aligned")
    }
    out.close()
  }
}
