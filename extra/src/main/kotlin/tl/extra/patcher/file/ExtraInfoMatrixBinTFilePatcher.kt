package tl.extra.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.patcher.translation.ExtraTranslation
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

internal class ExtraInfoMatrixBinTFilePatcher(
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
      out.writeInt(readInt())
      repeat(9) {
        entryCount++
        entryCount++
        val len = out.writeDatString(
          translation.getTranslation(entryCount, translationOffset, allowBlank = true),
          charset = charset
        )
        if (len > 0x1400) {
          error("Too long translation text")
        }
        out.writeNullBytes(0x1400 - len)
        entryCount++
      }
      entryCount = 0
      repeat(9) {
        entryCount++
        val len = out.writeDatString(
          translation.getTranslation(entryCount, translationOffset, allowBlank = true),
          charset = charset
        )
        if (len > 0x44) {
          error("Too long translation text")
        }
        out.writeNullBytes(0x44 - len)
        entryCount++
        entryCount++
      }
      entryCount = 0
      close()
    }
    out.align(16)
    out.close()
  }
}
