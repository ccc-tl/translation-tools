package tl.extra.ccc.patcher

import kio.LERandomAccessFile
import kio.util.child
import kio.util.seek
import tl.extra.ccc.file.Mob2DFile
import tl.extra.file.PakFile
import java.io.File

class DungeonPatcher(
  private val srcMob: File,
  private val srcMsgDat: File,
) {
  private val scaleYDelta = 0.11f
  private val mobUpdates = mapOf(
    4 to listOf(
      PakMobEntryUpdates(4, scaleYDelta),
    ),
    5 to listOf(
      PakMobEntryUpdates(4, scaleYDelta),
    ),
    6 to listOf(
      PakMobEntryUpdates(4, scaleYDelta),
    )
  )

  fun patchTo(outDir: File) {
    patchFile(srcMob, outDir) { patchMob(it) }
    patchFile(srcMsgDat, outDir) { patchMessageDat(it) }
  }

  private fun patchFile(srcFile: File, outDir: File, patch: (File) -> Unit) {
    val outFile = outDir.child(srcFile.name)
    srcFile.copyTo(outFile, overwrite = true)
    patch(outFile)
  }

  private fun patchMob(outMob: File) {
    val pak = PakFile(outMob)
    val raf = LERandomAccessFile(outMob)

    pak.entries.forEach nextPakEntry@{ pakEntry ->
      val entryUpdates = mobUpdates[pakEntry.index]
        ?: return@nextPakEntry
      val mob = Mob2DFile(pakEntry.bytes)

      entryUpdates.forEach { entryUpdate ->
        val mobEntry = mob.entries[entryUpdate.entryIndex]
        mobEntry.frames.forEach { frame ->
          val frameOffset = pak.offsets[pakEntry.index] + frame.filePos
          raf.seek(frameOffset + 0x10)
          raf.writeFloat(frame.scaleY + entryUpdate.addToScaleY)
        }
      }
    }

    raf.close()
  }

  private fun patchMessageDat(outMsgDat: File) {
    LERandomAccessFile(outMsgDat).use {
      // patch left texture and message background Y pos
      it.seek(0x3C + 0xC)
      it.writeInt(86 + -1)
      it.seek(0x1E4 + 0xC)
      it.writeInt(85 + -3)
    }
  }

  private data class PakMobEntryUpdates(
    val entryIndex: Int,
    val addToScaleY: Float,
  )
}
