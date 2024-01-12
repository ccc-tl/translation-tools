package tl.extra.ccc.file.dat

import kio.KioInputStream

interface DatParsingContext {
  fun getFileSize(): Int

  fun emitText(text: String = "")

  fun emit(text: String)

  fun emitDataBlock(addr: Int, text: String) {
  }

  fun emitWarning(text: String) {
  }

  fun addFunctionEntryPoint(entryPoint: Int) {
    addFunctionEntryPoint(FunctionEntryPoint(entryPoint))
  }

  fun addFunctionEntryPoint(entryPoint: FunctionEntryPoint) {
  }

  fun addJumpBranch(offset: Int) {
  }

  fun addCoverage(range: IntRange) {
  }

  fun addTextPointerLoc(ptr: Int) {
  }
}

interface DatOpcodeParser {
  fun parseOpcode(input: KioInputStream, opcodeGroup: Int, opcode: Int): DatParseState
}

class FunctionEntryPoint(val addr: Int, val prefix: String = "sub_", val printAddr: Boolean = true)

enum class DatParseState(val stop: Boolean) {
  OK(false), FurtherAnalysisNeeded(false),
  StopUnrecognizedOpcode(true), StopRecursionDetected(true), StopFunctionEnd(true), StopParserRequest(true),
  ResumeGame(false)
}
