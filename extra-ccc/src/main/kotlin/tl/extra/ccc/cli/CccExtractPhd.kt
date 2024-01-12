package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccUnpack
import tl.extra.fateOutput
import tl.extra.vgmstreamTool
import tl.util.PhdSoundProcessor

fun main() {
  val out = fateOutput
  PhdSoundProcessor(vgmstreamTool).apply {
    val cccOut = out.child("ccc-se").apply { mkdir() }
    process(cccUnpack.child("sound/se"), cccOut)
  }
  println("Done")
}
