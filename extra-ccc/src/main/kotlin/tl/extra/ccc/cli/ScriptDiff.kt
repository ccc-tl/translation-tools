package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.extra.fateOutput
import tl.util.ScriptEditorEntry
import tl.util.ScriptEditorFilesWriter
import java.io.File

fun main() {
  ScriptDiff(
    cccToolkit.child("src/translation/dat"),
    fateOutput.child("script-translation-12-07.txt"),
    fateOutput.child("ccc-script-diff3")
  ).process()
  println("Done")
}

class ScriptDiff(
  translationDir: File,
  compareEnFile: File,
  private val outDir: File,
) {
  private val translation = CccTranslation(
    translationDir.child("script-japanese.txt"),
    stripNewLine = true,
    stripJpAndNotesNewLine = false
  )
  private val translationBase = CccTranslation(
    translationDir.child("script-japanese.txt"),
    enFile = compareEnFile,
    stripNewLine = true,
    stripJpAndNotesNewLine = false,
    safetyChecks = false,
  )

  fun process() {
    val scanRange = 0 until translation.jpTexts.size

    val entries = mutableListOf<ScriptEditorEntry>()
    val entriesOld = mutableListOf<ScriptEditorEntry>()
    val entriesNew = mutableListOf<ScriptEditorEntry>()
    scanRange.mapNotNull { offset ->
      val jp = translation.getJapanese(offset)
      val en = translation.getRawTranslation(offset, allowBlank = true)
      val baseEn = translationBase.getRawTranslation(offset, allowBlank = true)
      if (baseEn != en) {
        entries.add(ScriptEditorEntry(jp, "$baseEn\\n\\n(old $offset)", translation.getNote(offset), "", ""))
        entriesOld.add(ScriptEditorEntry(jp, baseEn.replace("\\n", " "), translation.getNote(offset), "", ""))
        entriesNew.add(ScriptEditorEntry(jp, en.replace("\\n", " "), translation.getNote(offset), "", ""))
        entries.add(ScriptEditorEntry(jp, "$en\\n\\n(new $offset)", translation.getNote(offset), "", ""))
      }
    }
    println("${entries.size / 2} changed entries")
    ScriptEditorFilesWriter(entries).writeTo(outDir)
    ScriptEditorFilesWriter(entriesOld).writeTo(outDir.child("old"))
    ScriptEditorFilesWriter(entriesNew).writeTo(outDir.child("new"))
  }
}
