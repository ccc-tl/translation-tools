package tl.extra.ccc.file.dat

import kio.KioInputStream
import kio.util.toUnsignedInt
import kio.util.toWHex
import tl.extra.fateOutput
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.Locale

class DatFile(
  private val file: File,
  private val outFile: File,
  manualFunctionsToParse: Array<Int> = emptyArray(),
  useCCC: Boolean = false,
  private val parseEventFunctions: Boolean = true,
  private val findAllFunctionsMode: FindAllFunctionsMode = FindAllFunctionsMode.NONE,
  aggressive: Boolean = false,
) : DatParsingContext {
  constructor(
    file: File,
    outName: String = file.nameWithoutExtension,
    manualFunctionsToParse: Array<Int> = emptyArray(),
    cccMode: Boolean = false,
    parseEventFunctions: Boolean = true,
    findAllFunctionsMode: FindAllFunctionsMode = FindAllFunctionsMode.NONE,
    aggressive: Boolean = false,
  ) : this(
    file,
    File(fateOutput, "$outName.dasm"),
    manualFunctionsToParse,
    cccMode,
    parseEventFunctions,
    findAllFunctionsMode,
    aggressive,
  )

  private val bytes = file.readBytes()

  private val output = PrintWriter(OutputStreamWriter(FileOutputStream(outFile), Charsets.UTF_8))

  private val opcodeParser = if (useCCC) CccDatOpcodeParser(this, aggressive) else ExtraDatOpcodeParser(this)

  private val functionEntryPoints = mutableListOf<FunctionEntryPoint>()
  private val functionEntryPointsToParse = mutableListOf<FunctionEntryPoint>()
  private val visitedAddresses = mutableListOf<Int>()

  private val jumpBranches = mutableListOf<Int>()
  private val waitingJumpBranches = mutableListOf<Int>()

  private val coverageTable = arrayOfNulls<Int?>(bytes.size)
  private var prevCoverageId = 0
  private val textPointers = mutableListOf<Int>()

  private val dataBlocks = mutableListOf<Pair<Int, String>>()
  private val warning = mutableListOf<String>()

  private var currentLineInfo = "[---]"

  init {
    manualFunctionsToParse.forEach { addFunctionEntryPoint(FunctionEntryPoint(it, "mSub_")) }
    coverTrailingZeroes()

    val input = KioInputStream(bytes)

    findAllFunctions()

    with(input) {
      val mainFunctionOffset = readInt()
      val exitFunctionOffset = readInt()
      addFunctionEntryPoint(FunctionEntryPoint(mainFunctionOffset, "main", false))
      addFunctionEntryPoint(FunctionEntryPoint(exitFunctionOffset, "exit", false))
      addCoverage(0x0 until 0x8)

      if (parseEventFunctions) {
        while (true) {
          val eventHandlerOffset = readInt()
          if (eventHandlerOffset == 0x00001E01) {
            break
          }
          addFunctionEntryPoint(FunctionEntryPoint(eventHandlerOffset, "event_", printAddr = true))
          addCoverage(pos() - 0x4 until pos())
        }
      }

      while (functionEntryPointsToParse.size > 0) {
        val entryPoint = functionEntryPointsToParse[0]
        functionEntryPointsToParse.removeAt(0)
        if (entryPoint.printAddr) {
          emitText("${entryPoint.prefix}${entryPoint.addr.toWHex()}():")
        } else {
          emitText("${entryPoint.prefix}():")
        }
        setPos(entryPoint.addr)
        try {
          parseCodeBlock(this)
        } catch (e: IOException) {
          emitText("STOP: IOException. Msg: ${e.message}. At ${pos().toWHex()}.")
        }
        emitText("-------\n")
      }

      close()
    }

    dataBlocks.forEach { (addr, text) ->
      emitText("Data block at ${addr.toWHex()}:")
      emitText(text)
    }
    warning.forEach { emitText(it) }

    output.close()
  }

  private fun findAllFunctions() {
    if (findAllFunctionsMode == FindAllFunctionsMode.NONE) return
    emitWarning("Using forced DAT function discovery")
    with(KioInputStream(bytes)) {
      while (!eof()) {
        val opcode = readInt()
        if (opcode == 0x00001401) {
          val addr = readInt()
          if (findAllFunctionsMode == FindAllFunctionsMode.NORMAL) {
            temporaryJump(addr - 0x4) {
              if (readInt() == 0x00001E01) {
                addFunctionEntryPoint(addr)
              }
            }
          } else {
            addFunctionEntryPoint(addr)
          }
        }
      }

      if (findAllFunctionsMode == FindAllFunctionsMode.AGGRESSIVE) {
        setPos(0)
        while (!eof()) {
          val opcode = readInt()
          if (opcode == 0x00001E01) {
            addFunctionEntryPoint(pos())
          }
        }
      }
    }
  }

  private fun parseCodeBlock(input: KioInputStream) = with(input) {
    jumpBranches.clear()
    waitingJumpBranches.clear()
    var parseState = DatParseState.OK
    while (parseState == DatParseState.OK || parseState == DatParseState.FurtherAnalysisNeeded) {
      val idx = pos()
      visitedAddresses.add(idx)
      if (eof()) {
        emitText("STOP: File end reached")
        return
      }
      val opcodeGroupByte = readByte()
      val opcodeByte = readByte()
      val checkByte1 = readByte()
      val checkByte2 = readByte()
      if (checkByte1.toInt() != 0 || checkByte2.toInt() != 0) {
        emitText("STOP: Non opcode value encountered: ${readInt(idx).toWHex()}. Block aborted.")
        emitText()
        emitWarning("Parser tried to parse non opcode value at ${idx.toWHex()}, this may mean that some previous opcode was parsed incorrectly.")
        return
      }
      val opcodeGroup = opcodeGroupByte.toUnsignedInt()
      val opcode = opcodeByte.toUnsignedInt()

      val strOpcode = "[${opcodeGroupByte.toWHex()} ${opcodeByte.toWHex()}]"
      val strIdx = "[${idx.toWHex()}]"
      currentLineInfo = "$strIdx $strOpcode"

      parseState = opcodeParser.parseOpcode(input, opcodeGroup, opcode)
      val coveredBytes = input.pos() - idx
      if ((!parseState.stop || parseState == DatParseState.StopParserRequest || parseState == DatParseState.StopFunctionEnd) && coveredBytes > 0) {
        addCoverage(idx until idx + coveredBytes)
      }

      when (parseState) {
        DatParseState.StopUnrecognizedOpcode -> {
          emitText("STOP: Unrecognized opcode at $currentLineInfo")
          emitText()
          emitWarning("Unrecognized opcode")
        }
        DatParseState.StopFunctionEnd -> {
          emitText("STOP: Function end reached")
          emitText()
        }
        DatParseState.StopRecursionDetected -> {
          emitText("STOP: Recursion detected")
          emitText()
        }
        DatParseState.StopParserRequest -> {
          emitText("STOP: Requested by instruction parser. This MAY NOT mean that script execution ends here.")
          emitText()
        }
        else -> {
        }
      }

      while (parseState.stop && waitingJumpBranches.size != 0) {
        val jump = waitingJumpBranches.removeAt(0)
        if (jump in visitedAddresses) {
          continue
        }
        emitText("INFO: Resume parsing at jump branch: ${jump.toWHex()}")
        setPos(jump)
        parseState = DatParseState.OK
      }
    }
  }

  override fun getFileSize(): Int {
    return bytes.size
  }

  override fun emitText(text: String) {
    output.println(text)
  }

  override fun emit(text: String) {
    output.println("$currentLineInfo: $text")
  }

  override fun emitDataBlock(addr: Int, text: String) {
    dataBlocks.add(Pair(addr, text))
  }

  override fun emitWarning(text: String) {
    val warnText = "WARN at $currentLineInfo: $text"
    warning.add(warnText)
    println(warnText)
  }

  override fun addFunctionEntryPoint(entryPoint: FunctionEntryPoint) {
    functionEntryPoints.forEach {
      if (it.addr == entryPoint.addr) return
    }
    functionEntryPoints.add(entryPoint)
    functionEntryPointsToParse.add(entryPoint)
  }

  override fun addJumpBranch(offset: Int) {
    if (!jumpBranches.contains(offset)) {
      jumpBranches.add(offset)
      waitingJumpBranches.add(offset)
    }
  }

  override fun addCoverage(range: IntRange) {
    val id = prevCoverageId + 1
    for (idx in range) {
      coverageTable[idx] = id
    }
    prevCoverageId = id
  }

  override fun addTextPointerLoc(ptr: Int) {
    textPointers.add(ptr)
  }

  private fun coverTrailingZeroes() {
    val id = prevCoverageId + 1
    for (idx in bytes.size - 1 downTo 0 step 4) {
      val byte0 = bytes[idx]
      val byte1 = bytes[idx - 1]
      val byte2 = bytes[idx - 2]
      val byte3 = bytes[idx - 3]
      val zeroByte = 0.toByte()
      if (byte0 == zeroByte && byte1 == zeroByte && byte2 == zeroByte && byte3 == zeroByte) {
        coverageTable[idx] = id
        coverageTable[idx - 1] = id
        coverageTable[idx - 2] = id
        coverageTable[idx - 3] = id
      } else {
        break
      }
    }
    prevCoverageId = id
  }

  fun writeHtml() {
    val html = StringBuilder()
    html.append(
      """
<html>
<head>
<style>
* { font-family: monospace; }
#mark0 { background-color:#a8d1ff; }
#mark1 { background-color:#fff2a8; }
</style>
</head>
<body>
""",
    )
    val lineWidth = 16
    var currentLine = 0
    var currentByteInLine = 0
    var asciiRep = ""
    var markOpen = false
    var markStyle = "mark1"
    html.append("${currentLine.toWHex()}h ")
    bytes.forEachIndexed { idx, byte ->
      asciiRep += getAscii(byte)

      val (coverageIdx, byteCovered) = isByteCovered(idx)
      val markNewStyle = when {
        coverageIdx % 2 == 0 -> "mark0"
        coverageIdx % 2 == 1 -> "mark1"
        else -> "mark0"
      }

      if (byteCovered && markOpen && markStyle != markNewStyle) {
        markStyle = markNewStyle
        html.append("</mark>")
        html.append("<mark id=$markStyle>")
      }

      if (byteCovered && !markOpen) {
        markStyle = markNewStyle
        html.append("<mark id=$markStyle>")
        markOpen = true
      }

      if (!byteCovered && markOpen) {
        html.append("</mark>")
        markOpen = false
      }

      html.append(byte.toWHex())

      html.append(" ")
      currentByteInLine++
      if (currentByteInLine >= lineWidth) {
        if (markOpen) {
          html.append("</mark>")
        }
        html.append(escapeHTML(asciiRep))
        html.append("<br>")
        currentLine += lineWidth
        if (idx != bytes.size - 1) {
          html.append("${currentLine.toWHex()}h ")
        }
        asciiRep = ""
        currentByteInLine = 0
        if (markOpen) {
          html.append("<mark id=$markStyle>")
        }
      }
    }

    html.append("</body></html>")
    File(fateOutput, "${outFile.nameWithoutExtension}.html").writeText(html.toString())
    println("Wrote HTML. Coverage ${"%.03f".format(Locale.US, calculateCoverage())}%")
  }

  fun calculateCoverage(): Float {
    var coveredBytes = 0
    bytes.forEachIndexed { idx, _ ->
      val (_, byteCovered) = isByteCovered(idx)
      if (byteCovered) coveredBytes++
    }
    return coveredBytes * 100f / bytes.size
  }

  private fun isByteCovered(addr: Int): Pair<Int, Boolean> {
    val idx = coverageTable[addr]
      ?: return Pair(-1, false)
    return Pair(idx, true)
  }
}

enum class FindAllFunctionsMode {
  NONE,
  NORMAL,
  AGGRESSIVE,
}

fun getAscii(byte: Byte): Char {
  if (byte in 32..126) {
    return byte.toInt().toChar()
  }
  return '.'
}

fun escapeHTML(s: String): String {
  val out = StringBuilder()
  for (c in s) {
    if (c.code > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
      out.append("&#")
      out.append(c.code)
      out.append(';')
    } else {
      out.append(c)
    }
  }
  return out.toString()
}
