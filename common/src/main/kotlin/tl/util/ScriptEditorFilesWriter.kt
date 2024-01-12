package tl.util

import kio.util.appendLine
import kio.util.child
import java.io.File

class ScriptEditorFilesWriter(val entries: List<ScriptEditorEntry>) {
  fun writeTo(outDir: File) {
    outDir.mkdir()
    val jpOut = StringBuilder()
    val enOut = StringBuilder()
    val notesOut = StringBuilder()
    val portraitOut = StringBuilder()
    val audioOut = StringBuilder()
    entries.forEach { entry ->
      jpOut.appendLine("${entry.jp}{end}\n")
      enOut.appendLine("${entry.en}{end}\n")
      notesOut.appendLine("${entry.note}{end}\n")
      portraitOut.appendLine("${entry.portrait}{end}\n")
      audioOut.appendLine("${entry.audio}{end}\n")
    }
    outDir.child("script-japanese.txt").writeText(jpOut.toString())
    outDir.child("script-translation.txt").writeText(enOut.toString())
    outDir.child("script-notes.txt").writeText(notesOut.toString())
    outDir.child("script-portrait.txt").writeText(portraitOut.toString())
    outDir.child("script-audio.txt").writeText(audioOut.toString())
  }
}

data class ScriptEditorEntry(
  val jp: String,
  val en: String = "",
  val note: String = "",
  val portrait: String = "",
  val audio: String = "",
)
