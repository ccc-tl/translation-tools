package tl.extra.patcher.tex

import kio.util.child
import tl.extra.file.PakFile
import tl.extra.file.replaceEntry
import tl.extra.file.tex.TxbFile
import tl.extra.patcher.PakWriter
import java.io.File

abstract class Png2Tex(
  private val unpackDir: File,
  protected val png2TexDir: File,
  private val customPakFilesDir: File,
  private val extract: Boolean = false,
  private val verify: Boolean = true,
) {
  protected fun repackTexture(
    baseDirName: String,
    srcTexPath: String,
    modifiedEntries: List<TexConversion>,
    innerDirSuffix: String = "",
    dropEntries: List<Int> = emptyList(),
    copyToCustomPakFiles: Boolean = true,
  ) {
    val srcTex = File(unpackDir, srcTexPath)
    val dir = png2TexDir.child(baseDirName)
    if (extract) {
      val extractDir = dir.child("extract$innerDirSuffix")
      extractDir.mkdir()
      dir.child("modified$innerDirSuffix").mkdir()
      TxbFile(srcTex).writeToFolder(extractDir)
    }
    val outFile = dir.child(srcTex.name)
    val entries = PakFile(srcTex).entries
    modifiedEntries.forEach { conv ->
      entries.replaceEntry(
        conv.idx,
        entries[conv.idx].path,
        conv.type.convert(entries[conv.idx].bytes, dir.child("modified$innerDirSuffix/tex${conv.idx}.png"))
      )
    }
    val entriesList = entries.toMutableList()
    dropEntries.sortedDescending()
      .forEach { index ->
        entriesList.removeAt(index)
      }
    PakWriter(entriesList, true).writeTo(outFile)
    if (verify) {
      val verifyDir = dir.child("verify$innerDirSuffix")
      verifyDir.deleteRecursively()
      verifyDir.mkdir()
      TxbFile(outFile).writeToFolder(verifyDir)
    }
    if (copyToCustomPakFiles) {
      outFile.copyTo(customPakFilesDir.child(srcTexPath), overwrite = true)
    }
  }
}

class TexConversion(val idx: Int, val type: ConversionType)
