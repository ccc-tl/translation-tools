package tl.extra.ccc.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.ccc.patcher.translation.CccTranslation
import java.io.EOFException
import java.io.File
import java.nio.charset.Charset

class SjisFilePatcher(
  origBytes: ByteArray,
  outFile: File,
  translation: CccTranslation,
  translationOffset: Int,
  charset: Charset = Charsets.WINDOWS_932,
) {
  init {
    val out = KioOutputStream(outFile)
    var entryCount = 0
    with(KioInputStream(origBytes)) {
      while (!eof()) {
        try {
          val l1 = readNullTerminatedString(Charsets.WINDOWS_932)
          val l2 = readNullTerminatedString(Charsets.WINDOWS_932)
          val l3 = readNullTerminatedString(Charsets.WINDOWS_932)
          val l4 = readNullTerminatedString(Charsets.WINDOWS_932)
          val l5 = readNullTerminatedString(Charsets.WINDOWS_932)
          val texts = arrayOf(l1, l2, l3, l4, l5)
          if (texts.all { it.isNotBlank() }) {
            texts.forEach { _ ->
              val newText = translation.getTranslation(entryCount, translationOffset)
              out.writeNullTerminatedString(newText, charset)
              entryCount++
            }
          }
        } catch (ignored: EOFException) {
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
