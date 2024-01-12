package tl.extra

import kio.util.child
import tl.projectBase

val fateBase = projectBase.child("Fate")
val fateTools = fateBase.child("Tools")
val fateOutput = fateBase.child("TMP")

val vgmstreamTool = fateTools.child("vgmstream/test.exe")
