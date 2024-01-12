package tl.file

import kio.KioInputStream
import kio.util.toUnsignedInt
import java.io.File

class GmoFile(val bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  val start: GmoContainerBlock
  val file: GmoContainerBlock
  val blocks: List<GmoBlock>

  init {
    val blocks = mutableListOf<GmoBlock>()
    with(KioInputStream(bytes)) {
      if (readString(11) != "OMG.00.1PSP") {
        error("Not a GMO file")
      }
      setPos(0x10)
      if (readShort().toUnsignedInt() != 0x2) {
        error("Expected GMO start block")
      }
      val startHeaderSize = readShort().toUnsignedInt()
      start = GmoContainerBlock(0x2, startHeaderSize, readInt(), readBytes(startHeaderSize - 0x8))
      if (readShort().toUnsignedInt() != 0x3) {
        error("Expected GMO file block")
      }
      val fileHeaderSize = readShort().toUnsignedInt()
      file = GmoContainerBlock(0x3, fileHeaderSize, readInt(), readBytes(fileHeaderSize - 0x8))

      while (!eof()) {
        val pos = pos()
        val type = readShort().toUnsignedInt()
        if (type == 0x2 || type == 0x3) {
          error("Unexpected container block encountered during parsing")
        }
        val headerSize = readShort().toUnsignedInt()
        val size = readInt()
        val headerStart = pos + 0x8
        val headerEndDataStart = if (headerSize == 0) headerStart else pos + headerSize
        val dataEnd = pos + size
        val header = readBytes(headerEndDataStart - headerStart)
        val data = readBytes(dataEnd - headerEndDataStart)
        blocks.add(GmoBlock(pos, type, headerSize, size, header, data))
        setPos(pos + size)
      }
      close()
    }
    this.blocks = blocks.toMutableList()
  }
}

class GmoContainerBlock(
  val type: Int,
  val headerSize: Int,
  val fileSize: Int,
  val header: ByteArray,
)

class GmoBlock(
  val pos: Int,
  val type: Int,
  val headerSize: Int,
  val size: Int,
  val header: ByteArray,
  val data: ByteArray,
)
