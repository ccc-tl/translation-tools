package tl.util

import kio.util.toWHex

class SpaceAllocationTracker(private val dataAlign: Int = 1) {
  private val blocks = mutableMapOf<Long, TrackingBlock>()

  fun addOffsetUse(offset: Long, size: Int) {
    blocks.getOrPut(offset) { TrackingBlock(offset, alignValue(size, dataAlign)) }.addUse()
  }

  fun removeOffsetUse(offset: Long) {
    val block = blocks[offset] ?: error("Missing block for offset ${offset.toWHex()}")
    block.removeUse()
  }

  fun compactFree(): SpaceAllocator {
    val freeBlocks = blocks.values
      .filter { it.uses == 0 }
      .sortedBy { it.offset }
    val compactedBlocks: MutableList<Block> = mutableListOf(freeBlocks.first())
    freeBlocks.drop(1)
      .forEach {
        if (it.offset == compactedBlocks.last().endOffset) {
          val lastBlock = compactedBlocks.removeAt(compactedBlocks.lastIndex)
          compactedBlocks.add(Block(lastBlock.offset, lastBlock.size + it.size))
        } else {
          compactedBlocks.add(it)
        }
      }
    return SpaceAllocator(dataAlign, compactedBlocks)
  }
}

class SpaceAllocator(private val dataAlign: Int, freeBlocks: List<Block>) {
  private val blocks = freeBlocks.toMutableList()

  fun allocateBestBlockFor(size: Int): Block? {
    // find block that will result in the smallest amount of wasted space
    val alignedSize = alignValue(size, dataAlign)
    val matchingBlock = blocks
      .filter { alignedSize <= it.size }
      .minByOrNull { it.size - alignedSize }
    if (matchingBlock != null) {
      blocks.remove(matchingBlock)
      val remainingSize = matchingBlock.size - alignedSize
      if (remainingSize > dataAlign) {
        blocks.add(Block(matchingBlock.offset + alignedSize, remainingSize))
      }
    }
    return matchingBlock
  }
}

private class TrackingBlock(offset: Long, size: Int) : Block(offset, size) {
  var uses: Int = 0
    private set

  fun addUse() {
    uses++
  }

  fun removeUse() {
    uses--
    if (uses < 0) {
      error("Block can't have negative use count")
    }
  }
}

open class Block(val offset: Long, val size: Int) {
  val endOffset = offset + size
}
