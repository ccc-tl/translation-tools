package tl.extra.cli

import kio.util.child
import tl.extra.extraCpkUnpack
import tl.extra.extraIsoUnpack
import tl.extra.extraJpCpkUnpack
import tl.extra.extraJpPakUnpack
import tl.extra.extraPakUnpack
import tl.extra.extraUnpack
import tl.extra.fateOutput
import tl.extra.util.PackageExtractor
import tl.file.CpkFile
import java.io.File

fun main() {
  ExtraSetupStep1(
    inputDir = fateOutput,
    extractIsoDir = extraIsoUnpack,
    extractCpkDir = extraCpkUnpack,
    extractPakDir = extraPakUnpack,
    extractDir = extraUnpack,
    extractJpCpkDir = extraJpCpkUnpack,
    extractJpPakDir = extraJpPakUnpack
  )
    .execute()
  println("Done")
}

class ExtraSetupStep1(
  private val inputDir: File,
  private val extractIsoDir: File,
  private val extractCpkDir: File,
  private val extractPakDir: File,
  private val extractDir: File,
  private val extractJpCpkDir: File,
  private val extractJpPakDir: File,
) {
  fun execute() {
    val cpkIsoPath = "PSP_GAME/USRDIR/data.cpk"
    val cpkFile = inputDir.child("us/$cpkIsoPath")
    if (!cpkFile.exists()) {
      error("US CPK file does not exist")
    }
    val cpkJpFile = inputDir.child("jp/$cpkIsoPath")
    if (!cpkJpFile.exists()) {
      error("JP CPK file does not exist")
    }
    listOf(extractIsoDir, extractCpkDir, extractPakDir, extractDir, extractJpCpkDir, extractJpPakDir)
      .forEach {
        it.mkdirs()
        it.listFiles()?.size?.let { size ->
          if (size > 0) {
            error("Output directory is not empty: $it")
          }
        }
      }

    println("Copying US ISO files...")
    inputDir.child("us").copyRecursively(extractIsoDir)
    println("Extracting CPK...")
    CpkFile(cpkFile).extractTo(extractCpkDir)
    println("Extracting PAK...")
    PackageExtractor(extractCpkDir, extractPakDir)
    println("Creating combined extract, this will take a while...")
    extractCpkDir.copyRecursively(extractDir)
    extractPakDir.copyRecursively(extractDir, overwrite = true)

    println("Extracting JP CPK...")
    CpkFile(cpkJpFile).extractTo(extractJpCpkDir)
    println("Extracting JP PAK...")
    PackageExtractor(extractJpCpkDir, extractJpPakDir)
  }
}
