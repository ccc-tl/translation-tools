package tl.extra.file.tex

import tl.tex.ColorPalette

fun createFatePalette(palBytes: ByteArray, palMode: Int): ColorPalette {
  return when (palMode) {
    0 -> ColorPalette(palBytes, ColorPalette.Mode.BGR565)
    1 -> ColorPalette(palBytes, ColorPalette.Mode.GRAB4444)
    2 -> ColorPalette(palBytes, ColorPalette.Mode.GRAB4444)
    3 -> ColorPalette(palBytes, ColorPalette.Mode.RGBA8888)
    else -> error("Unsupported palette type $palMode")
  }
}
