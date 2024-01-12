package tl.extra.ccc.file

import kio.KioInputStream
import kio.util.swapBytes
import kio.util.toHex
import kio.util.toWHex

/**
 * Initial parser for 2D .MOB files. 2D MOB has multiple file entries (same as PAK file type).
 * This parser loads up single individual entry from MOB, use PakFile wrapper for direct loading from .MOB file
 * */
class Mob2DFile(bytes: ByteArray) {
  val entries = mutableListOf<Mob2DEntry>()

  init {
    val input = KioInputStream(bytes)

    with(input) {
      readInt() // is it always 02 00 66 00 for 2D mob files?
      val headerEntryCount = readInt()
      readInt()
      readInt()

      val framesCount = mutableListOf<Int>()

      repeat(headerEntryCount) {
        framesCount.add(readInt())
        readInt()
      }

      framesCount.forEachIndexed { index, count ->
        val frames = mutableListOf<Mob2DFrame>()
        repeat(count) { frameIndex ->
          // frame entry is 0x34 bytes
          frames.add(
            Mob2DFrame(
              filePos = pos(),
              time = readInt(),
              posX = readFloat(),
              posY = readFloat(),
              scaleX = readFloat(),
              scaleY = readFloat(),
              unkValue = readFloat(),
              rotZ = readShort(), // guessing here, rotation fields could be something else too...
              rotW = readShort(),
              rotX = readShort(),
              rotY = readShort(),
              entryId = readInt(),
              color0 = readInt(),
              color1 = readInt(),
              color2 = readInt(),
              renderMode = readInt(),
              index = frameIndex
            )
          )
        }

        entries.add(Mob2DEntry(index, frames))
      }

      close()
    }
  }

  fun dump() {
    entries.forEachIndexed { index, entry ->
      println("Entry $index, ${entry.frames.size} frames")
      entry.frames.forEach {
        dumpFrame(it)
      }
    }
  }

  private fun dumpFrame(frame: Mob2DFrame) {
    with(frame) {
      println(
        "filePos = ${filePos.toHex()}, time = $time, x = $posX, y = $posY, scaleX = $scaleX, scaleY = $scaleY, unkValue = $unkValue, " +
          "entryId = $entryId, color0 = ${color0.swapBytes().toWHex()}, color1 = ${color1.swapBytes().toWHex()}, " +
          "color2 = ${color2.swapBytes().toWHex()}, renderMode = ${renderMode.swapBytes().toWHex()}, " +
          "rotZ = $rotZ, rotW = $rotW, rotX = $rotX, rotY = $rotY, index = $index"
      )
    }
  }

  fun collectFrames(mobEntryId: Int): List<Mob2DFrame> {
    return entries.flatMap { entry ->
      entry.frames.filter { it.entryId == mobEntryId }
    }
  }
}

class Mob2DEntry(val index: Int, val frames: List<Mob2DFrame>)

class Mob2DFrame(
  val filePos: Int,
  val time: Int,
  val posX: Float,
  val posY: Float,
  val scaleX: Float,
  val scaleY: Float,
  val unkValue: Float,
  val rotZ: Short,
  val rotW: Short,
  val rotX: Short,
  val rotY: Short,
  val entryId: Int,
  val color0: Int,
  val color1: Int,
  val color2: Int,
  val renderMode: Int,
  val index: Int,
)
