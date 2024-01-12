package tl.util

import java.awt.image.BufferedImage

fun splitTileSheet(
  img: BufferedImage,
  tileWidth: Int,
  tileHeight: Int,
): MutableList<BufferedImage> {
  val glyphColumns = img.width / tileWidth
  val glyphRows = img.height / tileHeight
  val tiles = mutableListOf<BufferedImage>()
  repeat(glyphRows) { row ->
    repeat(glyphColumns) { column ->
      val glyphImg = img.getSubimage(column * tileWidth, row * tileHeight, tileWidth, tileHeight)
      tiles.add(glyphImg)
    }
  }
  return tiles
}
