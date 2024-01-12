package tl.file

import kio.KioInputStream
import kio.util.toUnsignedInt
import java.io.File

@Suppress("UNUSED_VARIABLE")
class GimFile(val bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  val blocks: List<GimBlock>
  val imageFormats: List<GimImageFormat>
  val pixelOrders: List<GimPixelOrder>

  init {
    val blocks = mutableListOf<GimBlock>()
    val imageFormats = mutableListOf<GimImageFormat>()
    val pixelOrders = mutableListOf<GimPixelOrder>()
    with(KioInputStream(bytes)) {
      if (readString(11) != "MIG.00.1PSP") {
        error("Not a GIM file")
      }
      setPos(0x10)
      while (!eof()) {
        val pos = pos()
        val blockType = readShort().toUnsignedInt()
        val blockUnk = readShort()
        val blockSize = readInt()
        val nextBlock = pos + readInt()
        val blockData = pos + readInt()
        temporaryJump(blockData) {
          blocks.add(GimBlock(pos, blockType))
          if (blockType == 4) {
            setPos(blockData + 0x4)
            val imageFormat = readShort().toUnsignedInt()
            val pixelOrder = readShort().toUnsignedInt()
            imageFormats.add(GimImageFormat.forId(imageFormat))
            pixelOrders.add(GimPixelOrder.forId(pixelOrder))
          }
        }
        setPos(nextBlock)
      }
      close()
    }
    this.blocks = blocks
    this.imageFormats = imageFormats
    this.pixelOrders = pixelOrders
  }
}

class GimBlock(val pos: Int, val type: Int)

enum class GimImageFormat(val id: Int) {
  RGBA5650(0),
  RGBA5551(1),
  RGBA4444(2),
  RGBA8888(3),
  INDEX4(4),
  INDEX8(5);

  companion object {
    fun forId(id: Int): GimImageFormat {
      return entries.single { it.id == id }
    }
  }

  fun getColorCount(): Int {
    return when (this) {
      INDEX4 -> 16
      INDEX8 -> 256
      else -> error("Format '${toGimConvString()}' is not indexed")
    }
  }

  fun toGimConvString(): String {
    return super.toString().lowercase()
  }
}

enum class GimPixelOrder(val id: Int) {
  NORMAL(0),
  FASTER(1);

  companion object {
    fun forId(id: Int): GimPixelOrder {
      return entries.single { it.id == id }
    }
  }

  fun toGimConvString(): String {
    return super.toString().lowercase()
  }
}
