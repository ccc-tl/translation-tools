package tl.extra.patcher.tex

import java.io.File

enum class ConversionType {
  UNSWIZZLED_4BPP_PALETTED_NO_CCT,
  UNSWIZZLED_4BPP_CUSTOM_PALETTED_NO_CCT,
  SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
  SWIZZLED_4BPP_CUSTOM_PALETTED_LINEAR_CCT,
  SWIZZLED_4BPP_PALETTED_LINEAR_CCT_CORRECT_ALPHA,
  SWIZZLED_8BPP_PALETTED_LINEAR_CCT,
  SWIZZLED_8BPP_CUSTOM_PALETTED_LINEAR_CCT,
  ;

  fun convert(origBytes: ByteArray, pngFile: File, customPaletteBytesOverride: ByteArray?): ByteArray {
    println("Processing ${pngFile.absolutePath}...")
    return when (this) {
      UNSWIZZLED_4BPP_PALETTED_NO_CCT -> {
        PngToUnswizzled4BppPalettedTex(origBytes, pngFile).getTexBytes()
      }
      UNSWIZZLED_4BPP_CUSTOM_PALETTED_NO_CCT -> {
        PngToUnswizzled4BppCustomPalettedTex(origBytes, pngFile, customPaletteBytesOverride).getTexBytes()
      }
      SWIZZLED_4BPP_PALETTED_LINEAR_CCT -> {
        PngToSwizzled4BppPalettedLinearCctTex(origBytes, pngFile).getTexBytes()
      }
      SWIZZLED_4BPP_CUSTOM_PALETTED_LINEAR_CCT -> {
        PngToSwizzled4BppCustomPalettedLinearCctTex(origBytes, pngFile, customPaletteBytesOverride).getTexBytes()
      }
      SWIZZLED_4BPP_PALETTED_LINEAR_CCT_CORRECT_ALPHA -> {
        PngToSwizzled4BppPalettedLinearCctTex(origBytes, pngFile, correctAlpha = true).getTexBytes()
      }
      SWIZZLED_8BPP_PALETTED_LINEAR_CCT -> {
        PngToSwizzled8BppPalettedLinearCctTex(origBytes, pngFile).getTexBytes()
      }
      SWIZZLED_8BPP_CUSTOM_PALETTED_LINEAR_CCT -> {
        PngToSwizzled8BppCustomPalettedLinearCctTex(origBytes, pngFile, customPaletteBytesOverride).getTexBytes()
      }
    }
  }
}
