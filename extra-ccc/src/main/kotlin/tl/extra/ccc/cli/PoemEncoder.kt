package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccToolkit
import tl.extra.fateOutput

fun main() {
  listOf(1, 2).forEach { idx ->
    val poem = cccToolkit.child("src/poem$idx.txt").readText()
    fateOutput.child("poem${idx}_utf16le.bin").writeBytes(poem.toByteArray(Charsets.UTF_16LE))
  }
  println("Done")
}
