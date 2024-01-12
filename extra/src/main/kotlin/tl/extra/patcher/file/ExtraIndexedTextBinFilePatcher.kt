package tl.extra.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.patcher.translation.ExtraTranslation
import tl.util.readDatString
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

internal class ExtraIndexedTextBinFilePatcher(
  origBytes: ByteArray,
  outFile: File,
  translation: ExtraTranslation,
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
        val newText = translation.getTranslation(entryCount, translationOffset, allowBlank = true)
        entryCount++
        if (newText.isBlank()) {
          out.writeInt(index)
          out.writeInt(len)
          out.writeDatString(text, charset = charset)
        } else {
          out.writeInt(index)
          val bytes = newText.toByteArray(charset)
          out.writeInt(bytes.size + (4 - (bytes.size % 4)))
          out.writeDatString(newText, charset = charset)
        }
        if (eof()) {
          break
        }
      }
      close()
    }
    out.align(16)
    out.close()
  }
}
