package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccCpkUnpack
import tl.extra.ccc.cccDnsUnpack
import tl.extra.ccc.cccPakUnpack
import tl.extra.ccc.cccUnpack
import tl.extra.fateOutput
import tl.extra.util.PackageExtractor
import tl.file.CpkFile
import java.io.File

fun main() {
  CccSetupStep1(fateOutput, cccDnsUnpack, cccCpkUnpack, cccPakUnpack, cccUnpack)
    .execute()
  println("Done")
}

class CccSetupStep1(
  private val inputDir: File,
  private val extractDnsDir: File,
  private val extractCpkDir: File,
  private val extractPakDir: File,
  private val extractDir: File,
) {
  fun execute() {
    val cpkFile = inputDir.child("GAME.cpk")
    if (!cpkFile.exists()) {
      error("CPK file does not exist")
    }
    listOf(extractDnsDir, extractCpkDir, extractPakDir, extractDir)
      .forEach {
        it.mkdirs()
        it.listFiles()?.size?.let { size ->
          if (size > 0) {
            error("Output directory is not empty: $it")
          }
        }
      }

    println("Copying CPK...")
    cpkFile.copyTo(extractDnsDir.child("GAME.cpk"))
    println("Extracting CPK...")
    CpkFile(cpkFile).extractTo(extractCpkDir)
    println("Extracting PAK...")
    PackageExtractor(extractCpkDir, extractPakDir)
    println("Creating combined extract, this will take a while...")
    extractCpkDir.copyRecursively(extractDir)
    extractPakDir.copyRecursively(extractDir, overwrite = true)
  }
}
