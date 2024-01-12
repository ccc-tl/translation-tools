package tl.extra.ccc.cli

import kio.util.child
import kio.util.relativizePath
import kio.util.walkDir
import tl.extra.ccc.cccUnpack
import tl.extra.ccc.file.ImptFile
import tl.extra.ccc.file.PspImFile
import tl.extra.extraUnpack
import tl.extra.fateOutput
import tl.extra.file.tex.TxbFile
import java.io.File

private val errorMessages = mutableListOf<String>()
private const val textureCccUnpack = true

private var texCount = 0

private val outDir = fateOutput.child("textures-extract")
private val basePath = File(if (textureCccUnpack) cccUnpack.absolutePath else extraUnpack.absolutePath)

fun main() {
  outDir.mkdir()
  val errorHandler = { file: File, _: Exception ->
    if (file.length() != 0L) {
      val msg = "ERROR: Uncaught exception on ${file.path}"
      errorMessages.add(msg)
      println(msg)
    }
  }
  walkDir(basePath, errorHandler, ::processTxbFile)
  walkDir(basePath, errorHandler, ::processImFile)
  walkDir(basePath, errorHandler, ::processImptFile)

  println()
  if (errorMessages.size != 0) {
    println("--- Errors Summary ---")
    println(errorMessages.joinToString(separator = "\n"))
    println("Failed to unpack ${errorMessages.size} textures.")
  } else {
    println("All texture were unpacked successfully")
  }
  println("${texCount - errorMessages.size} files should have been written")
  println("Done")
}

private fun processTxbFile(file: File) {
  if (file.extension != "txb") {
    return
  }
  println("Process ${file.path}")
  val texFile = TxbFile(file)
  val outDir = outDir.child(file.relativizePath(basePath)).apply { mkdirs() }
  texFile.writeToFolder(outDir)
  texCount += texFile.textures.size
  println()
}

private fun processImFile(file: File) {
  if (file.extension != "Im") {
    return
  }
  println("Process ${file.path}")
  val imFile = PspImFile(file)
  val outDir = outDir.child(file.relativizePath(basePath)).apply { mkdirs() }
  imFile.textures.forEachIndexed { index, tex ->
    tex.writeToPng(outDir.child("im - $index.png"))
  }
  texCount += imFile.textures.size
  println()
}

private fun processImptFile(file: File) {
  if (file.extension != "Impt") {
    return
  }
  println("Process ${file.path}")
  val imptFile = ImptFile(file)
  val outDir = outDir.child(file.relativizePath(basePath)).apply { mkdirs() }
  imptFile.textures.forEachIndexed { index, tex ->
    tex.writeToPng(outDir.child("impt - $index.png"))
  }
  texCount += imptFile.textures.size
  println()
}
