package tl.cli

import kio.util.child
import tl.util.splitTileSheet
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
  if (args.size != 4) {
    println("Usage: [file] [outDir] [tileWidth] [tileHeight]")
    return
  }
  val file = File(args[0])
  val outDir = File(args[1])
  val tileWidth = args[2].toInt()
  val tileHeight = args[3].toInt()

  val img = ImageIO.read(file)
  if (!outDir.exists() || outDir.listFiles()!!.isNotEmpty()) {
    error("Output directory does not exist or is not empty")
  }
  splitTileSheet(img, tileWidth, tileHeight).forEachIndexed { id, tile ->
    ImageIO.write(tile, "PNG", outDir.child("$id.png"))
  }
  println("Done")
}
