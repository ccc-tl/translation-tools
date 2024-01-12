package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccUnpack
import tl.extra.ccc.file.Mob2DFile
import tl.extra.file.PakFile

fun main() {
  PakFile(cccUnpack.child("interface/dialog/0000.mob")).entries.forEach {
    println("Entry ${it.index}:")
    Mob2DFile(it.bytes).dump()
    println("")
  }
  println("Done")
}
