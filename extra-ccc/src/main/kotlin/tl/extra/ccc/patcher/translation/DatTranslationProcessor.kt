package tl.extra.ccc.patcher.translation

import com.google.common.hash.Hashing
import kio.KioOutputStream
import kio.LERandomAccessFile
import kio.util.align
import kio.util.child
import kio.util.readJson
import kio.util.seek
import kio.util.writeJson
import tl.extra.ccc.util.CccWidthLimit
import tl.extra.ccc.util.getTextRemapCompiledBytes
import tl.extra.ccc.util.replaceControlCodesForTextMeasure
import tl.extra.util.TextMeasure
import tl.util.sdbmHash
import tl.util.writeDatString
import java.io.ByteArrayOutputStream
import java.io.File

class DatTranslationProcessor(
  private val pakExtract: File,
  private val outDir: File,
  unitDir: File,
  private val cacheFile: File,
  private val remappedTextDataOut: KioOutputStream,
  private val textMeasure: TextMeasure,
  private val warn: (String) -> Unit,
) {
  private val jsonDir = unitDir.child("json")
  private val translationOverridesFile = unitDir.child("overrides.json")
  private val translationOverrides = translationOverridesFile.readJson<DatTranslationOverrides>()
  private val translationFile = unitDir.child("script-translation.txt")
  private val translation = CccTranslation(
    unitDir.child("script-japanese.txt"),
    stripNewLine = true
  )

  private val cache by lazy {
    if (cacheFile.exists()) cacheFile.readJson() else DatCache()
  }

  init {
    if (remappedTextDataOut.pos() != 0) {
      error("DAT translation processor must be the first one to write remapped text data")
    }
    if (isPatchingNeeded()) {
      scanDuplicates()
      patchFiles()
      saveCache()
    } else {
      println("DAT patching not needed")
      remappedTextDataOut.writeBytes(cache.remappedTextData ?: error("Remapped text data must be initialized for cached DAT patching"))
    }
  }

  private fun isPatchingNeeded(): Boolean {
    if (cache.hash == null) {
      return true
    }
    val currentHash = getHashOfCurrent()
    return !cache.hash.contentEquals(currentHash)
  }

  private fun saveCache() {
    cacheFile.writeJson(DatCache(getHashOfCurrent(), remappedTextDataOut.getAsByteArrayOutputStream().toByteArray()))
  }

  private fun getHashOfCurrent(): ByteArray {
    return Hashing.sha256().hashBytes(translationFile.readBytes() + translationOverridesFile.readBytes()).asBytes()
  }

  private fun scanDuplicates() {
    translation.jpTexts
      .mapIndexed { index, s -> index to s }
      .groupBy { it.second }
      .filter { it.value.size > 1 }
      .forEach { (text, offsets) ->
        val duplicateOffsets = offsets.joinToString { it.first.toString() }
        println("TL WARN: Duplicate text found: $text, it exists at: $duplicateOffsets")
      }
  }

  private fun patchFiles() {
    jsonDir.listFiles()
      ?.filter { it.extension == "json" }
      ?.forEach { jsonFile ->
        val datRelPath = jsonFile.nameWithoutExtension.replace("$", "/")

        val outDatFile = outDir.child(datRelPath)
        outDatFile.parentFile.mkdirs()
        pakExtract.child(datRelPath).copyTo(outDatFile, overwrite = true)

        val entries = jsonFile.readJson<List<CccDatEntry>>()

        println("Patching $datRelPath...")
        patchFile(outDatFile, entries, translationOverrides.getOrElse(jsonFile.nameWithoutExtension) { emptyMap() })
      }
  }

  private fun patchFile(outDatFile: File, entries: List<CccDatEntry>, overrides: DatFileOverrides) {
    val textMeasureStream = KioOutputStream(ByteArrayOutputStream())
    val raf = LERandomAccessFile(outDatFile)

    overrides.forEach { (textLoc, override) ->
      if (entries.none { it.textLoc == textLoc }) {
        warn("DAT: Override at $textLoc to $override does not match any entry")
      }
    }

    entries
      .groupBy { it.textLoc }
      .forEach { (textLoc, entries) ->
        if (entries.distinctBy { it.text }.size > 1) {
          error("Entries with same text loc has different actual texts")
        }

        val jpText = entries.first().text
        val jpTlText = jpText.replace("\n", "\\n").trim()
        val defaultEnText = translation.getTranslation(jpTlText).trimEnd()
        val enText = overrides[textLoc]
          ?.also { println("Overriding translation at text loc $textLoc: '$defaultEnText' -> '$it'") }
          ?: defaultEnText
        if (enText == jpTlText) {
          return@forEach
        }

        val jpTextLen = textMeasureStream.writeDatString(jpText)
        val enTextLen = textMeasureStream.writeDatString(enText)

        checkSgTexts(jpText, enText)
        checkChoiceTexts(entries, enText)
        val useRemapper = checkInDungeonTexts(entries, enText, enTextLen)

        if (useRemapper) {
          println("Using DAT remapping for text: ${enText.replace("\n", "\\n")}")
          val remapBytes = getTextRemapCompiledBytes(remappedTextDataOut.pos())
          remappedTextDataOut.writeDatString(enText)
          if (remapBytes.size > jpTextLen) {
            error("Can't write remapping bytes because the original text is too short: $jpTlText")
          }
          raf.seek(textLoc)
          raf.write(remapBytes)
        } else {
          if (enTextLen <= jpTextLen) {
            raf.seek(textLoc)
            raf.writeDatString(enText)
          } else {
            val newTextLoc = raf.length().toInt()
            raf.seek(raf.length())
            raf.writeDatString(enText)
            entries.forEach {
              raf.seek(it.ptrLoc)
              raf.writeInt(newTextLoc)
            }
          }
        }
      }

    raf.seek(raf.length())
    raf.align(16)
    raf.close()
  }

  private fun checkSgTexts(jpText: String, enText: String) {
    val jpHasSg = jpText.contains("#C\\d{8,9}ＳＧ".toRegex())
    val enHasSg = enText.contains("#C\\d{8,9}SG".toRegex())
    val enHash = sdbmHash(enText.toByteArray())
    if (jpHasSg != enHasSg && enHash != -538666683) {
      warn("DAT: SG highlight mismatch: ${enText.replace("\n", "\\n")} (hash = $enHash)")
    }
  }

  private fun checkChoiceTexts(entries: List<CccDatEntry>, enText: String) {
    if (entries.none { it.type.equals("choice", ignoreCase = true) }) {
      return
    }
    val testEnText = replaceControlCodesForTextMeasure(enText)
    val warningEnText by lazy { enText.replace("\n", "\\n") }
    if (testEnText.contains("\n")) {
      warn("DAT: New line in choice text: $warningEnText")
    } else if (textMeasure.measureInGameText(testEnText) > CccWidthLimit.choice) {
      warn("DAT: Too long choice text: $warningEnText")
    }
  }

  private fun checkInDungeonTexts(entries: List<CccDatEntry>, enText: String, enTextLen: Int): Boolean {
    if (entries.none { it.type.equals("indungeon", ignoreCase = true) }) {
      return false
    }
    translation.checkForTooLongTranslation(textMeasure, warn, "Indungeon (DAT)", CccWidthLimit.inDungeon, maxLines = 2, enText, null)
    return enTextLen > 0x7F
  }
}

data class CccDatEntry(
  val type: String,
  val ptrLoc: Int,
  val textLoc: Int,
  val text: String,
)

private class DatCache(
  val hash: ByteArray? = null,
  val remappedTextData: ByteArray? = null,
)

private typealias DatTranslationOverrides = Map<String, DatFileOverrides>
private typealias DatFileOverrides = Map<Int, String>
