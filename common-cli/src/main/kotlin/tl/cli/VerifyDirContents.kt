package tl.cli

import com.google.common.io.Files
import kio.util.relativizePath
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  if (args.size != 2) {
    println("Usage: [dir1] [dir2]")
    return
  }
  val dir1 = File(args[0])
  val dir2 = File(args[1])
  VerifyDirContents(dir1, dir2)
  println("Done")
}

/**
 * Verify if two directories content is the same
 * IMPORTANT: This will ignore any extra files that only exist in in2
 */
@Suppress("UnstableApiUsage")
class VerifyDirContents(dir1: File, dir2: File, exitOnError: Boolean = false, verbose: Boolean = true) {
  init {
    if (!dir1.exists()) {
      error("dir1 does not exist")
    }
    if (!dir2.exists()) {
      error("dir2 does not exist")
    }
    println("Checking files, this may take a while...")
    for (sourceFile in Files.fileTraverser().depthFirstPreOrder(dir1)) {
      if (sourceFile.isDirectory) {
        continue
      }
      val relativePath = sourceFile.relativizePath(dir1)
      val compareFile = File(dir2, relativePath)
      if (!compareFile.exists() ||
        sourceFile.length() != compareFile.length() ||
        !sourceFile.readBytes().contentEquals(compareFile.readBytes())
      ) {
        println("Check $relativePath... content mismatch!")
        if (exitOnError) {
          exitProcess(1)
        }
      } else if (verbose) {
        println("Check $relativePath... ok")
      }
    }
  }
}
