package tl.extra.ccc.patcher.translation

import kio.util.WINDOWS_932
import kio.util.arrayCopy
import kio.util.child
import kio.util.toWHex
import kmipsx.elf.ElfPatchSuite
import tl.extra.ccc.patcher.provideEbootTextMappings
import java.io.File
import kotlin.math.ceil

class EbootTranslationProcessor(unitDir: File, private val warn: (String) -> Unit) {
  private val translation = CccTranslation(unitDir.child("script-japanese.txt"), failOnLiteralNewLine = true)
  val translationEbootPatches = ElfPatchSuite(baseAddress = 0x8804000, useRelativeChangeAddress = true)

  fun patchEboot(eboot: File) {
    // generateEntryList(eboot)
    insertText(eboot)
  }

  private fun insertText(eboot: File) {
    val emptyBlocks = mutableListOf<EmptyBlock>()
    emptyBlocks.add(EmptyBlock(0x225BCC, 0xE4))
    emptyBlocks.add(EmptyBlock(0x225CCC, 0xE4))
    emptyBlocks.add(EmptyBlock(0x225DCC, 0xE4))
    emptyBlocks.add(EmptyBlock(0x225ECC, 0xE4))
    emptyBlocks.add(EmptyBlock(0x225FC8, 0xE4))
    emptyBlocks.add(EmptyBlock(0x2260C8, 0xE4))

    val entries = provideEbootTextMappings(translation)
    val ebootBytes = eboot.readBytes()
    var extraRequiredBytes = 0
    var freedBytes = 0
    translation.jpTexts.forEachIndexed { textIndex, jpText ->
      val enText = translation.getTranslation(textIndex)
      val targets = entries.filter { it.tlIndex == textIndex }

      if (targets.isEmpty()) {
        warn("EBOOT text insertion[$textIndex]: Missing entry mapping for entry index")
        return@forEachIndexed
      }

      targets.forEach { target ->
        if (target.skip) return@forEach
        val patchText = target.overrideText.ifBlank { enText }
        val patchBytes = patchText.toByteArray(Charsets.WINDOWS_932)
        val patchDestAddr: Int

        if (patchBytes.size > target.allowedSize) {
          val targetBlock = emptyBlocks.firstOrNull { it.usedBytes + patchBytes.size < it.size }
          if (targetBlock == null) {
            warn("EBOOT translation[$textIndex]: Can't auto-patch entry, too long translation: $jpText => $patchText")
            extraRequiredBytes += patchBytes.size
            freedBytes += target.allowedSize
            return@forEach
          }
          val addr = targetBlock.allocate(patchBytes.size)
          patchDestAddr = addr.first
          val virtualDestAddr = addr.second

          if (!target.auxPatchUsed) {
            warn("EBOOT translation[$textIndex]: Too long entry does not provide aux patch. $jpText => $patchText")
          } else {
            translationEbootPatches.addPatches(target.auxPatch.map { it(virtualDestAddr) })
          }
        } else {
          patchDestAddr = target.physicalAddress
          arrayCopy(
            src = Array(target.allowedSize) { 0.toByte() }.toByteArray(),
            dest = ebootBytes,
            destPos = patchDestAddr,
          )
          if (target.auxPatchUsed) {
            warn("EBOOT translation[$textIndex]: Standard overwrite entry provides aux patch. Ignored. $jpText => $patchText")
          }
        }

        arrayCopy(src = patchBytes, dest = ebootBytes, destPos = patchDestAddr)
      }
    }

    if (extraRequiredBytes > 0) {
      val emptyBlocksTotal = emptyBlocks.sumOf { it.size - it.usedBytes }
      warn(
        "EBOOT translation: Need $extraRequiredBytes bytes to patch all entries, those entries free up $freedBytes bytes. " +
          "Empty blocks provide additional $emptyBlocksTotal bytes.",
      )
    }

    eboot.writeBytes(ebootBytes)
  }

  private fun generateEntryList(eboot: File) {
    println("--- Generating EBOOT text pointer entries ---")
    val ebootBytes = eboot.readBytes()
    translation.jpTexts.forEachIndexed { jpTextIndex, jpText ->
      val jpTextBytes = jpText.toByteArray(Charsets.WINDOWS_932).run {
        val list = toMutableList()
        list.add(0.toByte())
        list.toByteArray()
      }
      val allowedSize = (ceil(jpTextBytes.size / 4.0) * 4).toInt() - 1
      var foundOnce = false
      var index = -1
      while (true) {
        index = indexOf(ebootBytes, jpTextBytes, if (index == -1) 0 else index + 1)
        if (index == -1) {
          break
        }
        println("add(Entry($jpTextIndex, $allowedSize, 0x${(index - 0xA0).toWHex()})) //$jpText")
        foundOnce = true
      }
      if (!foundOnce) {
        println("//$jpTextIndex: $jpText is missing")
      }
    }
    println("--- Generating text pointer finished ---")
  }

  private fun indexOf(outerArray: ByteArray, smallerArray: ByteArray, start: Int = 0): Int {
    for (i in start..outerArray.size - smallerArray.size) {
      var found = true
      for (j in smallerArray.indices) {
        if (outerArray[i + j] != smallerArray[j]) {
          found = false
          break
        }
      }
      if (found) {
        return i
      }
    }
    return -1
  }

  inner class EmptyBlock(private val physicalAddress: Int, val size: Int) {
    private val virtualAddress = physicalAddress + 0x8804000 - 0xA0
    var usedBytes = 0
      private set

    fun allocate(bytes: Int): Pair<Int, Int> {
      val physicalBlockAddr = physicalAddress + usedBytes
      val virtualBlockAddr = virtualAddress + usedBytes
      val padding = 4 - (bytes % 4)
      usedBytes += bytes + padding
      if (usedBytes > size) {
        error("Allocation used more bytes than this block had. Address: $physicalAddress")
      }
      return physicalBlockAddr to virtualBlockAddr
    }
  }
}
