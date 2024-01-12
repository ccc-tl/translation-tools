package tl.extra.cli

import kio.util.child
import kio.util.relativizePath
import tl.extra.extraUnpack
import tl.extra.fateBase
import tl.extra.fateOutput
import tl.util.ScriptEditorEntry
import tl.util.ScriptEditorFilesWriter
import java.io.File

fun main() {
  val output = fateOutput
  generateCvUnit(extraUnpack, extraUnpack.child("cv"), output)
  generateSeUnit(fateBase.child("ExtractedResources/Sound/extra-se"), output)
  println("Done")
}

private fun generateCvUnit(baseDir: File, cvDir: File, output: File) {
  val entries = cvDir.listFiles()?.map {
    ScriptEditorEntry(it.relativizePath(baseDir), "", "")
  } ?: error("Missing CV dir")
  ScriptEditorFilesWriter(entries)
    .writeTo(output.child("extra-cv"))
}

private fun generateSeUnit(seDir: File, output: File) {
  val entries = seDir.listFiles()
    ?.map { ScriptEditorEntry(it.relativizePath(seDir), "", "") }
    ?: error("Missing SE dir")
  ScriptEditorFilesWriter(entries)
    .writeTo(output.child("extra-se"))
}
