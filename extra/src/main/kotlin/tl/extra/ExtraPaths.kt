package tl.extra

import kio.util.child

private val extraToolkitName = System.getenv("FE_EXTRA_TOOLKIT_NAME") ?: "_Extra Toolkit"

val extraToolkit = fateBase.child(extraToolkitName)

val extraIsoUnpack = fateBase.child("Unpack/FateExtra ISO")
val extraCpkUnpack = fateBase.child("Unpack/FateExtra CPK")
val extraPakUnpack = fateBase.child("Unpack/FateExtra PAK")
val extraUnpack = fateBase.child("Unpack/FateExtra Combined")

val extraJpCpkUnpack = fateBase.child("Unpack/FateExtra JP CPK")
val extraJpPakUnpack = fateBase.child("Unpack/FateExtra JP PAK")
