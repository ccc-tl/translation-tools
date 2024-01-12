package tl.extra.ccc.patcher

import kio.LERandomAccessFile
import kio.util.child
import kio.util.seek
import tl.extra.ccc.file.Mob2DFile
import tl.extra.file.PakFile
import java.io.File

class DialogPatcher(private val srcMob: File) {
  fun patchTo(outDir: File) {
    patchFile(srcMob, outDir) { patchBackgroundScale(it) }
  }

  private fun patchFile(srcFile: File, outDir: File, patch: (File) -> Unit) {
    val outFile = outDir.child(srcFile.name)
    srcFile.copyTo(outFile, overwrite = true)
    patch(outFile)
  }

  private val updates = mapOf(
    0 to listOf(
      // 32 * (7.5625 + 1.375) = 286
      PakMobEntryUpdates(0, listOf(0f, 0.157f)),
      PakMobEntryUpdates(10, listOf(1.375f, 1.375f))
      // 32 * (7.5625 + 1.1875) = 280
      // PakMobEntryUpdates(0, listOf(0f, 0.135f)),
      // PakMobEntryUpdates(10, listOf(1.1875f, 1.1875f))
    )
  )

  private fun patchBackgroundScale(outMob: File) {
    val pak = PakFile(outMob)
    val raf = LERandomAccessFile(outMob)

    pak.entries.forEach nextPakEntry@{ pakEntry ->
      val mob = Mob2DFile(pakEntry.bytes)
      val entryUpdates = updates[pakEntry.index]
        ?: return@nextPakEntry

      entryUpdates.forEach { entryUpdate ->
        val mobEntry = mob.entries[entryUpdate.entryId]
        mobEntry.frames.forEach nextFrame@{ frame ->
          if (frame.scaleX == 0f) {
            return@nextFrame
          }
          val frameOffset = pak.offsets[pakEntry.index] + frame.filePos
          raf.seek(frameOffset + 0xC)
          raf.writeFloat(frame.scaleX + entryUpdate.addToFrameScaleX[frame.index])
        }
      }
    }

    raf.close()
  }

  private data class PakMobEntryUpdates(
    val entryId: Int,
    val addToFrameScaleX: List<Float>,
  )
}
