package tl.extra.file.tex

import kio.SequentialArrayReader
import kio.SequentialArrayWriter

class NonPalletedUnswizzler(val input: ByteArray, width: Int) {
  val blocks: List<Block>
  val chunks: List<Chunk>

  init {
    val blockWidth = 4
    val blockHeight = 8
    val blockSize = blockWidth * blockHeight * 4
    val availableBlocks = input.size / blockSize
    println("Available blocks: $availableBlocks")
    blocks = mutableListOf()
    repeat(availableBlocks) { blockIdx ->
      val blockStart = blockSize * blockIdx
      blocks.add(Block(blockIdx, input.copyOfRange(blockStart, blockStart + blockSize)))
    }

    val blocksPerChunk = width / blockWidth
    val availableChunks = availableBlocks / blocksPerChunk
    println("Available chunks: $availableChunks")
    chunks = mutableListOf()

    // given sequential blocks: 0 1 2 3 4 5 6 7 ...
    // chunk is composed in sequence:
    // SeqPartA: 0 1 2 3 4 5 6 7 ... (depends on how many chunks can fit in image width)

    repeat(availableChunks) { chunkIdx ->
      val chunk = Chunk(chunkIdx, ByteArray(blockSize * blocksPerChunk))
      chunks.add(chunk)
      val chunkWriter = SequentialArrayWriter(chunk.bytes)

      var blockIdx = 0
      // for each SeqPartX
      val startBlockIndex = chunkIdx * blocksPerChunk + blockIdx
      val endBlockIndex = startBlockIndex + blocksPerChunk
      val chunkBlocks = blocks.slice(startBlockIndex until endBlockIndex).map { SequentialArrayReader(it.bytes) }
      blockIdx += blocksPerChunk

      repeat(blockHeight) {
        repeat(blocksPerChunk) { idx ->
          repeat(blockWidth) {
            chunkWriter.write(chunkBlocks[idx].read()) // one pixel is encoded by 4 bytes
            chunkWriter.write(chunkBlocks[idx].read())
            chunkWriter.write(chunkBlocks[idx].read())
            chunkWriter.write(chunkBlocks[idx].read())
          }
        }
      }
    }
  }
}

class PalettedUnswizzler(val input: ByteArray, width: Int, bpp: Int, cctPresent: Boolean, cctChunkWidth: Int?) {
  val blocks: List<Block>
  val chunks: MutableList<Chunk>

  private val blockWidth = 16 // here we are still operating on raw data so width is 16 bytes for both 8 and 4 BPP
  private val blockHeight = 8
  private val blockSize = blockWidth * blockHeight

  init {
    val availableBlocks = input.size / blockSize
    println("Available blocks: $availableBlocks")
    blocks = mutableListOf()
    repeat(availableBlocks) { blockIdx ->
      val blockStart = blockSize * blockIdx
      blocks.add(
        Block(
          blockIdx,
          input.copyOfRange(blockStart, blockStart + blockSize)
        )
      )
    }

    val blocksPerChunk = if (bpp == 8) {
      if (cctPresent) {
        if (cctChunkWidth!! == 32) {
          8
        } else {
          2
        }
      } else {
        width / 16
      }
    } else {
      if (cctPresent) {
        4
      } else {
        width / 32
      }
    }
    val availableChunks = availableBlocks / blocksPerChunk
    println("Available chunks: $availableChunks")
    chunks = mutableListOf()
    if (bpp == 8) {
      if (cctPresent) {
        if (cctChunkWidth!! == 32) {
          processChunk8Bpp32Width(availableChunks, blocksPerChunk)
        } else {
          processChunk8Bpp(availableChunks, blocksPerChunk)
        }
      } else {
        processLinearChunk(availableChunks, blocksPerChunk)
      }
    } else {
      if (cctPresent) {
        processChunk4Bpp(availableChunks, blocksPerChunk)
      } else {
        processLinearChunk(availableChunks, blocksPerChunk)
      }
    }
  }

