package tl.extra.patcher.tex

import kio.KioInputStream
import kio.KioOutputStream
import tl.tex.ColorPalette
import tl.tex.ImageReader
import tl.util.findClosetsDivisibleBy
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

@Suppress("UNUSED_VARIABLE")
class PngToUnswizzled4BppCustomPalettedTex(origBytes: ByteArray, pngFile: File, paletteBytesOverride: ByteArray?) {
  private val outStream = ByteArrayOutputStream()
  private val paletteBytesGenerator = PaletteBytesGenerator(16)

  init {
    with(KioInputStream(origBytes)) {
      val headerTop = readBytes(0x12) // this will never change
      val swizzled = readShort().toInt()
      val width = readShort().toInt()
      val height = readShort().toInt()
      val unkWidth = readShort().toInt()
      val unkHeight = readShort().toInt()
      val texDataSize = readInt()
      val texOffset = readInt()
      val unkCCTPresent = readInt()
      val unkB2 = readInt()
      val unkB3 = readInt()

      if (swizzled == 1) {
        println("WARN: This texture was originally swizzled")
      }
      if (unkCCTPresent == 1) {
        println("WARN: This texture originally contained CCT")
      }

      setPos(0x30)
      readBytes(texDataSize)

      val newImage = ImageIO.read(pngFile)

      val palettePos = pos()
      val paletteHeader = readBytes(0x10)
      val palMode = readShort(at = palettePos).toInt()
      val palSize = readShort(at = palettePos + 0x4).toInt()
      val palBytes = readBytes(palSize)

      val newPalBytes = paletteBytesOverride ?: paletteBytesGenerator.fromImage(newImage)
      val palette = ColorPalette(newPalBytes, ColorPalette.Mode.RGBA8888)

      val newImageDataWidth = findClosetsDivisibleBy(newImage.width, 32)
      val newImageDataHeight = findClosetsDivisibleBy(newImage.height, 16)
      val newImageReader = ImageReader(newImage, newImageDataWidth, newImageDataHeight)
      val newTexDataSize = newImageDataWidth * newImageDataHeight / 2

      val newTexBytes = ByteArrayOutputStream()
      repeat(newTexDataSize) {
        val color1 = newImageReader.nextPixel()
        val color2 = newImageReader.nextPixel()
        val colorId1 = palette.colors.indexOfFirst { it == color1 }
        val colorId2 = palette.colors.indexOfFirst { it == color2 }
        val encoded = (colorId2 shl 4) or colorId1
        newTexBytes.write(encoded)
      }

      with(KioOutputStream(outStream)) {
        writeBytes(headerTop)
        writeShort(0) // not swizzled
        writeShort(newImage.width.toShort())
        writeShort(newImageDataHeight.toShort())
        writeShort(newImageDataWidth.toShort())
        writeShort(0) // unkHeight
        writeInt(newTexDataSize)
        writeInt(0) // texOffset
        writeInt(0) // unkCCTPresent
        writeInt(unkB2) // useless
        writeInt(unkB3) // useless
        writeBytes(newTexBytes.toByteArray())
        writeShort(3)
        writeShort(16)
        writeInt(64)
        writeInt(0)
        writeInt(0) // paletteOffset?
        writeBytes(newPalBytes)
      }
    }
  }

  fun getTexBytes(): ByteArray {
    return outStream.toByteArray()
  }
}
