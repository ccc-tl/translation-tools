package tl.extra.ccc.cli

import kio.util.relativizePath
import kio.util.walkDir
import tl.extra.ccc.cccUnpack
import tl.extra.ccc.file.dat.DatFile
import tl.extra.ccc.file.dat.FindAllFunctionsMode
import tl.extra.extraUnpack
import tl.extra.fateOutput
import java.io.File
import java.util.Locale

fun main() {
  DatDatabaseBuilder(extraUnpack, false, "db-fate", "db-fate.html")
  DatDatabaseBuilder(cccUnpack, true, "db-ccc", "db-ccc.html")
  println("Done")
}

class DatDatabaseBuilder(
  private val baseGameDir: File,
  private val cccMode: Boolean,
  private val outFolderName: String,
  private val indexFileName: String,
  private val aggressive: Boolean = false,
) {
  val index = StringBuilder()

  init {
    File(fateOutput, outFolderName).mkdirs()
    index.append("<table>")
    walkDir(baseGameDir, processFile = this::processDatFile)
    index.append("</table>")
    File(fateOutput, indexFileName).writeText(index.toString())
    println("Wrote database index")
  }

  private fun processDatFile(file: File) {
    if (file.extension != "dat") {
      return
    }
    println("Process ${file.path}")
    val relPath = file.relativizePath(baseGameDir)
    val outPath = "$outFolderName${File.separator}" + relPath.replace("/", "$")
    val pathWithoutExt = outPath.substring(0, outPath.lastIndexOf("."))
    val dasmPath = "$pathWithoutExt.dasm"
    val htmlPath = "$pathWithoutExt.html"
    var coverage = -1f
    try {
      val datFile = DatFile(
        file,
        cccMode = cccMode,
        outName = pathWithoutExt,
        parseEventFunctions = relPath.contains("field_new"),
        findAllFunctionsMode = if (aggressive) FindAllFunctionsMode.AGGRESSIVE else FindAllFunctionsMode.NONE,
        aggressive = aggressive,
      )
      coverage = datFile.calculateCoverage()
      datFile.writeHtml()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    index.append(
      "<tr><td>$relPath</td> <td><a href=$dasmPath>Disassembly</a></td> <td><a href=$htmlPath>Coverage report</a></td> " +
        "<td>Coverage: ${if (coverage < 0) "parse error" else "%.03f".format(Locale.US, coverage) + "%"}</td><tr>\n",
    )
    println()
  }
}
