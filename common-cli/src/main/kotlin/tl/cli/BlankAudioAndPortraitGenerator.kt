package tl.cli

import tl.util.Translation
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  if (args.size != 1) {
    println("Usage: [generateForDir]")
    exitProcess(1)
  }
  val generateFor = File(args[0])
  val translation = Translation(generateFor)
  val audioFile = generateFor.resolveSibling("script-audio.txt")
  val portraitFile = generateFor.resolveSibling("script-portrait.txt")
  arrayOf(audioFile, portraitFile).forEach { file ->
    if (file.exists()) {
      error("${file.name} already exists!")
    }
  }
  val blankOut = StringBuilder()
  repeat(translation.jpTexts.size) {
    blankOut.append("{end}\n\n")
  }
  blankOut.toString().let {
    audioFile.writeText(it)
    portraitFile.writeText(it)
  }
  println("Done")
}
