package tl.extra.ccc.cli

import kio.util.child
import tl.extra.fateOutput
import tl.util.createCopy
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
  val baseDir = fateOutput.child("PortraitV2")
  val baseIn = baseDir.child("in")
  processPortraits(baseIn, baseDir.child("out-full"), false)
  processPortraits(baseIn, baseDir.child("out-crop"), true)
  println("Done")
}

private fun processPortraits(baseIn: File, baseOut: File, crop: Boolean) {
  baseOut.mkdir()
  val baseOverride = mapOf(
    "eli_2" to "eli",
    "emi_0_2" to "emi_0",
    "emi_1_2" to "emi_1",
    "emi_2_2" to "emi_2",
    "emi_3_2" to "emi_3",
    "emi_4_2" to "emi_4",
    "gil_0_2" to "gil_0",
    "gil_2_2" to "gil_2",
    "gil_3_2" to "gil_3",
    "gil_4_2" to "gil_4",
    "kar_2" to "kar",
    "ksk_2" to "ksk",
    "mks_2" to "mks",
    "ner_0_2" to "ner_0",
    "ner_1_2" to "ner_1",
    "ner_2_2" to "ner_2",
    "ner_3_2" to "ner_3",
    "ner_4_2" to "ner_4",
    "ran_0_2" to "ran_0",
    "ran_1_2" to "ran_1",
    "rin_0_2" to "rin_0",
    "rin_1_2" to "rin_1",
    "rin_2_2" to "rin_2",
    "sak_0_2" to "sak_0",
    "sak_1_2" to "sak_1",
    "sak_2_2" to "sak_2",
    "sak_3_2" to "sak_3",
    "sin_0_2" to "sin_0",
    "sin_1_2" to "sin_1",
    "tam_0_2" to "tam_0",
    "tam_1_2" to "tam_1",
    "tam_2_2" to "tam_2",
    "tam_3_2" to "tam_3",
    "tam_4_2" to "tam_4",
    "zin_2" to "zin"
  )
  baseIn.listFiles()!!.forEach { inDir ->
    println("Processing ${inDir.name}...")
    val outDir = baseOut.child(inDir.name)
    PortraitBuilder(inDir, getCustomBaseFromDir(baseIn, baseOverride[inDir.name]), crop)
      .merge(outDir)
  }
}

private fun getCustomBaseFromDir(baseIn: File, name: String?): File? {
  if (name == null) {
    return null
  }
  val files = baseIn.child(name).listFiles() ?: return null
  return files.single { it.name.startsWith("0 -") }
}

class PortraitBuilder(private val inDir: File, private val customBase: File?, private val crop: Boolean = false) {
  fun merge(outDir: File) {
    outDir.mkdir()
    val files = inDir.listFiles()!!.toList()
    if (files.size == 1) {
      handleSingle(files[0], outDir)
    } else {
      handleMultiple(files, outDir)
    }
  }

  private fun handleSingle(file: File, outDir: File) {
    val faceImage = ImageIO.read(file)
    ImageIO.write(cropFace(faceImage), "PNG", outDir.child("${inDir.name}_1.png"))
  }

  private fun handleMultiple(files: List<File>, outDir: File) {
    val images = files
      .map { it.name.split(" - ")[0].toInt() to it }
      .sortedBy { it.first }
      .map { it.second }
      .toMutableList()

    val baseImage = if (customBase != null) {
      ImageIO.read(customBase)
    } else {
      val baseFile = images.removeAt(0)
      ImageIO.read(baseFile)
    }

    images.forEachIndexed { index, faceFile ->
      val faceImage = ImageIO.read(faceFile)
      val mergedImage = baseImage.createCopy()
      val g = mergedImage.createGraphics()
      g.drawImage(faceImage, 0, 0, null)
      g.dispose()
      val outIndex = if (customBase != null) {
        index
      } else {
        index + 1
      }
      ImageIO.write(cropFace(mergedImage), "PNG", outDir.child("${inDir.name}_$outIndex.png"))
    }
  }

  private fun cropFace(image: BufferedImage) = if (crop) image.getSubimage(0, 23, 150, 130) else image
}
