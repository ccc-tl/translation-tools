package tl.extra.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.patcher.translation.ExtraTranslation
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

internal class ExtraInfoMatrixBinDFilePatcher(
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
      out.writeBytes(readBytes(0x54))
      repeat(3) {
        val len = out.writeDatString(
          translation.getTranslation(entryCount, translationOffset, allowBlank = true),
          charset = charset,
        )
        if (len > 0x20) {
          error("Too long translation text")
        }
        out.writeNullBytes(0x20 - len)
        entryCount++
        entryCount++
        entryCount++
        skip(0x20)
      }
      repeat(6) {
        val len = out.writeDatString(
          translation.getTranslation(entryCount, translationOffset, allowBlank = true),
          charset = charset,
        )
        if (len > 0x80) {
          error("Too long translation text")
        }
        out.writeNullBytes(0x80 - len)
        entryCount++
        entryCount++
        entryCount++
        skip(0x80)
      }
      out.writeBytes(readBytes((size - longPos()).toInt()))
      close()
    }
    out.align(16)
    out.close()
  }
}
