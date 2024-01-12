package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccUnpack
import tl.extra.ccc.file.Mob2DFile
import tl.extra.fateOutput
import tl.extra.file.PakFile

fun main() {
  val out = fateOutput.child("poem_mob_TMP")
  out.deleteRecursively()
  out.mkdir()
  val pak = PakFile(cccUnpack.child("interface/poem/poem_07.mob"))
  pak.entries.forEach { pakEntry ->
    println("--- PAK entry ${pakEntry.index} ---")
    val mob = Mob2DFile(pakEntry.bytes)
    mob.dump()
    pakEntry.writeToFile(out.child("${pakEntry.index}.bin"))
  }
  println("Done")
}