  private fun processChunk8Bpp32Width(availableChunks: Int, blocksPerChunk: Int) {
    // given sequential blocks: 0 1 2 3 4 5 6 7
    // chunk is composed in sequence:
    // SeqPartA: 0 1
    // SeqPartB: 2 3
    // SeqPartC: 4 5
    // SeqPartD: 6 7

    repeat(availableChunks) { chunkIdx ->
      val chunk = Chunk(chunkIdx, ByteArray(blockSize * blocksPerChunk))
      chunks.add(chunk)
      val chunkWriter = SequentialArrayWriter(chunk.bytes)

      var blockIdx = 0
      // for each SeqPartX
      repeat(4) {
        val blockA = SequentialArrayReader(blocks[chunkIdx * blocksPerChunk + blockIdx].bytes)
        val blockB = SequentialArrayReader(blocks[chunkIdx * blocksPerChunk + blockIdx + 1].bytes)
        blockIdx += 2

        repeat(blockHeight) {
          repeat(blockWidth) {
            chunkWriter.write(blockA.read())
          }
          repeat(blockWidth) {
            chunkWriter.write(blockB.read())
          }
        }
      }
    }
  }

  private fun processChunk8Bpp(availableChunks: Int, blocksPerChunk: Int) {
    // given sequential blocks: 0 1 2 3 4 5 6 7
    // chunk is composed in sequence:
    // SeqPartA: 0
    // SeqPartB: 1

    repeat(availableChunks) { chunkIdx ->
      val chunk = Chunk(chunkIdx, ByteArray(blockSize * blocksPerChunk))
      chunks.add(chunk)
      val chunkWriter = SequentialArrayWriter(chunk.bytes)
      var blockIdx = 0
      // for each SeqPartX
      repeat(2) {
        val block = SequentialArrayReader(blocks[chunkIdx * blocksPerChunk + blockIdx].bytes)
        blockIdx += 1

        repeat(blockHeight) {
          repeat(blockWidth) {
            chunkWriter.write(block.read())
          }
        }
      }
    }
  }

  private fun processChunk4Bpp(availableChunks: Int, blocksPerChunk: Int) {
    // given sequential blocks: 0 1 2 3
    // chunk is composed in sequence:
    // SeqPartA: 0
    // SeqPartB: 1
    // SeqPartC: 2
    // SeqPartD: 3

    repeat(availableChunks) { chunkIdx ->
      val chunk = Chunk(chunkIdx, ByteArray(blockSize * blocksPerChunk))
      chunks.add(chunk)
      val chunkWriter = SequentialArrayWriter(chunk.bytes)
      var blockIdx = 0
      // for each SeqPartX
      repeat(4) {
        val block = SequentialArrayReader(blocks[chunkIdx * blocksPerChunk + blockIdx].bytes)
        blockIdx += 1

        repeat(blockHeight) {
          repeat(blockWidth) {
            chunkWriter.write(block.read())
          }
        }
      }
    }
  }

  private fun processLinearChunk(availableChunks: Int, blocksPerChunk: Int) {
    // given sequential blocks: 0 1 2 3 4 5 6 7 ...
    // chunk is composed in sequence:
    // SeqPartA: 0 1 2 3 4 5 6 7 ... (depends on how many chunks can fit in image width)

    repeat(availableChunks) { chunkIdx ->
      val chunk = Chunk(chunkIdx, ByteArray(blockSize * blocksPerChunk))
      chunks.add(chunk)
      val chunkWriter = SequentialArrayWriter(chunk.bytes)

      var blockIdx = 0
      // for each SeqPartX
      val startBlockIndex = chunkIdx * blocksPerChunk + blockIdx
      val endBlockIndex = startBlockIndex + blocksPerChunk
      val chunkBlocks = blocks.slice(startBlockIndex until endBlockIndex).map { SequentialArrayReader(it.bytes) }
      blockIdx += blocksPerChunk

      repeat(blockHeight) {
        repeat(blocksPerChunk) { idx ->
          repeat(blockWidth) {
            chunkWriter.write(chunkBlocks[idx].read())
          }
        }
      }
    }
  }
}
