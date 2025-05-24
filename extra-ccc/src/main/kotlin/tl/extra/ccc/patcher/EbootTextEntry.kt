package tl.extra.ccc.patcher

import kmipsx.elf.ElfPatch

class EbootTextEntry(
  val tlIndex: Int,
  val allowedSize: Int,
  val address: Int,
  val skip: Boolean = false,
  val overrideText: String = "",
  val auxPatch: Array<(insertedTextAddr: Int) -> ElfPatch> = emptyArray(),
) {
  val physicalAddress = if (address == -1) -1 else address + 0xA0
  val auxPatchUsed = !auxPatch.contentEquals(emptyArray())
}
