package tl.extra.ccc.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import kio.util.toWHex
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.util.readDatString
import tl.util.writeDatString
import java.io.File
import java.nio.charset.Charset

class InfoMatrixDBinFilePatcher(
  origBytes: ByteArray,
  outFile: File,
  translation: CccTranslation,
  translationOffset: Int,
  charset: Charset = Charsets.WINDOWS_932,
) {
  init {
    val out = KioOutputStream(outFile)
    with(KioInputStream(origBytes)) {
      val header = readBytes(0x64)
      out.writeBytes(header)

      val entryLens = Array(4) { 0x20 }.toMutableList()
      entryLens.addAll(Array(10) { 0x80 })

      entryLens.forEachIndexed { idx, entryLen ->
        val text = readDatString(maintainStreamPos = true, charset = charset)
        skip(entryLen)

        val newText = translation.getTranslation(idx, translationOffset)

        val len = out.writeDatString(newText, charset)
        if (len > entryLen) {
          error("Too long text: '$newText' for '$text', max length ${entryLen.toWHex()} bytes")
        }
        out.writeNullBytes(entryLen - len)
      }

      out.writeBytes(readBytes(0xC))

      close()
    }
    out.align(16)
    if (out.pos() % 16 != 0) {
      error("Data not aligned")
    }
    out.close()
  }
}
