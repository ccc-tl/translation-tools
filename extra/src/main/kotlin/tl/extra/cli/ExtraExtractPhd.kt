package tl.extra.cli

import kio.util.child
import tl.extra.extraUnpack
import tl.extra.fateOutput
import tl.extra.vgmstreamTool
import tl.util.PhdSoundProcessor

fun main() {
  val out = fateOutput
  PhdSoundProcessor(vgmstreamTool).apply {
    val extraOut = out.child("extra-se").apply { mkdir() }
    process(extraUnpack.child("sound/se"), extraOut)
  }
  println("Done")
}
