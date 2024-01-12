package tl.extra.ccc.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import kio.util.toWHex
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.extra.ccc.util.getTextRemapRelativeBytes
import tl.util.alignValue
import tl.util.readDatString
import tl.util.writeDatString
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset

class InfoMatrixTBinFilePatcher(
  origBytes: ByteArray,
  outFile: File,
  translation: CccTranslation,
  translationOffset: Int,
  charset: Charset = Charsets.WINDOWS_932,
) {
  init {
    val out = KioOutputStream(outFile)
    val outAlignedSize = alignValue(origBytes.size, 16)
    val relativeRemapOut = KioOutputStream(ByteArrayOutputStream())
    with(KioInputStream(origBytes)) {
      val header = readBytes(0x4)
      out.writeBytes(header)

      var entryCount = 0
      while (!eof()) {
        val startPos = pos()
        val text = readDatString(charset = charset)
        skipNullBytes()
        val entryLen = pos() - startPos

        val newText = translation.getTranslation(entryCount, translationOffset)
        entryCount++
        val newTextLength = KioOutputStream(ByteArrayOutputStream()).writeDatString(newText)
        if (newTextLength > entryLen - 0x8) { // 0x8 is just a hack to give some margin for wide characters
          val remapBytes = getTextRemapRelativeBytes(outAlignedSize - out.pos() + relativeRemapOut.pos())
          relativeRemapOut.writeDatString(newText)
          out.writeBytes(remapBytes)
          out.writeNullBytes(entryLen - remapBytes.size)
        } else {
          val len = out.writeDatString(newText, charset)
          if (len > entryLen) {
            error("Too long text: '$newText' for '$text', max length ${entryLen.toWHex()} bytes, idx: " + entryCount)
          }
          out.writeNullBytes(entryLen - len)
        }
      }
      close()
    }
    out.align(16)
    if (out.pos() != origBytes.size) {
      error("Output file is larger than input")
    }
    out.writeBytes(relativeRemapOut.getAsByteArrayOutputStream().toByteArray())
    out.align(16)
    if (out.pos() % 16 != 0) {
      error("Data not aligned")
    }
    out.close()
  }
}
