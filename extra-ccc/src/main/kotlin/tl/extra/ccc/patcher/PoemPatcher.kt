package tl.extra.ccc.patcher

import kio.KioInputStream
import kio.KioOutputStream
import kio.LERandomAccessFile
import kio.util.child
import kio.util.seek
import kio.util.temporaryJump
import kio.util.toWHex
import tl.extra.ccc.file.Mob2DFile
import tl.extra.file.PakFile
import tl.extra.file.replaceEntry
import tl.extra.patcher.PakWriter
import java.io.ByteArrayOutputStream
import java.io.File

class PoemPatcher(private val srcDat: File, private val srcMob: File, private val updates: List<PoemUpdate>) {
  fun patchTo(outDir: File) {
    val outDat = outDir.child(srcDat.name)
    val outMob = outDir.child(srcMob.name)
    srcDat.copyTo(outDat, overwrite = true)
    srcMob.copyTo(outMob, overwrite = true)
    patchTextXY(outDat)
    // warning: changing update order will break existing patches
    patchMobLights(outMob)
    patchMobSwaps(outMob)
    patchMobWrites(outMob)
  }

  private fun patchTextXY(outDat: File) {
    with(LERandomAccessFile(outDat)) {
      val structMappings = buildStructMapping(this)
      updates.mapNotNull { it as? PoemUpdate.TextX }
        .forEach {
          updateStructXPos(this, structMappings.getValue(it.structId), it.structId, it.delta)
          if (it.subStructId != -1) {
            updateStructXPos(this, structMappings.getValue(it.subStructId), it.subStructId, it.subDelta)
          }
        }
      updates.mapNotNull { it as? PoemUpdate.TextY }
        .forEach {
          updateStructYPos(this, structMappings.getValue(it.structId), it.structId, it.delta)
          if (it.subStructId != -1) {
            updateStructYPos(this, structMappings.getValue(it.subStructId), it.subStructId, it.subDelta)
          }
        }
      close()
    }
  }

  private fun buildStructMapping(raf: LERandomAccessFile): Map<Int, Int> {
    val mappings = mutableMapOf<Int, Int>()
    raf.temporaryJump(0) {
      while (it.filePointer < it.length()) {
        val pos = it.filePointer.toInt()
        val opcode = it.readInt()
        if (opcode == 0x00000A20) {
          val structId = it.readInt()
          val textureId = it.readInt()
          it.readInt()
          val texRenderMode2 = it.readInt()
          if (textureId == -1 && texRenderMode2 == 6) {
            mappings[structId] = pos
          } else {
            it.seek(pos + 4)
          }
        }
      }
    }
    return mappings
  }

  private fun updateStructXPos(raf: LERandomAccessFile, pos: Int, structId: Int, delta: Int) {
    raf.temporaryJump(pos + 0x14) {
      if (it.readInt() != 0x00000021) {
        error("Expected struct to have position set at offset ${raf.filePointer.toWHex()}")
      }
      if (it.readInt() != structId) {
        error("Expected struct id to be $structId")
      }
      if (it.readInt() != 0x0) {
        error("Expected XYZ selector to be 0 (X)")
      }
      val newPos = it.readInt() + delta
      it.seek(it.filePointer - 0x4)
      it.writeInt(newPos)
    }
  }

  private fun updateStructYPos(raf: LERandomAccessFile, pos: Int, structId: Int, delta: Int) {
    raf.temporaryJump(pos + 0x24) {
      if (it.readInt() != 0x00000021) {
        error("Expected struct to have position set at offset ${raf.filePointer.toWHex()}")
      }
      if (it.readInt() != structId) {
        error("Expected struct id to be $structId")
      }
      if (it.readInt() != 0x1) {
        error("Expected XYZ selector to be 1 (Y)")
      }
      val newPos = it.readInt() + delta
      it.seek(it.filePointer - 0x4)
      it.writeInt(newPos)
    }
  }

