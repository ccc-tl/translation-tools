package tl.extra.patcher

import kio.util.child
import tl.extra.extraToolkit
import tl.extra.extraUnpack
import tl.extra.patcher.tex.ConversionType
import tl.extra.patcher.tex.Png2Tex
import tl.extra.patcher.tex.TexConversion
import java.io.File

fun main() {
  Png2TexExtra(extraToolkit)
}

private class Png2TexExtra(baseDir: File) : Png2Tex(
  extraUnpack,
  baseDir.child("src/png2tex"),
  baseDir.child("src/custom-pak-files"),
  extract = false,
  verify = true
) {
  init {
    repackFont()
    repackNameEntry()
    repackTutorial02to04()
    repackTutorial09()
    repackTutorial41()
    repackTutorial66()
    repackBtlMenu()
    repackBtlEmi()
  }

  private fun repackFont() {
    repackTexture(
      "font", "font.txb",
      listOf(
        TexConversion(0, ConversionType.UNSWIZZLED_4BPP_PALETTED_NO_CCT)
      )
    )
  }

  private fun repackNameEntry() {
    repackTexture(
      "nameentry", "interface/nameentry/0000.txb",
      listOf(
        TexConversion(15, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(25, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(27, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT)
      )
    )
  }

  private fun repackTutorial02to04() {
    listOf("02", "03", "04").forEach { tutorialId ->
      repackTexture(
        "tutorial$tutorialId", "interface/tutorial/tutorial_other_$tutorialId.txb",
        listOf(
          TexConversion(4, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT)
        )
      )
    }
  }

  private fun repackTutorial09() {
    repackTexture(
      "tutorial09", "interface/tutorial/tutorial_other_09.txb",
      listOf(
        TexConversion(0, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT),
        TexConversion(1, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT)
      )
    )
  }

  private fun repackTutorial41() {
    repackTexture(
      "tutorial41", "interface/tutorial/tutorial_other_41.txb",
      listOf(
        TexConversion(0, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT),
        TexConversion(1, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT)
      )
    )
  }

  private fun repackTutorial66() {
    repackTexture(
      "tutorial66", "interface/tutorial/tutorial_other_66.txb",
      listOf(
        TexConversion(0, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT),
        TexConversion(1, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT),
        TexConversion(2, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT)
      )
    )
  }

  private fun repackBtlMenu() {
    repackTexture(
      "btl_menu", "battle/interface/btl_menu.txb",
      listOf(
        TexConversion(176, ConversionType.UNSWIZZLED_4BPP_PALETTED_NO_CCT)
      )
    )
  }

  private fun repackBtlEmi() {
    repackTexture(
      "btl_emi", "battle/interface/emi/0001.txb",
      listOf(
        TexConversion(0, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(17, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT)
      )
    )
  }
}
