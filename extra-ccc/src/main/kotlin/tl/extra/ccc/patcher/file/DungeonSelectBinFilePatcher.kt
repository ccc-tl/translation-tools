package tl.extra.ccc.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.util.readDatString
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

class DungeonSelectBinFilePatcher(
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
        val text1 = readDatString(maintainStreamPos = true, charset = charset)
        skip(0x30)
        val text2 = readDatString(maintainStreamPos = true, charset = charset)
        skip(0x30)
        val unkBytes = readBytes(0x30)

        val newText1 = translation.getTranslation(entryCount, translationOffset)
        entryCount++
        val newText2 = translation.getTranslation(entryCount, translationOffset)
        entryCount++

        run {
          val len = out.writeDatString(newText1, charset)
          if (len > 0x30) {
            error("Too long text 1: '$newText1' for '$text1', max length 0x30 bytes")
          }
          out.writeNullBytes(0x30 - len)
        }

        run {
          val len = out.writeDatString(newText2, charset)
          if (len > 0x30) {
            error("Too long text: '$newText2' for '$text2', max length 0x30 bytes")
          }
          out.writeNullBytes(0x30 - len)
        }

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
