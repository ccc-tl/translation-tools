package tl.tex

import kio.KioInputStream
import kio.util.child
import kio.util.execute
import kio.util.relativizePath
import kio.util.walkDir
import java.io.File
import java.nio.file.Files

class GimTextureConverter(
  private val gimConvTool: File,
  private val srcDir: File,
  private val fileFilter: (File) -> Boolean = { isFileGimTexture(it) },
) {
  fun convertTo(outDir: File) {
    outDir.mkdir()
    if (outDir.listFiles()!!.isNotEmpty()) {
      error("Output directory is not empty")
    }
    outDir.mkdirs()
    var convertedCount = 0
    walkDir(srcDir) {
      if (fileFilter(it)) {
        if (it.extension == "gim") {
          execute(
            gimConvTool,
            args = listOf(
              it,
              "-o",
              outDir.child("${it.relativizePath(srcDir).replace("/", "$")}.png"),
            ),
          )
          convertedCount++
        } else {
          val gimFile = it.resolveSibling("${it.nameWithoutExtension}.gim")
          Files.move(it.toPath(), gimFile.toPath())
          execute(
            gimConvTool,
            args = listOf(
              gimFile,
              "-o",
              outDir.child("${it.relativizePath(srcDir).replace("/", "$")}.png"),
            ),
          )
          convertedCount++
          Files.move(gimFile.toPath(), it.toPath())
        }
      }
    }
    println("Should have created $convertedCount files")
  }
}

fun isFileGimTexture(file: File): Boolean {
  if (file.length() <= 11) {
    return false
  }
  with(KioInputStream(file)) {
    val magic = readString(11)
    close()
    return magic == "MIG.00.1PSP"
  }
}
