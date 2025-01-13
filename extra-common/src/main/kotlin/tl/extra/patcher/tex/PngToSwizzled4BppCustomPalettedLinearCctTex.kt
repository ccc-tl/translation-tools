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
class PngToSwizzled4BppCustomPalettedLinearCctTex(origBytes: ByteArray, pngFile: File, paletteBytesOverride: ByteArray?) {
  private val outStream = ByteArrayOutputStream()
  private val paletteBytesGenerator = PaletteBytesGenerator(16)

  init {
    with(KioInputStream(origBytes)) {
      val headerTop = readBytes(0x10) // this will never change
      val textureType = readShort()
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

      val newImage = ImageIO.read(pngFile)

      val palettePos = pos()
      val palMode = readShort()
      val palColorNum = readShort()
      val palByteSize = readInt()
      val unkP2 = readInt() // useless
      val unkP3 = readInt() // paletteOffset
      val palBytes = readBytes(palColorNum.toInt())

      val newPalBytes = paletteBytesOverride ?: paletteBytesGenerator.fromImage(newImage)
      val palette = ColorPalette(newPalBytes, ColorPalette.Mode.RGBA8888)

      val unkC0 = readInt()
      val unkC1 = readInt()
      val cctChunkCount = readShort().toInt()
      val cctChunkSize = readShort().toInt()
      val cctUniqueChunkCount = readShort().toInt()
      val unkC3 = readShort().toInt()
      val unkC4 = readInt()

      val newImageDataWidth = findClosetsDivisibleBy(newImage.width, 32)
      val newImageDataHeight = findClosetsDivisibleBy(newImage.height, 32)
      val newImageReader = ImageReader(newImage, newImageDataWidth, newImageDataHeight)
      val newTexDataSize = newImageDataWidth * newImageDataHeight / 2

      val newTexBytesStream = ByteArrayOutputStream()
      repeat(newTexDataSize) {
        val color1 = newImageReader.nextPixel()
        val color2 = newImageReader.nextPixel()
        val colorId1 = palette.colors.indexOfFirst { it == color1 }
        val colorId2 = palette.colors.indexOfFirst { it == color2 }
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
        writeShort(4)
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
        writeShort(3)
        writeShort(16)
        writeInt(64)
        writeInt(unkP2)
        writeInt(0) // paletteOffset?
        writeBytes(newPalBytes)
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
