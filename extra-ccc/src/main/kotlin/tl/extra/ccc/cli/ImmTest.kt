package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.file.ImmFile
import tl.extra.extraUnpack
import tl.extra.fateOutput
import tl.extra.file.PakFile
import tl.extra.file.PakFileEntry
import java.io.File

fun main() {
  val mobPak = PakFile(extraUnpack.child("chr/tam/0002.mob"))
  printEntry(mobPak.entries[3], true, 2, "walk.py")
  printEntry(mobPak.entries[4], true, 0, "run.py")
  println("Done")
}

private fun printEntry(
  entry: PakFileEntry,
  emitPython: Boolean = false,
  ignoreFrames: Int = 1,
  outFileName: String = "anim.py",
  globalOffset: Int = 0,
) {
  println("Process ${entry.index} ${entry.path}")
  val imm = ImmFile(entry.bytes)
  if (emitPython) {
    val script = imm.emitPython(ignoreFrames, globalOffset)
    if (globalOffset != 0) {
      File(fateOutput, outFileName).appendText(script)
    } else {
      File(fateOutput, outFileName).writeText(script)
    }
    println("Python script written")
  } else {
    imm.debugDump()
  }
  println()
}
