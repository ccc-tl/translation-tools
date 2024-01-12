package tl.extra.ccc.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.util.readDatString
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

@Suppress("UNUSED_VARIABLE")
class TextBinFilePatcher(
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
      while (true) {
        val index = readInt()
        if ((index == 0 && entryCount > 0) || eof()) {
          break
        }
        val len = readInt()
        val text = readDatString(fixUpNewLine = true, charset = charset)
        val newText = translation.getTranslation(entryCount, translationOffset)
          .replace("\\n", "\n\r") // note: for this file new line sequence is inverted
        entryCount++
        out.writeInt(index)
        val bytes = newText.toByteArray(charset)
        out.writeInt(bytes.size + (4 - (bytes.size % 4)))
        out.writeDatString(newText, charset = charset)
        if (eof()) {
          break
        }
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
