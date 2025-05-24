package tl.extra.ccc.patcher.translation

import kio.LERandomAccessFile
import kio.util.align
import kio.util.child
import kio.util.readJson
import kio.util.seek
import kio.util.toHex
import tl.extra.ccc.util.CccWidthLimit
import tl.extra.util.TextMeasure
import tl.util.ScriptEditorEntry
import tl.util.writeDatString
import java.io.File

class InDungeonTranslationProcessor(
  pakExtract: File,
  outDir: File,
  unitDir: File,
  private val textMeasure: TextMeasure,
  private val warn: (String) -> Unit,
) {
  private val jsonDir = unitDir.child("json")
  private val translation = CccTranslation(
    unitDir.child("script-japanese.txt"),
    stripNewLine = false,
    replaceLiteralNewLine = false,
    failOnLiteralNewLine = true,
  )
  private val unitTlEntries = translation.jpTexts.indices
    .map { idx ->
      ScriptEditorEntry(translation.jpTexts[idx], translation.getTranslation(idx, allowBlank = true), translation.notes[idx])
    }

  init {
    jsonDir.listFiles()!!.forEach { jsonFile ->
      val relPath = jsonFile.nameWithoutExtension.replace('$', '/')
      println("Processing $relPath...")
      val srcFile = pakExtract.child(relPath)
      val outFile = outDir.child(relPath)
      srcFile.copyTo(outFile, overwrite = true)
      val jsonEntries = jsonFile.readJson<Map<Int, List<ChrDatEntryDescriptor>>>()
      if (jsonEntries.isEmpty()) {
        println("No entries for file, skipping")
        if (!outFile.delete()) {
          error("Can't delete unused file")
        }
        return@forEach
      }
      val fileEntries = unitTlEntries
        .filter { it.note.startsWith(relPath) }
        .toMutableList()
      with(LERandomAccessFile(outFile)) {
        jsonEntries.forEach patchEntry@{ (textPtr, entries) ->
          if (entries.isEmpty()) {
            error("Text pointer ${textPtr.toHex()} has no entries assigned")
          }
          if (entries.all { it.likelyFalsePositive }) {
            println("Skipped likely false positive: ${entries.first().text}")
            return@patchEntry
          }
          val jpText = entries.first().text
          if (entries.any { it.text != jpText }) {
            error("Text mismatch in entries. Expected text to be the same in entry group.")
          }
          val tlEntryIdx = fileEntries.indexOfFirst { it.jp == jpText }
          if (tlEntryIdx == -1) {
            error("Missing TL entry for $jpText. (it might be a duplicate that was removed too early, in that case fix patcher)")
          }
          val tlEntry = fileEntries.removeAt(tlEntryIdx)
          if (tlEntry.en.isBlank()) {
            return@patchEntry
          }
          seek(length())
          align(4)
          val newPtr = filePointer.toInt()
          val writeSize = writeDatString(tlEntry.en)
          if (writeSize > 0x7F) {
            warn("Indungeon: Entry exceeds safe size and may cause issues: $writeSize, ${tlEntry.en}")
          }
          entries.forEach nextEntry@{
            if (it.likelyFalsePositive) {
              return@nextEntry
            }
            seek(it.descriptorPtr)
            writeInt(newPtr)
          }
        }
        seek(length())
        align(16)
        close()
      }
    }
    translation.checkForTooLongTranslations(textMeasure, warn, "Indungeon", CccWidthLimit.inDungeon, maxLines = 2)
  }
}

data class ChrDatEntryDescriptor(
  val descriptorPtr: Int,
  val text: String,
  val textPtr: Int,
  val unkValue: Int,
  val speakerId: Int,
  val soundPtr: Int,
  val soundId: Int,
  val soundPath: String,
  val likelyFalsePositive: Boolean,
)
