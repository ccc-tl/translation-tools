package tl.util

import java.awt.image.BufferedImage

fun BufferedImage.createCopy(): BufferedImage {
  val img = BufferedImage(this.width, this.height, this.type)
  val g = img.createGraphics()
  g.drawImage(this, 0, 0, null)
  g.dispose()
  return img
}

fun BufferedImage.isBlank(): Boolean {
  val buf = getRGB(0, 0, width, height, null, 0, width)
  return buf.all { it ushr 24 == 0 }
}
