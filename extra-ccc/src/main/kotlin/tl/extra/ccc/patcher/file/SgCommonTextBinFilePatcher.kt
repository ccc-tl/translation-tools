package tl.extra.ccc.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.util.readDatString
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

class SgCommonTextBinFilePatcher(
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
        val text = readDatString(maintainStreamPos = true, charset = charset)
        skip(0x30)
        val unkBytes = readBytes(0x30)

        val newText = translation.getTranslation(entryCount, translationOffset)
        entryCount++

        val len = out.writeDatString(newText, charset)
        if (len > 0x30) {
          error("Too long text: '$newText' for '$text', max length 0x30 bytes")
        }
        out.writeNullBytes(0x30 - len)

        out.writeBytes(unkBytes)
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
