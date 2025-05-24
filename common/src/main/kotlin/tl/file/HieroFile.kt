package tl.file

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class HieroFile(fntFile: File, pngFile: File) {
  val fontImg: BufferedImage = ImageIO.read(pngFile)

  var lineHeight: Int = 0
    private set
  var base: Int = 0
    private set

  val chars: List<Char>
  val kernings: List<Kerning>
  val glyphs: Map<Int, BufferedImage>

  init {
    val chars = mutableListOf<Char>()
    val kernings = mutableListOf<Kerning>()
    fntFile.readLines().forEach { line ->
      val parts = line.replace(" +".toRegex(), " ").split(" ")
      val type = parts[0]
      fun partAsInt(n: Int): Int {
        return parts[n].split("=")[1].toInt()
      }
      when (type) {
        "common" -> {
          lineHeight = partAsInt(1)
          base = partAsInt(2)
        }
        "char" -> {
          chars.add(
            Char(
              partAsInt(1),
              partAsInt(2),
              partAsInt(3),
              partAsInt(4),
              partAsInt(5),
              partAsInt(6),
              partAsInt(7),
              partAsInt(8),
            ),
          )
        }
        "kerning" -> {
          kernings.add(Kerning(partAsInt(1), partAsInt(2), partAsInt(3)))
        }
      }
    }
    this.chars = chars
    this.kernings = kernings

    val glyphs = mutableMapOf<Int, BufferedImage>()
    chars.forEach {
      if (it.width != 0 && it.height != 0) {
        glyphs[it.id] = fontImg.getSubimage(it.x, it.y, it.width, it.height)
      }
    }
    this.glyphs = glyphs.toMap()
  }

  class Char(
    val id: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val xoffset: Int,
    val yoffset: Int,
    val xadvance: Int,
  )

  class Kerning(val first: Int, val second: Int, val amount: Int) {
    constructor(first: kotlin.Char, second: kotlin.Char, amount: Int) : this(first.code, second.code, amount)
  }
}
