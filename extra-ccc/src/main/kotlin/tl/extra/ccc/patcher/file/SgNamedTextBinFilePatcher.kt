package tl.extra.ccc.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.util.readDatString
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

class SgNamedTextBinFilePatcher(
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
      while (!eof()) {
        val unkBytes = readBytes(0x4)
        val text = readDatString(maintainStreamPos = true, charset = charset)
        skip(0x3C)

        val newText = translation.getTranslation(entryCount, translationOffset)
        entryCount++

        out.writeBytes(unkBytes)
        val len = out.writeDatString(newText, charset)
        if (len > 0x3C) {
          error("Too long text: '$newText' for '$text', max length 0x3C bytes")
        }
        out.writeNullBytes(0x3C - len)
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
