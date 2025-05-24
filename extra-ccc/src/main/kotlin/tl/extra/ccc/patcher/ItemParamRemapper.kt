package tl.extra.ccc.patcher

import kio.KioOutputStream
import kio.util.arrayCopy
import kio.util.toHex
import tl.extra.ccc.file.ItemParam01BinFile
import tl.extra.ccc.file.ItemParam04BinFile
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.extra.ccc.util.TextRemapperResult
import tl.extra.ccc.util.getTextRemapBytes
import tl.util.writeDatString
import java.io.ByteArrayOutputStream
import java.io.File

class ItemParam01Remapper(inBytes: ByteArray, outFile: File, translation: CccTranslation, translationOffset: Int) :
  ItemParamRemapper(inBytes, outFile, translation, translationOffset) {
  companion object {
    private const val EXPECTED_SIZE = 0x64B0
  }

  override fun run(relocationSpaceStartAddress: Int): TextRemapperResult {
    val itemEntries = ItemParam01BinFile(inBytes).entries

    val relocationOut = KioOutputStream(ByteArrayOutputStream())

    val out = KioOutputStream(ByteArrayOutputStream())
    out.writeBytes(inBytes.sliceArray(0x0..0xF))
    itemEntries.forEachIndexed { index, itemEntry ->
      val newName = translation.getTranslation(index * 4 + 0, translationOffset)
      val newText1 = translation.getTranslation(index * 4 + 1, translationOffset)
      val newText2 = translation.getTranslation(index * 4 + 2, translationOffset)
      val newText3 = translation.getTranslation(index * 4 + 3, translationOffset)

      arrayCopy(
        src = getTextRemapBytes(relocationSpaceStartAddress + relocationOut.pos()),
        dest = itemEntry.bytes,
        destPos = 0x0,
      )
      relocationOut.writeDatString(newName)
      arrayCopy(
        src = getTextRemapBytes(relocationSpaceStartAddress + relocationOut.pos()),
        dest = itemEntry.bytes,
        destPos = 0x5C,
      )
      relocationOut.writeDatString(newText1)
      arrayCopy(
        src = getTextRemapBytes(relocationSpaceStartAddress + relocationOut.pos()),
        dest = itemEntry.bytes,
        destPos = 0x8C,
      )
      relocationOut.writeDatString(newText2)
      arrayCopy(
        src = getTextRemapBytes(relocationSpaceStartAddress + relocationOut.pos()),
        dest = itemEntry.bytes,
        destPos = 0xBC,
      )
      relocationOut.writeDatString(newText3)

      out.writeBytes(itemEntry.bytes)
    }
    var outBytes = out.getAsByteArrayOutputStream().toByteArray()
    if (outBytes.size == EXPECTED_SIZE + 4) { // this is ok since last entry is missing its last word
      outBytes = outBytes.sliceArray(0 until EXPECTED_SIZE)
    }
    if (outBytes.size != EXPECTED_SIZE) {
      error(
        "Patched item_param_01.bin size mismatch, this is guaranteed to crash. " +
          "Got ${outBytes.size.toHex()}, expected ${EXPECTED_SIZE.toHex()}.",
      )
    }
    outFile.parentFile.mkdirs()
    outFile.writeBytes(outBytes)

    return TextRemapperResult(
      relocationOut.getAsByteArrayOutputStream().toByteArray(),
      relocationSpaceStartAddress + relocationOut.pos(),
    )
  }
}

class ItemParam04Remapper(inBytes: ByteArray, outFile: File, translation: CccTranslation, translationOffset: Int) :
  ItemParamRemapper(inBytes, outFile, translation, translationOffset) {
  companion object {
    private const val EXPECTED_SIZE = 0x4DB0
  }

  override fun run(relocationSpaceStartAddress: Int): TextRemapperResult {
    val itemEntries = ItemParam04BinFile(inBytes).entries

    val relocationOut = KioOutputStream(ByteArrayOutputStream())

    val out = KioOutputStream(ByteArrayOutputStream())
    out.writeBytes(inBytes.sliceArray(0x0..0xF))
    itemEntries.forEachIndexed { index, itemEntry ->
      val newName = translation.getTranslation(index * 3 + 0, translationOffset)
      val newText1 = translation.getTranslation(index * 3 + 1, translationOffset)
      val newText2 = translation.getTranslation(index * 3 + 2, translationOffset)

      arrayCopy(
        src = getTextRemapBytes(relocationSpaceStartAddress + relocationOut.pos()),
        dest = itemEntry.bytes,
        destPos = 0,
      )
      relocationOut.writeDatString(newName)
      arrayCopy(
        src = getTextRemapBytes(relocationSpaceStartAddress + relocationOut.pos()),
        dest = itemEntry.bytes,
        destPos = 0x50,
      )
      relocationOut.writeDatString(newText1)
      arrayCopy(
        src = getTextRemapBytes(relocationSpaceStartAddress + relocationOut.pos()),
        dest = itemEntry.bytes,
        destPos = 0x80,
      )
      relocationOut.writeDatString(newText2)

      out.writeBytes(itemEntry.bytes)
    }
    val outBytes = out.getAsByteArrayOutputStream().toByteArray()
    if (outBytes.size != EXPECTED_SIZE) {
      error(
        "Patched item_param_04.bin size mismatch, this is guaranteed to crash. " +
          "Got ${outBytes.size.toHex()}, expected ${EXPECTED_SIZE.toHex()}.",
      )
    }
    outFile.writeBytes(outBytes)

    return TextRemapperResult(
      relocationOut.getAsByteArrayOutputStream().toByteArray(),
      relocationSpaceStartAddress + relocationOut.pos(),
    )
  }
}

abstract class ItemParamRemapper(
  val inBytes: ByteArray,
  val outFile: File,
  val translation: CccTranslation,
  val translationOffset: Int,
) {
  abstract fun run(relocationSpaceStartAddress: Int): TextRemapperResult
}
