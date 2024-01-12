package tl.extra.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.patcher.translation.ExtraTranslation
import tl.util.readDatString
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

internal class ExtraFixedSizeTextBinFilePatcher(
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

      val fileEntryCount = readInt()
      val unkInt = readInt()
      out.writeInt(fileEntryCount)
      out.writeInt(unkInt)

      repeat(fileEntryCount) {
        val index = readInt()
        val text = readDatString(maintainStreamPos = true, charset = charset)
        readBytes(0x40)

        val newText = translation.getTranslation(entryCount, translationOffset, allowBlank = true)
        entryCount++

        if (newText.isBlank()) {
          out.writeInt(index)
          val len = out.writeDatString(text, charset = charset)
          out.writeNullBytes(0x40 - len)
        } else {
          out.writeInt(index)
          val len = out.writeDatString(newText, charset = charset)
          if (len > 0x40) {
            error("Too long text: '$newText' for FixedSizeTextBinFile, max length 0x40 bytes")
          }
          out.writeNullBytes(0x40 - len)
        }
      }
      close()
    }
    out.align(16)
    out.close()
  }
}
