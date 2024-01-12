package tl.tex

import kio.KioInputStream
import kio.util.child
import kio.util.execute
import kio.util.getSubArrayPos
import kio.util.relativizePath
import kio.util.walkDir
import java.io.File

class GmoTextureConverter(
  private val gimConvTool: File,
  private val srcDir: File,
  private val fileFilter: (File) -> Boolean,
) {
  fun convertTo(outDir: File) {
    outDir.mkdirs()
    val tmpGim = outDir.child("tmp.gim")
    var convertedGmoCount = 0
    var convertedGimCount = 0

    walkDir(srcDir) {
      if (fileFilter(it)) {
        println("Processing ${it.relativizePath(srcDir)}")
        val textures = getGmoTextures(it)
        println("Found ${textures.size} textures")
        textures.forEachIndexed { index, texture ->
          tmpGim.writeBytes(texture.bytes)
          val outFile = outDir.child("${it.relativizePath(srcDir).replace("/", "$")}!tex$index.png")
          execute(gimConvTool, args = listOf(tmpGim, "-o", outFile))
          convertedGimCount++
        }
        convertedGmoCount++
        println()
      }
    }
    tmpGim.delete()
    println("Processed $convertedGmoCount files")
    println("Should have created $convertedGimCount files")
  }
}

fun getGmoTextures(gmoFile: File): List<GmoTexture> {
  val gimMagic = "MIG.00.1PSP".toByteArray()
  val gmoBytes = gmoFile.readBytes()
  val gmoInput = KioInputStream(gmoBytes)
  val textures = mutableListOf<GmoTexture>()
  var startFrom = 0
  while (true) {
    val texPos = getSubArrayPos(gmoBytes, gimMagic, startFrom)
    if (texPos == -1) {
      break
    }
    gmoInput.setPos(texPos - 0x4)
    val texSize = gmoInput.readInt()
    val texStart = gmoInput.pos()
    textures.add(GmoTexture(texStart, texSize, gmoInput.readBytes(texSize)))
    startFrom = gmoInput.pos()
  }
  return textures
}

class GmoTexture(val startPos: Int, val size: Int, val bytes: ByteArray)
