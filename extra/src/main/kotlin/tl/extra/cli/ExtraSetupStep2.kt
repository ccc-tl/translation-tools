package tl.extra.cli

import kio.util.child
import kmipsx.elf.ElfFile
import tl.extra.extraJpPakUnpack
import tl.extra.extraToolkit
import tl.extra.extraUnpack
import tl.extra.fateOutput
import java.io.File

fun main() {
  ExtraSetupStep2(fateOutput, extraToolkit, extraUnpack, extraJpPakUnpack)
    .execute()
  println("Done")
}

class ExtraSetupStep2(
  private val inputDir: File,
  toolkitDir: File,
  private val extractDir: File,
  private val extractJpPakDir: File,
) {
  private val srcDir = toolkitDir.child("src").also { it.mkdirs() }
  private val toolsDir = toolkitDir.child("tools").also { it.mkdirs() }

  private val translationDir = srcDir.child("translation").also { it.mkdirs() }
  private val translationTemplateDir = inputDir.child("toolkit-template/extra/translation")

  fun execute() {
    copyFiles()
    prepareTranslations()
  }

  private fun copyFiles() {
    println("Copying files...")

    listOf("custom-cpk-files", "custom-pak-files", "iso-base", "stock")
      .forEach { srcDir.child(it).mkdir() }

    inputDir.child("fine.exe").copyTo(toolsDir.child("fine.exe"))
    inputDir.child("ULUS10576.BIN")
      .also { ElfFile(it) }
      .copyTo(srcDir.child("ULUS10576.BIN"))
    listOf(
      "UMD_DATA.BIN",
      "PSP_GAME/ICON0.PNG",
      "PSP_GAME/PIC0.PNG",
      "PSP_GAME/PIC1.PNG",
      "PSP_GAME/PARAM.SFO",
      "PSP_GAME/SYSDIR/OPNSSMP.BIN",
      "PSP_GAME/SYSDIR/UPDATE/DATA.BIN",
      "PSP_GAME/SYSDIR/UPDATE/EBOOT.BIN",
      "PSP_GAME/SYSDIR/UPDATE/PARAM.SFO",
      "PSP_GAME/USRDIR/saveutil/icon0.png",
      "PSP_GAME/USRDIR/saveutil/icon1.png",
      "PSP_GAME/USRDIR/saveutil/icon2.png",
      "PSP_GAME/USRDIR/saveutil/pic1.png",
    ).forEach { path ->
      inputDir.child("us").child(path).copyTo(srcDir.child("iso-base").child(path).also { it.parentFile.mkdirs() })
    }
    srcDir.child("iso-base/PSP_GAME/USRDIR/debug").mkdir()
    listOf(
      Pair("PSP_GAME/PARAM.SFO", "PARAM.SFO"),
      Pair("PSP_GAME/SYSDIR/OPNSSMP.BIN", "OPNSSMP.BIN"),
      Pair("PSP_GAME/SYSDIR/EBOOT.BIN", "ULUS10576.BIN"),
    ).forEach { (path, outName) ->
      inputDir.child("us").child(path).copyTo(srcDir.child("stock").child(outName))
    }
    extractDir.child("movie/FateExtraOP.pmf").copyTo(srcDir.child("custom-cpk-files/movie/FateExtraOP.pmf"))
    extractDir.child("pak-files.json").copyTo(srcDir.child("pak-files.json"))
    extractJpPakDir.child("paramData/chaDataTbl.sjis").copyTo(srcDir.child("chaDataTbl-jp.sjis"))
  }

  private fun prepareTranslations() {
    println("Preparing translations...")
    ExtraMainScriptDumper(srcDir.child("translation/dat"), extractDir, extractJpPakDir, applyEntryFixes = true)
    ExtraMiscScriptDumper(srcDir.child("translation/misc"), extractDir, extractJpPakDir)
    listOf("dat", "misc").forEach { unit ->
      srcDir.child("translation/$unit/script-translation.txt").copyTo(srcDir.child("translation/$unit/script-translation-orig.txt"))
    }
    translationTemplateDir.copyRecursively(translationDir, overwrite = true)
  }
}
