package tl.extra.ccc

import kio.util.child
import tl.extra.fateBase

private val cccToolkitName = System.getenv("FE_CCC_TOOLKIT_NAME") ?: "_CCC Toolkit"

val cccToolkit = fateBase.child(cccToolkitName)

val cccDnsUnpack = fateBase.child("Unpack/FateExtraCCC DNS")
val cccCpkUnpack = fateBase.child("Unpack/FateExtraCCC CPK")
val cccPakUnpack = fateBase.child("Unpack/FateExtraCCC PAK")
val cccUnpack = fateBase.child("Unpack/FateExtraCCC Combined")
