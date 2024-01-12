package tl.extra.patcher.file

import kio.KioInputStream
import kio.KioOutputStream
import kio.LERandomAccessFile
import kio.util.WINDOWS_932
import kio.util.align
import kio.util.getSubArrayPos
import kio.util.toWHex
import tl.extra.patcher.translation.ExtraTranslation
import tl.util.writeDatString
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset

internal class ExtraChrDatFilePatcher(
  origBytes: ByteArray,
  outFile: File,
  translation: ExtraTranslation,
  origTranslation: ExtraTranslation,
  translationOffset: Int,
  count: Int,
  charset: Charset = Charsets.WINDOWS_932,
) {
  init {
    outFile.writeBytes(origBytes)
    val raf = LERandomAccessFile(outFile)

    val attachedTextMap = mutableMapOf<String, Long>()
    repeat(count) { offset ->
      val en = translation.getTranslation(offset, translationOffset, allowBlank = true)
      val origEn = origTranslation.getTranslation(offset, translationOffset, allowBlank = true)
      if (en != origEn) {
        val enByteLen = KioOutputStream(ByteArrayOutputStream()).writeDatString(en, charset = charset)
        val origByteLen = KioOutputStream(ByteArrayOutputStream()).writeDatString(origEn, charset = charset)
        if (enByteLen <= origByteLen) {
          raf.seek(getSubArrayPos(origBytes, origEn.toByteArray()).toLong())
          raf.writeDatString(en, charset = charset)
        } else {
          val pos = getSubArrayPos(origBytes, origEn.toByteArray())
          if (pos == -1) {
            error("$origEn not found")
          }
          var written = false
          with(KioInputStream(origBytes)) {
            while (!eof()) {
              val pointerPos = pos()
              if (readInt() == pos) {
                var attachedPointer = attachedTextMap[en]
                if (attachedPointer == null) {
                  attachedPointer = raf.length()
                  raf.seek(raf.length())
                  raf.writeDatString(en, charset = charset)
                  attachedTextMap[en] = attachedPointer
                }
                raf.seek(pointerPos.toLong())
                raf.writeInt(attachedPointer.toInt())
                if (written) {
                  println("TL WARN: Written in-dungeon twice: at ${pointerPos.toWHex()}, $en")
                }
                written = true
              }
            }
          }
          if (!written) {
            println("$en not written")
          }
        }
      }
    }
    raf.seek(raf.length())
    raf.align(16)
    raf.close()
  }
}
