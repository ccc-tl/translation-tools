package tl.extra.patcher.tex

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.toWHex
import tl.extra.file.tex.createFatePalette
import tl.tex.ImageReader
import tl.util.findClosetsDivisibleBy
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

@Suppress("UNUSED_VARIABLE")
class PngToSwizzled4BppPalettedLinearCctTex(origBytes: ByteArray, pngFile: File, correctAlpha: Boolean = false) {
  private val outStream = ByteArrayOutputStream()

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

      if (swizzled == 0) {
        println("WARN: This texture wasn't originally swizzled")
      }
      if (unkCCTPresent == 0) {
        println("WARN: This texture originally did not contain CCT")
      }

      setPos(0x30)
      readBytes(texDataSize)

      val palettePos = pos()
      val paletteHeader = readBytes(0x10)
      val palMode = readShort(at = palettePos).toInt()
      val palSize = readShort(at = palettePos + 0x4).toInt()
      val palette = createFatePalette(readBytes(palSize), palMode)

      val unkC0 = readInt()
      val unkC1 = readInt()
      val cctChunkCount = readShort().toInt()
      val cctChunkSize = readShort().toInt()
      val cctUniqueChunkCount = readShort().toInt()
      val unkC3 = readShort().toInt()
      val unkC4 = readInt()

      val newImage = ImageIO.read(pngFile)
      val newImageDataWidth = findClosetsDivisibleBy(newImage.width, 32)
      val newImageDataHeight = findClosetsDivisibleBy(newImage.height, 32)
      val newImageReader = ImageReader(newImage, newImageDataWidth, newImageDataHeight)
      val newTexDataSize = newImageDataWidth * newImageDataHeight / 2

      val newTexBytesStream = ByteArrayOutputStream()
      repeat(newTexDataSize) {
        var color1 = newImageReader.nextPixel()
        var color2 = newImageReader.nextPixel()
        if (correctAlpha) {
          if (color1 == 0x01000000 || color1 == 0x02000000 || color1 == 0x03000000) color1 = 0
          if (color2 == 0x01000000 || color2 == 0x02000000 || color2 == 0x03000000) color2 = 0
        }
        val colorId1 = palette.getClosestColor(color1)
        val colorId2 = palette.getClosestColor(color2)
        if (colorId1 == -1 || colorId2 == -1) {
          error("Failed to package texture data, color approximation returned -1 for colors: ${color1.toWHex()}, ${color2.toWHex()}")
        }
        val encoded = (colorId2 shl 4) or colorId1
        newTexBytesStream.write(encoded)
      }
      val swizzleResult = swizzle4Bpp(
        newTexBytesStream.toByteArray(),
        newImageDataWidth,
        newImageDataHeight,
      )

      with(KioOutputStream(outStream)) {
        writeBytes(headerTop)
        writeShort(1) // swizzled
        writeShort(newImage.width.toShort())
        writeShort(newImage.height.toShort())
        writeShort(newImageDataWidth.toShort())
        writeShort(0) // unkHeight
        writeInt(newTexDataSize)
        writeInt(0) // texOffset
        writeInt(1) // unkCCTPresent
        writeInt(unkB2) // useless
        writeInt(unkB3) // useless
        writeBytes(swizzleResult.bytes)
        writeBytes(paletteHeader)
        writeBytes(palette.paletteBytes)
        writeInt(swizzleResult.chunksPerLineCount) // unkC0
        writeInt(swizzleResult.chunkLinesCount) // unkC1
        writeShort(swizzleResult.chunks.size.toShort()) // cctChunkCount
        writeShort(32) // cctChunkSize
        writeShort(swizzleResult.chunks.size.toShort()) // cctUniqueChunkCount
        writeShort(unkC3.toShort())
        writeInt(unkC4)
        swizzleResult.chunks.forEach {
          writeShort(it.id.toShort())
        }
      }
    }
  }

  fun getTexBytes(): ByteArray {
    return outStream.toByteArray()
  }
}
