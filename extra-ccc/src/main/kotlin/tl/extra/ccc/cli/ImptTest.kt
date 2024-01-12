package tl.extra.ccc.cli

import kio.util.walkDir
import tl.extra.ccc.file.ImptFile
import tl.extra.extraUnpack
import java.io.File

fun main() {
  walkDir(extraUnpack, { _, e -> throw e }, ::processImptFile)
  println("Done")
}

private fun processImptFile(it: File) {
  if (it.extension != "Impt") {
    return
  }
  println("Process ${it.path}")
  ImptFile(it)
  println()
}
