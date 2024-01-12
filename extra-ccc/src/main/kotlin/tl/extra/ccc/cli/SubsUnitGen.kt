package tl.extra.ccc.cli

import kio.util.child
import kio.util.relativizePath
import kio.util.walkDir
import tl.extra.ccc.cccUnpack
import tl.extra.fateBase
import tl.extra.fateOutput
import tl.util.ScriptEditorEntry
import tl.util.ScriptEditorFilesWriter

fun main() {
  generateCvUnit()
  generateSeUnit()
  println("Done")
}

private fun generateCvUnit() {
  val unpackDir = cccUnpack
  val output = fateOutput
  val entries = mutableListOf<ScriptEditorEntry>()
  walkDir(unpackDir.child("cv")) {
    entries.add(ScriptEditorEntry(it.relativizePath(unpackDir), "", ""))
  }
  ScriptEditorFilesWriter(entries).writeTo(output.child("ccc-cv"))
}

private fun generateSeUnit() {
  val seDir = fateBase.child("ExtractedResources/Sound/ccc-se")
  val output = fateOutput
  val entries = seDir.listFiles()!!.map {
    ScriptEditorEntry(it.relativizePath(seDir), "", "")
  }
  ScriptEditorFilesWriter(entries).writeTo(output.child("ccc-se"))
}
