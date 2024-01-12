package tl.tex

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min

class ImageWriter(
  val width: Int,
  val height: Int,
  private val removeAlphaMask: Boolean,
  private val argbAlphaMask: Int,
) {
  constructor(width: Int, height: Int) : this(width, height, false, -1)

  private val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

  private var xPos = 0
  private var yPos = 0

  fun writePixel(argbColor: Int) {
    if (removeAlphaMask && argbColor == argbAlphaMask) {
      img.setRGB(xPos, yPos, 0)
    } else {
      img.setRGB(xPos, yPos, argbColor)
    }
    xPos++
    if (xPos >= width) {
      yPos++
      xPos = 0
    }
  }

  fun eof(): Boolean {
    return xPos == width && yPos == height
  }

  fun writeToPng(file: File, outWidth: Int = width, outHeight: Int = height) {
    val outImg = if (outWidth == width && outHeight == height) {
      img
    } else {
      img.getSubimage(0, 0, min(img.width, outWidth), min(img.height, outHeight))
    }
    ImageIO.write(outImg, "PNG", file)
  }
}
