package tl.tex

import kio.util.mapValue
import kio.util.toUnsignedInt
import java.io.File

class ColorPalette(val paletteBytes: ByteArray, val mode: Mode) {
  lateinit var colors: IntArray
    private set

  init {
    when (mode) {
      Mode.RGBA8888 -> parseColorsRGBA8888()
      Mode.GRAB4444 -> parseColorsGRAB4444()
      Mode.BGR565 -> parseColorsBGR565()
    }
  }

  private fun parseColorsRGBA8888() {
    colors = IntArray(paletteBytes.size / 4)
    repeat(colors.size) { index ->
      val palOffset = index * 4
      val r = paletteBytes[palOffset].toUnsignedInt()
      val g = paletteBytes[palOffset + 1].toUnsignedInt()
      val b = paletteBytes[palOffset + 2].toUnsignedInt()
      val a = paletteBytes[palOffset + 3].toUnsignedInt()
      colors[index] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
  }

  private fun parseColorsGRAB4444() {
    colors = IntArray(paletteBytes.size / 2)
    repeat(colors.size) { index ->
      val palOffset = index * 2
      val gr = paletteBytes[palOffset].toUnsignedInt()
      val ab = paletteBytes[palOffset + 1].toUnsignedInt()
      val g = mapValue((gr ushr 4).toFloat(), 0f, 15f, 0f, 255f).toInt()
      val r = mapValue((gr and 0xF).toFloat(), 0f, 15f, 0f, 255f).toInt()
      val a = mapValue((ab ushr 4).toFloat(), 0f, 15f, 0f, 255f).toInt()
      val b = mapValue((ab and 0xF).toFloat(), 0f, 15f, 0f, 255f).toInt()

      colors[index] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
  }

  private fun parseColorsBGR565() {
    colors = IntArray(paletteBytes.size / 2)
    repeat(colors.size) { index ->
      val palOffset = index * 2
      val gr = paletteBytes[palOffset].toUnsignedInt()
      val bg = paletteBytes[palOffset + 1].toUnsignedInt()
      val color = (bg shl 8) or gr
      val b = mapValue((color ushr 11).toFloat(), 0f, 31f, 0f, 255f).toInt()
      val g = mapValue((color ushr 5 and 0x3F).toFloat(), 0f, 63f, 0f, 255f).toInt()
      val r = mapValue((color and 0x1F).toFloat(), 0f, 31f, 0f, 255f).toInt()
      val a = 0xFF

      colors[index] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
  }

  fun getClosestColor(color: Int): Int {
    if ((color ushr 24 and 0xFF) == 0) {
      val directAlpha = colors.indexOfFirst { it ushr 24 and 0xFF == 0 }
      if (directAlpha != -1) return directAlpha
      println("WARN: No direct alpha found for color with full alpha")
    }
    return colors
      .mapIndexed { index, palColor -> index to colorDistance(color, palColor) }
      .minByOrNull { it.second }!!.first
  }

  private fun colorDistance(argbColor1: Int, argbColor2: Int): Double {
    fun channelDistance(ch1: Double, ch2: Double, alphaDiff: Double): Double {
      val diff = ch1 - ch2
      return diff * diff + (diff + alphaDiff) * (diff + alphaDiff)
    }

    val c1a = (argbColor1 ushr 24 and 0xFF) / 255.0
    val c1r = (argbColor1 ushr 16 and 0xFF) / 255.0
    val c1g = (argbColor1 ushr 8 and 0xFF) / 255.0
    val c1b = (argbColor1 and 0xFF) / 255.0
    val c2a = (argbColor2 ushr 24 and 0xFF) / 255.0
    val c2r = (argbColor2 ushr 16 and 0xFF) / 255.0
    val c2g = (argbColor2 ushr 8 and 0xFF) / 255.0
    val c2b = (argbColor2 and 0xFF) / 255.0

    val alphaDiff = c2a - c1a
    return channelDistance(c1r, c2r, alphaDiff) +
      channelDistance(c1g, c2g, alphaDiff) +
      channelDistance(c1b, c2b, alphaDiff)
  }

  fun writeToFile(file: File) {
    val width: Int
    val height: Int
    when (colors.size) {
      16 -> {
        width = 4
        height = 4
      }
      256 -> {
        width = 16
        height = 16
      }
      else -> error("Unsupported palette dimensions for palette image")
    }
    val image = ImageWriter(width, height)
    colors.forEach { image.writePixel(it) }
    image.writeToPng(file)
  }

  operator fun get(index: Int): Int {
    return colors[index]
  }

  enum class Mode {
    RGBA8888, BGR565, GRAB4444
  }
}