  private fun patchMobLights(outMob: File) {
    val pak = PakFile(outMob)
    with(LERandomAccessFile(outMob)) {
      updates.mapNotNull { it as? PoemUpdate.Light }
        .forEach { update ->
          val mobFrames = Mob2DFile(pak.entries[update.pakEntryId].bytes)
            .collectFrames(update.lightId)
          if (mobFrames.size < 4) {
            error("Not enough mob frames to update light automatically")
          }
          val xPos = listOf(update.xFrom, update.xFrom - 10, update.xTo - 10, update.xTo)
          repeat(4) { frameIdx ->
            val offset = pak.offsets[update.pakEntryId]
            val frame = mobFrames[frameIdx + update.frameIdxOffset]
            val newX = xPos[frameIdx]
            seek(offset + frame.filePos + 4)
            writeFloat(newX)
            val newY = readFloat() + update.yDelta
            seek(filePointer - 4)
            writeFloat(newY)
          }
        }
      close()
    }
  }

  private fun patchMobSwaps(outMob: File) {
    updates.mapNotNull { it as? PoemUpdate.SwapBytes }
      .forEach { update ->
        val entries = PakFile(outMob).entries
        val a = splitHeadTail(entries[update.pakEntryIdA].bytes, update.entryOffsetA, update.sizeA)
        val b = splitHeadTail(entries[update.pakEntryIdB].bytes, update.entryOffsetB, update.sizeB)
        val newA = mergeHeadTail(a.first, b.second, a.third)
        val newB = mergeHeadTail(b.first, a.second, b.third)
        entries.replaceEntry(update.pakEntryIdA, "", newA)
        entries.replaceEntry(update.pakEntryIdB, "", newB)
        PakWriter(entries.toList(), false).writeTo(outMob)
      }
  }

  private fun splitHeadTail(bytes: ByteArray, offset: Int, size: Int): Triple<ByteArray, ByteArray, ByteArray> {
    val input = KioInputStream(bytes)
    val head = input.readBytes(offset)
    val middle = input.readBytes(size)
    val tail = input.readBytes(bytes.size - offset - size)
    return Triple(head, middle, tail)
  }

  private fun mergeHeadTail(head: ByteArray, middle: ByteArray, tail: ByteArray): ByteArray {
    val output = KioOutputStream(ByteArrayOutputStream())
    output.writeBytes(head)
    output.writeBytes(middle)
    output.writeBytes(tail)
    return output.getAsByteArrayOutputStream().toByteArray()
  }

  private fun patchMobWrites(outMob: File) {
    val pak = PakFile(outMob)
    with(LERandomAccessFile(outMob)) {
      updates.mapNotNull { it as? PoemUpdate.WriteMobInt }
        .forEach { update ->
          val pakOffset = pak.offsets[update.pakEntryId]
          seek(pakOffset + update.entryOffset)
          writeInt(update.newValue)
        }
      close()
    }
  }
}

sealed class PoemUpdate {
  class TextX(
    val structId: Int,
    val subStructId: Int,
    val delta: Int,
    val subDelta: Int = delta,
  ) : PoemUpdate()

  class TextY(
    val structId: Int,
    val subStructId: Int,
    val delta: Int,
    val subDelta: Int = delta,
  ) : PoemUpdate()

  class Light(
    val pakEntryId: Int,
    val lightId: Int,
    val xFrom: Float,
    val xTo: Float,
    val yDelta: Float,
    val frameIdxOffset: Int = 0,
  ) : PoemUpdate()

  class SwapBytes(
    val pakEntryIdA: Int,
    val entryOffsetA: Int,
    val sizeA: Int,
    val pakEntryIdB: Int,
    val entryOffsetB: Int,
    val sizeB: Int,
  ) : PoemUpdate()

  class WriteMobInt(
    val pakEntryId: Int,
    val entryOffset: Int,
    val newValue: Int,
  ) : PoemUpdate()
}
