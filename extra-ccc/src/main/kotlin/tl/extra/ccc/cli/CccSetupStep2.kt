package tl.extra.ccc.cli

import kio.KioInputStream
import kio.util.child
import kio.util.readJson
import kmipsx.elf.ElfFile
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.cccUnpack
import tl.extra.fateOutput
import tl.util.readDatString
import java.io.File

fun main() {
  CccSetupStep2(fateOutput, cccToolkit, cccUnpack)
    .execute()
  println("Done")
}

class CccSetupStep2(
  private val inputDir: File,
  private val toolkitDir: File,
  private val extractDir: File,
) {
  private val srcDir = toolkitDir.child("src").also { it.mkdirs() }
  private val toolsDir = toolkitDir.child("tools").also { it.mkdirs() }

  private val translationDir = srcDir.child("translation").also { it.mkdirs() }
  private val translationTemplateDir = inputDir.child("toolkit-template/ccc/translation")

  fun execute() {
    copyFiles()
    prepareTranslations()
  }

  private fun copyFiles() {
    println("Copying files...")

    listOf("custom-iso-files", "custom-cpk-files", "custom-pak-files", "iso-base", "stock")
      .forEach { srcDir.child(it).mkdir() }

    inputDir.child("fine.exe").copyTo(toolsDir.child("fine.exe"))
    inputDir.child("NPJH50505.BIN")
      .also { ElfFile(it) }
      .copyTo(srcDir.child("NPJH50505.BIN"))
    listOf(
      "UMD_DATA.BIN",
      "PSP_GAME/ICON0.PNG",
      "PSP_GAME/PIC1.PNG",
      "PSP_GAME/INSDIR/ICON0.PNG",
      "PSP_GAME/INSDIR/PIC1.PNG",
      "PSP_GAME/SYSDIR/BOOT.BIN",
      "PSP_GAME/SYSDIR/OPNSSMP.BIN",
      "PSP_GAME/SYSDIR/UPDATE/DATA.BIN",
      "PSP_GAME/SYSDIR/UPDATE/EBOOT.BIN",
      "PSP_GAME/SYSDIR/UPDATE/PARAM.SFO",
    ).forEach { path ->
      inputDir.child(path).copyTo(srcDir.child("iso-base").child(path).also { it.parentFile.mkdirs() })
    }
    listOf(
      Triple("PSP_GAME/PARAM.SFO", "PARAM.SFO", false),
      Triple("PSP_GAME/SYSDIR/OPNSSMP.BIN", "OPNSSMP.BIN", false),
      Triple("PSP_GAME/SYSDIR/EBOOT.BIN", "NPJH50505.BIN", false),
      Triple("PSP_GAME/USRDIR/MOVIE/DEMO01.PMF", "DEMO01.PMF", true),
      Triple("PSP_GAME/USRDIR/MOVIE/DEMO02.PMF", "DEMO02.PMF", true),
    ).forEach { (path, outName, copyToCustomFiles) ->
      inputDir.child(path).copyTo(srcDir.child("stock").child(outName))
      if (copyToCustomFiles) {
        inputDir.child(path).copyTo(srcDir.child("custom-iso-files").child(path).also { it.parentFile.mkdirs() })
      }
    }
    extractDir.child("pak-files.json").copyTo(srcDir.child("pak-files.json"))
  }

  private fun prepareTranslations() {
    println("Preparing translations...")
    DatScriptJsonDumper(extractDir, translationDir.child("dat/json").also { it.mkdirs() })
      .dump()
    dumpDatScript()
    CccScriptDumper(
      extractDir,
      srcDir.child("NPJH50505.BIN"),
      translationTemplateDir.child("eboot/pointers.json"),
      translationDir,
    )
    translationTemplateDir.copyRecursively(translationDir, overwrite = true)
  }

  private fun dumpDatScript() {
    val datJpOut = StringBuilder()
    val datBlankOut = StringBuilder()
    translationTemplateDir.child("dat/pointers.json")
      .readJson<List<DatPointerEntry>>()
      .forEachIndexed { index, entry ->
        if (entry.f == null || entry.p == null) {
          datJpOut.appendLine("<unused entry>{end}\n")
          datBlankOut.appendLine("{end}\n")
        } else {
          with(KioInputStream(extractDir.child("field_new/${entry.f}/0000.dat"))) {
            setPos(entry.p)
            setPos(readInt())
            val jp = readDatString()
              .replace("\n", "\\n\n")
              .trim(' ', '　')
              .trimEnd('\n')
            if (index == 29489) {
              datJpOut.append("\n")
            }
            datJpOut.appendLine("${jp}\n{end}\n")
            datBlankOut.appendLine("{end}\n")
          }
        }
        if (index % 1000 == 0) {
          println("Processing DAT entries $index...")
        }
      }
    translationDir.child("dat/script-japanese.txt").writeText(datJpOut.toString())
    translationDir.child("dat/script-translation.txt").writeText(datBlankOut.toString())
    translationDir.child("dat/script-notes.txt").writeText(datBlankOut.toString())
  }

  private data class DatPointerEntry(val f: String?, val p: Int?)
}
