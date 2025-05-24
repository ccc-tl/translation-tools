package tl.extra.file.tex

import kio.KioInputStream
import kio.SequentialArrayReader
import kio.util.child
import kio.util.toUnsignedInt
import tl.tex.ImageWriter
import tl.util.findClosetsDivisibleBy
import java.io.File

@Suppress("UNUSED_VARIABLE")
class TexFile(
  bytes: ByteArray,
  private val removeAlphaMask: Boolean = false,
  private val collectBlocks: Boolean = false,
  private val collectChunks: Boolean = false,
) {
  constructor(file: File, removeAlphaMask: Boolean = false) : this(file.readBytes(), removeAlphaMask)

  companion object {
    private const val ALPHA_MASK = 0xFFFF0000.toInt()
  }

  private val outputImage: ImageWriter
  private val width: Int
  private val height: Int

  private val blocks: MutableList<ImageWriter> = mutableListOf()
  private val chunks: MutableList<ImageWriter> = mutableListOf()

  init {
    with(KioInputStream(bytes)) entry@{
      val unkA1 = readInt() // useless
      val unkA2 = readInt() // is palette present
      val unkA3 = readInt() // 0 -> from TXB, 8 -> from PSP.IM
      val unkA4 = readInt() // useless

      val texType = readShort()
      val swizzling = readShort() == 1.toShort()
      width = readShort().toInt()
      height = readShort().toInt()
      val unkWidth = readShort().toInt() // (?) somehow related to width
      val unkHeight = readShort() // always zero in CCC

      val texByteSize = readInt()
      val texOffset = readInt()

      val unkCCTPresent = readInt()
      val unkB2 = readInt() // useless
      val unkB3 = readInt() // useless

      val bpp: Int
      val palleted: Boolean
      when (texType) {
        3.toShort() -> {
          bpp = -1
          palleted = false
        }
        4.toShort() -> {
          bpp = 4
          palleted = true
        }
        5.toShort() -> {
          bpp = 8
          palleted = true
        }
        else -> {
          error("Unsupported texture type: $texType")
        }
      }

      println("Size: $width x $height, BPP $bpp")
      if (swizzling) {
        println("Texture is swizzled")
      }

      if (texByteSize == 0) {
        println("WARN: Texture does not contain any texture data, will write blank image.")
        outputImage = ImageWriter(width, height, removeAlphaMask, ALPHA_MASK)
        return@entry
      }
      val texBytes = readBytes(texByteSize)

      if (!palleted) {
        if (!swizzling) {
          error("Unexpected state, texture type 3 is not swizzled")
        }
        outputImage = processNonPalletedTexture(texBytes)
        return@entry
      }

      // Palette
      val palMode = readShort().toInt()
      val palColorNum = readShort() // guess, seems to work, generally 16 colors in 4 BPP mode, 256 colors in 8 BPP mode
      val palByteSize = readInt() // there are textures where this is incorrect for some reason - for example logo/0000.dat$tex3
      val unkP2 = readInt() // useless
      val unkP3 = readInt() // paletteOffset
      val palBytes: ByteArray
      val colorSize = when (palMode) {
        0 -> 2
        1 -> 2
        2 -> 2
        3 -> 4
        else -> error("Unsupported palette type: $palMode")
      }

      palBytes = readBytes(palColorNum * colorSize)
      val palette = createFatePalette(palBytes, palMode)

      val cct = parseChunkCompositionTable(this)

      val outImageWidth = findClosetsDivisibleBy(width, cct?.chunkSize ?: 32)
      val outImageHeight = findClosetsDivisibleBy(height, cct?.chunkSize ?: 32)
      println("Writing to image $outImageWidth x $outImageHeight")
      outputImage = ImageWriter(outImageWidth, outImageHeight, removeAlphaMask, ALPHA_MASK)

      val ignoreSwizzling = width <= 16
      if (ignoreSwizzling) {
        println("WARN: Texture too small, ignoring swizzle property")
      }

      if (swizzling && !ignoreSwizzling) {
        val unswizzler = PalettedUnswizzler(texBytes, width, bpp, cct != null, cct?.chunkSize)

        if (collectBlocks) {
          unswizzler.blocks.forEach { block ->
            val blockImage = if (bpp == 8) {
              ImageWriter(16, 8, removeAlphaMask, ALPHA_MASK)
            } else {
              ImageWriter(32, 8, removeAlphaMask, ALPHA_MASK)
            }
            block.bytes.forEach { pixelData ->
              if (bpp == 8) {
                blockImage.writePixel(palette[pixelData.toUnsignedInt()])
              } else {
                val twoPixelData = pixelData.toUnsignedInt()
                val pixelData1 = twoPixelData and 0b11110000 shr 4
                val pixelData2 = twoPixelData and 0b00001111
                blockImage.writePixel(palette[pixelData2])
                blockImage.writePixel(palette[pixelData1])
              }
            }
            blocks.add(blockImage)
          }
        }

        if (collectChunks) {
          unswizzler.chunks.forEach { chunk ->
            val chunkImage = if (cct == null) {
              ImageWriter(width, 8, removeAlphaMask, ALPHA_MASK)
            } else {
              ImageWriter(cct.chunkSize, cct.chunkSize, removeAlphaMask, ALPHA_MASK)
            }

            chunk.bytes.forEach { pixelData ->
              if (bpp == 8) {
                chunkImage.writePixel(palette[pixelData.toUnsignedInt()])
              } else {
                val twoPixelData = pixelData.toUnsignedInt()
                val pixelData1 = twoPixelData and 0b11110000 shr 4
                val pixelData2 = twoPixelData and 0b00001111
                chunkImage.writePixel(palette[pixelData2])
                chunkImage.writePixel(palette[pixelData1])
              }
            }
            chunks.add(chunkImage)
          }
        }

        val chunkWidth = cct?.chunkSize ?: width
        val chunkHeight = cct?.chunkSize ?: 8

        val chunksPerLine = outImageWidth / chunkWidth
        val pixelsPerChunkWidth = if (bpp == 8) chunkWidth else chunkWidth / 2

        var currentChunk = 0
        val chunksToWrite = cct?.table?.size ?: unswizzler.chunks.size

        // collect chunks for each chunk line, images consist of few chunk lines, each chunk line has few chunks
        val linesChunks = mutableListOf<MutableList<SequentialArrayReader>>()
        linesChunks.add(mutableListOf())
        var chunksToBeAssigned = chunksToWrite
        while (chunksToBeAssigned > 0) {
          if (linesChunks.last().size >= chunksPerLine) {
            linesChunks.add(mutableListOf())
          }

          if (cct != null) {
            val nextChunkId = cct.table[currentChunk]
            if (nextChunkId == -1) {
              linesChunks.last().add(SequentialArrayReader(ByteArray(1024)))
            } else {
              linesChunks.last().add(SequentialArrayReader(unswizzler.chunks[nextChunkId].bytes))
            }
          } else {
            linesChunks.last().add(SequentialArrayReader(unswizzler.chunks[currentChunk].bytes))
          }

          currentChunk++
          chunksToBeAssigned--
        }

        // last chunk line may have some chunks not set, just fill it with empty chunks
        while (linesChunks.last().size != chunksPerLine) {
          linesChunks.last().add(SequentialArrayReader(ByteArray(1024)))
        }

        // write chunks
        linesChunks.forEach { lineChunks ->
          repeat(chunkHeight) {
            lineChunks.forEach { chunk ->
              repeat(pixelsPerChunkWidth) {
                val pixelData = chunk.read()
                if (outputImage.eof()) {
                  return@entry
                }
                if (bpp == 8) {
                  outputImage.writePixel(palette[pixelData.toUnsignedInt()])
                  if (outputImage.eof()) {
                    println("WARN: Premature end of canvas, some pixel data not processed")
                  }
                } else if (bpp == 4) {
                  val twoPixelData = pixelData.toUnsignedInt()
                  val pixelData1 = twoPixelData and 0b11110000 shr 4
                  val pixelData2 = twoPixelData and 0b00001111
                  outputImage.writePixel(palette[pixelData2])
                  outputImage.writePixel(palette[pixelData1])
                  if (outputImage.eof()) {
                    println("WARN: Premature end of canvas, some pixel data not processed")
                  }
                } else {
                  error("Unsupported texture type")
                }
              }
            }
          }
        }
      } else {
        if (cct != null) {
          when {
            cct.table.size <= 2 -> {
              println("WARN: Unexpected CCT present in unswizzled texture however only one or zero chunks are listed in CCT, ignoring")
            }
            cct.isLinear() -> {
              println("WARN: Unexpected CCT present in unswizzled texture however CCT is linear, ignoring")
            }
            else -> {
              error("Unexpected CCT present in unswizzled texture")
            }
          }
        }

        texBytes.forEach { pixelData ->
          if (outputImage.eof()) {
            return@entry
          }
          if (bpp == 8) {
            outputImage.writePixel(palette[pixelData.toUnsignedInt()])
            if (outputImage.eof()) {
              println("WARN: Premature end of canvas, some pixel data not processed")
            }
          } else if (bpp == 4) {
            val twoPixelData = pixelData.toUnsignedInt()
            val pixelData1 = twoPixelData and 0b11110000 shr 4
            val pixelData2 = twoPixelData and 0b00001111
            outputImage.writePixel(palette[pixelData2])
            outputImage.writePixel(palette[pixelData1])
            if (outputImage.eof()) {
              println("WARN: Premature end of canvas, some pixel data not processed")
            }
          } else {
            error("Unsupported texture type")
          }
        }
      }
    }
  }

  private fun parseChunkCompositionTable(input: KioInputStream): Cct? {
    return with(input) {
      try {
        val unkC0 = readInt()
        if (unkC0 < 0) {
          // there seems to be interesting issue where some garbage is written at the end of txb file
          // it's most likely a bug in the original tool that was used to encode textures (it read data out of bounds)
          // normal CCT will have value larger than 0 there
          // example file is interface\cmn\i_con.txb
          println("CCT not present (although file was not ended).")
          null
        } else {
          val unkC1 = readInt()
          val cctChunkCount = readShort().toInt()
          var cctChunkSize = readShort().toInt()
          if (cctChunkSize == -1) {
            println("WARN: CCT chunk size is equal to -1, using default value 32. Texture may be unpacked incorrectly.")
            cctChunkSize = 32
          }
          val cctUniqueChunkCount = readShort() // guess
          val unkC3 = readShort()
          val unkC4 = readInt()

          if (cctUniqueChunkCount > 10000 || cctChunkSize > 20000) {
            throw IllegalStateException("CCT properties too large to be possible, ignoring CCT. Texture may be unpacked incorrectly.")
          }

          val cctList = mutableListOf<Int>()
          var leftChunks = cctChunkCount
          while (leftChunks > 0) {
            cctList.add(readShort().toInt())
            if (cctList.last() != -1) {
              leftChunks -= 1
            }
          }
          println(
            "CCT is present. CCT chunks: $cctChunkCount. CCT unique chunks: $cctUniqueChunkCount. " +
              "CCT actual chunks: ${cctList.size}. CCT chunk size: $cctChunkSize",
          )
          Cct(cctList, cctChunkSize)
        }
      } catch (e: Exception) {
        println("CCT not present (exception occurred).")
        null
      }
    }
  }

  private fun processNonPalletedTexture(texBytes: ByteArray): ImageWriter {
    val unswizzler = NonPalletedUnswizzler(texBytes, width)

    if (collectBlocks) {
      unswizzler.blocks.forEach { block ->
        val image = ImageWriter(4, 8, removeAlphaMask, ALPHA_MASK)
        writeNonPalettedTextureData(image, block.bytes)
        blocks.add(image)
      }
    }

    if (collectChunks) {
      unswizzler.chunks.forEach { chunk ->
        val image = ImageWriter(width, 8, removeAlphaMask, ALPHA_MASK)
        writeNonPalettedTextureData(image, chunk.bytes)
        chunks.add(image)
      }
    }

    val image = ImageWriter(width, height, removeAlphaMask, ALPHA_MASK)
    unswizzler.chunks.forEachIndexed { _, chunk ->
      writeNonPalettedTextureData(image, chunk.bytes)
    }
    return image
  }

  private fun writeNonPalettedTextureData(image: ImageWriter, bytes: ByteArray) {
    for (idx in bytes.indices step 4) {
      val r = bytes[idx].toUnsignedInt()
      val g = bytes[idx + 1].toUnsignedInt()
      val b = bytes[idx + 2].toUnsignedInt()
      val a = bytes[idx + 3].toUnsignedInt()
      val color = (a shl 24) or (r shl 16) or (g shl 8) or b

      if (removeAlphaMask && color == ALPHA_MASK) {
        image.writePixel(0)
      } else {
        image.writePixel(color)
      }
    }
  }

  fun writeToPng(outFile: File, cropOutImage: Boolean = true) {
    if (cropOutImage) {
      outputImage.writeToPng(outFile, width, height)
    } else {
      outputImage.writeToPng(outFile)
    }
  }

  fun writeBlocksToFolder(outFolder: File) {
    if (!collectBlocks) {
      error("Collecting blocks must be enabled to use this")
    }
    writeImagesToFolder(outFolder, blocks)
  }

  fun writeChunksToFolder(outFolder: File) {
    if (!collectChunks) {
      error("Collecting chunks must be enabled to use this")
    }
    writeImagesToFolder(outFolder, chunks)
  }

  private fun writeImagesToFolder(outFolder: File, images: List<ImageWriter>) {
    images.forEachIndexed { index, image ->
      image.writeToPng(outFolder.child("$index.png"))
    }
  }
}
