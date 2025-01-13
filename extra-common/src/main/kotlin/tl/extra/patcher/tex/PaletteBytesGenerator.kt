package tl.extra.patcher.tex

import kio.KioOutputStream
import tl.tex.ImageReader
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

class PaletteBytesGenerator(
  private val maxColors: Int,
  private val paletteSize: Int = maxColors * 4,
) {
  fun fromImage(image: BufferedImage): ByteArray {
    val reader = ImageReader(image)
    val colorsSet = mutableSetOf<Int>()
    while (!reader.eof()) {
      colorsSet.add(reader.nextPixel())
    }
    if (colorsSet.size > maxColors) {
      error("Too much colors after reading image: ${colorsSet.size}")
    }
    val out = KioOutputStream(
      ByteArrayOutputStream(),
      littleEndian = false,
    )
    colorsSet.forEach {
      out.writeInt((it shl 8) or (it ushr 24))
    }
    while (out.pos() < paletteSize) {
      out.writeInt(0)
    }
    return out.getAsByteArrayOutputStream().toByteArray()
  }
}
