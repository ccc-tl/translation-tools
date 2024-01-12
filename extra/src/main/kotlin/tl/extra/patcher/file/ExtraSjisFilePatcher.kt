package tl.extra.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import tl.extra.file.SjisEntry
import tl.extra.patcher.translation.ExtraTranslation
import java.io.EOFException
import java.io.File
import java.nio.charset.Charset

internal class ExtraSjisFilePatcher(
  origBytes: ByteArray,
  origJpBytes: ByteArray,
  outFile: File,
  translation: ExtraTranslation,
  translationOffset: Int,
  charset: Charset = Charsets.WINDOWS_932,
) {
  init {
    val out = KioOutputStream(outFile)
    var entryCount = 0
    val jpIn = KioInputStream(origJpBytes)
    with(KioInputStream(origBytes)) {
      while (!eof()) {
        try {
          val l1 = readNullTerminatedString(charset)
          val l2 = readNullTerminatedString(charset)
          val l3 = readNullTerminatedString(charset)
          val l4 = readNullTerminatedString(charset)
          val l5 = readNullTerminatedString(charset)
          val jpl1 = jpIn.readNullTerminatedString(charset)
          val jpl2 = jpIn.readNullTerminatedString(charset)
          val jpl3 = jpIn.readNullTerminatedString(charset)
          val jpl4 = jpIn.readNullTerminatedString(charset)
          val jpl5 = jpIn.readNullTerminatedString(charset)
          val texts = arrayOf(l1, l2, l3, l4, l5)
          if (texts.all { it.isNotBlank() }) {
            if (SjisEntry(l1, l2, l3, l4, l5).isUnused() &&
              SjisEntry(jpl1, jpl2, jpl3, jpl4, jpl5).isUnused()
            ) {
              out.writeNullTerminatedString(l1, charset = charset)
              out.writeNullTerminatedString(l2, charset = charset)
              out.writeNullTerminatedString(l3, charset = charset)
              out.writeNullTerminatedString(l4, charset = charset)
              out.writeNullTerminatedString(l5, charset = charset)
            } else {
              texts.forEach { text ->
                val newText = translation.getTranslation(entryCount, translationOffset, allowBlank = true)
                if (newText.isBlank()) {
                  out.writeNullTerminatedString(text, charset)
                } else {
                  out.writeNullTerminatedString(newText, charset)
                }
                entryCount++
              }
            }
          } else {
            out.writeNullTerminatedString(l1, charset = charset)
            out.writeNullTerminatedString(l2, charset = charset)
            out.writeNullTerminatedString(l3, charset = charset)
            out.writeNullTerminatedString(l4, charset = charset)
            out.writeNullTerminatedString(l5, charset = charset)
          }
        } catch (ignored: EOFException) {
        }
      }
      close()
    }
    out.align(16)
    out.close()
  }
}
