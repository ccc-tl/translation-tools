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
class PngToSwizzled8BppPalettedLinearCctTex(origBytes: ByteArray, pngFile: File) {
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
      val newImageDataWidth = findClosetsDivisibleBy(newImage.width, 16)
      val newImageDataHeight = findClosetsDivisibleBy(newImage.height, 16)
      val newImageReader = ImageReader(newImage, newImageDataWidth, newImageDataHeight)
      val newTexDataSize = newImageDataWidth * newImageDataHeight

      val newTexBytesStream = ByteArrayOutputStream()
      repeat(newTexDataSize) {
        val color = newImageReader.nextPixel()
        val colorId = palette.getClosestColor(color)
        if (colorId == -1) {
          error("Failed to package texture data, color approximation returned -1 for color: ${color.toWHex()}")
        }
        newTexBytesStream.write(colorId)
      }
      val swizzleResult =
        swizzle8Bpp(
          newTexBytesStream.toByteArray(),
          newImageDataWidth,
          newImageDataHeight
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
        writeShort(16) // cctChunkSize
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
