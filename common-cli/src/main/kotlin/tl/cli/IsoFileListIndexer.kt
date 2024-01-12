package tl.cli

import java.io.File

fun main(args: Array<String>) {
  if (args.size != 2) {
    println("Usage: [inputFile] [outputFile]")
    return
  }
  val inputFile = File(args[0])
  val outputFile = File(args[1])
  IsoFileListIndexer(inputFile, outputFile)
  println("Done")
}

class IsoFileListIndexer(inputFile: File, outputFile: File) {
  init {
    if (!inputFile.exists()) {
      error("Input file does not exist")
    }
    val out = StringBuilder()
    val lines = inputFile.readLines()
    lines.forEachIndexed { idx, fileLine ->
      out.append("$fileLine ${lines.size - idx}\r\n")
    }
    outputFile.writeText(out.toString())
  }
}
