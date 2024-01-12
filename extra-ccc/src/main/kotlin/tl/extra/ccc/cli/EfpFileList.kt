package tl.extra.ccc.cli

import tl.extra.ccc.cccUnpack
import tl.extra.ccc.file.EfpFile

fun main() {
  cccUnpack.walk().filter { it.extension in listOf("efp", "efc") }.forEach { file ->
    println(file.name)
    EfpFile(file).entries.forEach {
      println("\t${it.path}")
    }
    println()
  }
  println("Done")
}
