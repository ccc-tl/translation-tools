package tl.cli

import com.google.common.hash.Funnels
import com.google.common.hash.Hashing
import com.google.common.io.ByteStreams
import kio.util.relativizePath
import kio.util.walkDir
import java.io.File

fun main(args: Array<String>) {
  if (args.size != 1) {
    println("Usage: [inDir]")
    return
  }
  val inDir = File(args[0])
  FindDuplicates(inDir)
  println("Done")
}

@Suppress("UnstableApiUsage")
class FindDuplicates(inDir: File) {
  init {
    if (!inDir.exists()) {
      error("inDir does not exist")
    }
    println("Checking files, this may take a while...")

    val hashes = mutableMapOf<String, MutableList<File>>()

    walkDir(inDir) { file ->
      if (file.isDirectory) {
        return@walkDir
      }
      val relPath = file.relativizePath(inDir)
      println("Processing $relPath...")
      val hasher = Hashing.sha256().newHasher()
      file.inputStream().buffered().use {
        ByteStreams.copy(it, Funnels.asOutputStream(hasher))
      }
      hashes.getOrPut(hasher.hash().toString()) { mutableListOf() }
        .add(file)
    }

    println("\nResults\n")
    hashes.asSequence()
      .map { it.value }
      .filter { it.size > 1 }
      .forEach {
        println("Duplicate:")
        it.forEach { file ->
          println(file.absolutePath)
        }
        println()
      }
  }
}
