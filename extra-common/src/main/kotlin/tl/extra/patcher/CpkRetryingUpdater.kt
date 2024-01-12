package tl.extra.patcher

import tl.file.CpkFile
import tl.file.CpkPatchedFile
import java.io.File
import java.io.IOException

fun tryPatchCpkInPlace(
  srcCpkFile: File,
  outCpkFile: File,
  cpkReplacements: Map<String, File>,
  rewriteCmpToPak: Boolean,
): List<CpkPatchedFile> {
  println("Copying stock CPK...")
  while (true) {
    try {
      srcCpkFile.copyTo(outCpkFile, overwrite = true)
      return CpkFile(outCpkFile).patchInPlace(
        patchedFiles = cpkReplacements,
        insertNewFilesAt = srcCpkFile.length(),
        allowBlockReplace = true,
        rewriteFileName = {
          if (rewriteCmpToPak && it.endsWith(".cmp")) {
            it.replaceAfterLast(".", "pak")
          } else {
            null
          }
        }
      )
    } catch (e: IOException) {
      val msg = e.message ?: throw e
      if (msg.contains("it is being used by another process") ||
        msg.contains("Tried to overwrite the destination, but failed to delete it.")
      ) {
        println("CPK file is locked, retry in 1 sec...")
        Thread.sleep(1000)
      } else {
        throw e
      }
    }
  }
}
