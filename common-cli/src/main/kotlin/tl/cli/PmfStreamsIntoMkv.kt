package tl.cli

import kio.util.child
import kio.util.execute
import java.io.File

fun main(args: Array<String>) {
  if (args.size != 2) {
    println("Usage: [inputDir] [outputDir]")
    return
  }
  PmfStreamsIntoMkv(File(args[0]), File(args[1]))
  println("Done")
}

/**
 * Before using this extract PMF streams using VGMToolbox
 */
class PmfStreamsIntoMkv(inputDir: File, outputDir: File) {
  companion object {
    private const val EXT_H264 = "264"
  }

  init {
    val fileList = inputDir.listFiles() ?: emptyArray()
    outputDir.mkdirs()

    fileList.filter { it.isFile && it.extension == EXT_H264 }
      .forEach { file ->
        val baseName = file.nameWithoutExtension.substringBeforeLast("_")
        println("Processing $baseName...")
        val audioFile = fileList.first { it.isFile && it.name.startsWith(baseName) && it.extension != EXT_H264 }
        execute(
          "ffmpeg",
          args = listOf("-i", file, "-i", audioFile, "-c:v", "copy", "-c:a", "flac", outputDir.child("$baseName.mkv")),
          workingDirectory = outputDir
        )
      }
  }
}
