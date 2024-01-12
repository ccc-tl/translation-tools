package tl.cli

import kio.util.child
import tl.util.ScriptEditorEntry
import tl.util.ScriptEditorFilesWriter
import tl.util.Translation
import java.io.File

fun main(args: Array<String>) {
  if (args.size != 3) {
    println("Usage: [base] [new] [dest]")
    return
  }
  val base = File(args[0])
  val new = File(args[1])
  val dest = File(args[2])

  val baseTranslation = Translation(base.child("script-japanese.txt"))
  val newTranslation = Translation(new.child("script-japanese.txt"))

  val newEntries = mutableListOf<ScriptEditorEntry>()
  newTranslation.jpTexts.forEachIndexed { idx, jp ->
    newEntries.add(
      ScriptEditorEntry(
        jp,
        baseTranslation.enTexts.getOrElse(idx) { "" },
        baseTranslation.notes.getOrElse(idx) { "" },
        "",
        ""
      )
    )
  }
  ScriptEditorFilesWriter(newEntries).writeTo(dest)
  println("Done")
}
