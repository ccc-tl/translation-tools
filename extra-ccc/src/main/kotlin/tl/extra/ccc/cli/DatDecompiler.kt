package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccUnpack
import tl.extra.ccc.file.dat.DatFile
import tl.extra.ccc.file.dat.FindAllFunctionsMode

fun main() {
  decompileCccDat("field_new/001/0000.dat", arrayOf())
  println("Done")
}

private fun decompileCccDat(
  path: String,
  manualFunctionPtrs: Array<Int> = arrayOf(),
  parseEventFunctions: Boolean = true,
  aggressive: Boolean = false,
) {
  DatFile(
    cccUnpack.child(path), outName = path.replace("/", "_"),
    cccMode = true, manualFunctionsToParse = manualFunctionPtrs, parseEventFunctions = parseEventFunctions,
    aggressive = aggressive,
    findAllFunctionsMode = if (aggressive) FindAllFunctionsMode.AGGRESSIVE else FindAllFunctionsMode.NONE
  ).writeHtml()
}
