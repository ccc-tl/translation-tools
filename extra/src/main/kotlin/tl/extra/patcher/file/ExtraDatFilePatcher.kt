package tl.extra.patcher.file

import kio.KioOutputStream
import kio.LERandomAccessFile
import kio.util.align
import tl.extra.patcher.translation.ExtraTranslation
import tl.util.writeDatString
import java.io.ByteArrayOutputStream
import java.io.File

internal class ExtraDatFilePatcher(
  origBytes: ByteArray,
  outFile: File,
  translation: ExtraTranslation,
  origTranslation: ExtraTranslation,
  entries: List<CombinedDatEntry>,
  dayDat: Boolean,
) {
  init {
    outFile.writeBytes(origBytes)
    val raf = LERandomAccessFile(outFile)

    var modified = false
    val attachedTextMap = mutableMapOf<String, Long>()
    entries.forEach {
      var tlIndex = origTranslation.enTexts.indexOf(it.enEntry.text)
      if (tlIndex == -1) {
        println("TL WARN: DAT Missing entry: ${it.enEntry.text}")
        return@forEach
      }
      if (!dayDat) {
        if (tlIndex == 1763 && it.jpEntry.text.startsWith("#C120200255ありす#CDEFは#C255120120アリス#CDEF。")) {
          tlIndex = 1764
        }
      }
      val en = translation.getTranslation(tlIndex, allowBlank = true)
      if (en != it.enEntry.text) {
        val enByteLen = KioOutputStream(ByteArrayOutputStream()).writeDatString(en)
        val origByteLen = KioOutputStream(ByteArrayOutputStream()).writeDatString(it.enEntry.text)
        if (enByteLen <= origByteLen) {
          raf.seek(it.enEntry.textLoc.toLong())
          raf.writeDatString(en)
          modified = true
        } else {
          var attachedPointer = attachedTextMap[en]
          if (attachedPointer == null) {
            attachedPointer = raf.length()
            raf.seek(raf.length())
            raf.writeDatString(en)
            attachedTextMap[en] = attachedPointer
          }
          raf.seek(it.enEntry.pointerLoc.toLong())
          raf.writeInt(attachedPointer.toInt())
          modified = true
        }
      }
    }
    raf.seek(raf.length())
    raf.align(16)
    raf.close()
    if (!modified) {
      outFile.delete()
    }
  }
}

class CombinedDatEntry(val relPath: String, val enEntry: DatEntry, val jpEntry: DatEntry)

class DatEntry(val text: String, val textLoc: Int, val pointerLoc: Int)
