@file:Suppress("UNUSED_PARAMETER", "RedundantWith")

package tl.extra.ccc.file.dat

import kio.KioInputStream
import kio.util.appendLine
import kio.util.toWHex
import tl.util.readDatString

/**
 * Experimental .DAT opcode parsing for CCC.
 * May not be accurate, a lot of guesswork here.
 */
class CccDatOpcodeParser(
  private val ctx: DatParsingContext,
  private val aggressive: Boolean = false
) : DatOpcodeParser {
  override fun parseOpcode(input: KioInputStream, opcodeGroup: Int, opcode: Int): DatParseState {
    return when (opcodeGroup) {
      0x00 -> parseOpcodeGroup00(input, opcode)
      0x01 -> parseOpcodeGroup01(input, opcode)
      0x02 -> parseOpcodeGroup02(input, opcode)
      0x03 -> parseOpcodeGroup03(input, opcode)
      0x04 -> parseOpcodeGroup04(input, opcode)
      0x05 -> parseOpcodeGroup05(input, opcode)
      0x0C -> parseOpcodeGroup0C(input, opcode)
      0x0D -> parseOpcodeGroup0D(input, opcode)
      0x0E -> parseOpcodeGroup0E(input, opcode)
      0x0F -> parseOpcodeGroup0F(input, opcode)
      0x10 -> parseOpcodeGroup10(input, opcode)
      0x11 -> parseOpcodeGroup11(input, opcode)
      0x12 -> parseOpcodeGroup12(input, opcode)
      0x13 -> parseOpcodeGroup13(input, opcode)
      0x14 -> parseOpcodeGroup14(input, opcode)
      0x15 -> parseOpcodeGroup15(input, opcode)
      0x16 -> parseOpcodeGroup16(input, opcode)
      0x17 -> parseOpcodeGroup17(input, opcode)
      0x18 -> parseOpcodeGroup18(input, opcode)
      0x19 -> parseOpcodeGroup19(input, opcode)
      0x1A -> parseOpcodeGroup1A(input, opcode)
      0x1B -> parseOpcodeGroup1B(input, opcode)
      0x1C -> parseOpcodeGroup1C(input, opcode)
      0x1E -> parseOpcodeGroup1E(input, opcode)
      0x1F -> parseOpcodeGroup1F(input, opcode)
      0x20 -> parseOpcodeGroup20(input, opcode)
      0x21 -> parseOpcodeGroup21(input, opcode)
      0x22 -> parseOpcodeGroup22(input, opcode)
      0x23 -> parseOpcodeGroup23(input, opcode)
      0x24 -> parseOpcodeGroup24(input, opcode)
      0x25 -> parseOpcodeGroup25(input, opcode)
      0x26 -> parseOpcodeGroup26(input, opcode)
      0x28 -> parseOpcodeGroup28(input, opcode)
      0x29 -> parseOpcodeGroup29(input, opcode)
      0x30 -> parseOpcodeGroup30(input, opcode)
      0x31 -> parseOpcodeGroup31(input, opcode)
      0x32 -> parseOpcodeGroup32(input, opcode)
      0x33 -> parseOpcodeGroup33(input, opcode)
      0x35 -> parseOpcodeGroup35(input, opcode)
      0x36 -> parseOpcodeGroup36(input, opcode)
      0x37 -> parseOpcodeGroup37(input, opcode)
      else -> DatParseState.StopUnrecognizedOpcode
    }
  }

  private fun parseOpcodeGroup00(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("datParser.currentPosPtr = 0")
        if (!aggressive) {
          return DatParseState.StopParserRequest
        }
      }
      0x02 -> {
        ctx.emit("datParser.resumeGameExecution_totalTimeBased(forTime = ${fetchValue(input)})")
      }
      0x03 -> {
        ctx.emit("datParser.setupVariableBlock(sizeInWords = ${fetchValue(input)})")
      }
      0x06 -> {
        ctx.emit("datParser.resumeGameExecution_currentTimeBased(forTime = ${fetchValue(input)})")
      }
      0x0C -> {
        val structId = fetchValue(input)
        val addr = readInt()
        ctx.emit("datParser.prepareSubParser(structId = $structId, parseStartAddr = ${addr.datPtr()})")
        ctx.addJumpBranch(addr)
      }
      0x0D -> {
        ctx.emit("datParser.resetFields(structId = ${fetchValue(input)}, listOf(currentPosPtr, callbackFilePt, timedMethodFilePtr))")
      }
      0x0E -> {
        ctx.emit("datParser.waitSubParser?(parserId = ${fetchValue(input)})")
      }
      0x0F -> {
        val ptr = readInt()
        ctx.emit("datParser.setTimedMethod(ptr = ${ptr.datPtr()})")
        ctx.addJumpBranch(ptr)
      }
      0x10 -> {
        val ptr = readInt()
        ctx.emit("datParser.setCallbackMethod(ptr = ${ptr.datPtr()})")
        ctx.addJumpBranch(ptr)
      }
      0x12 -> {
        ctx.emit("datParser.mode = datParser.mode | ${fetchValue(input)}")
      }
      0x32 -> {
        ctx.emit("//${readDatString().replace("\n", "")}")
      }
      0x33 -> {
        ctx.emit("nop_debugVarLog?(${fetchValue(input)})")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup01(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val offset = readInt()
        ctx.emit("goto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x01 -> {
        val offset = readInt()
        ctx.emit("if (datParser.opcodeResult0 == 0)")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x02 -> {
        val offset = readInt()
        ctx.emit("if (datParser.opcodeResult0 != 0)")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x03 -> {
        val offset = readInt()
        ctx.emit("if (datParser.opcodeResult0 > 0)")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x04 -> {
        val offset = readInt()
        ctx.emit("if (datParser.opcodeResult0 >= 0)")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x05 -> {
        val offset = readInt()
        ctx.emit("if (datParser.opcodeResult0 < 0)")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x06 -> {
        val offset = readInt()
        ctx.emit("if (datParser.opcodeResult0 <= 0)")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x14 -> {
        val entryPoint = readInt()
        ctx.emit("sub_${entryPoint.toWHex()}()")
        ctx.addFunctionEntryPoint(entryPoint)
      }
      0x1E -> {
        ctx.emit("return")
        return DatParseState.StopFunctionEnd
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup02(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("resultOf(${fetchValue(input)} - ${fetchValue(input)})")
      }
      0x01 -> {
        ctx.emit("updateMemoryValue(location = ${fetchValue(input)}, newValue = ${fetchValue(input)})")
      }
      0x02 -> {
        ctx.emit("resultOf(${fetchValue(input)} AND ${fetchValue(input)})")
      }
      0x03 -> {
        val target = fetchValue(input)
        val operand = fetchValue(input)
        ctx.emit("updateMemoryValue(location = $target, newValue = $target AND $operand)")
      }
      0x04 -> {
        val val1 = fetchValue(input)
        val val2 = fetchValue(input)
        ctx.emit("updateMemoryValue($val1, newValue = $val1 OR $val2)")
      }
      0x07 -> {
        val val1 = fetchValue(input)
        val val2 = fetchValue(input)
        ctx.emit("updateMemoryValue($val1, newValue = $val1 ADDU $val2)")
      }
      0x0C -> {
        val val1 = fetchValue(input)
        val val2 = fetchValue(input)
        ctx.emit("updateMemoryValue($val1, newValue = $val1 AND NOR($val2))")
      }
      0x15 -> {
        ctx.emit("moveTruncatedFloat(float = ${fetchValue(input)}, targetLocation = ${fetchValue(input)})")
      }
      0x17 -> {
        val target = fetchValue(input)
        val bitsCount = fetchValue(input)
        ctx.emit("updateMemoryValue(location = $target, newValue = shiftRightArithmetic(value = $target, byBits = $bitsCount))")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup03(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val var1 = readInt()
        val var2 = readInt()
        val offset = readInt()
        ctx.emit("if (${var1.varBlockOrImm()} == ${var2.varBlockOrImm()})")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x01 -> {
        val var1 = readInt()
        val var2 = readInt()
        val offset = readInt()
        ctx.emit("if (${var1.varBlockOrImm()} != ${var2.varBlockOrImm()})")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x03 -> {
        val var1 = readInt()
        val var2 = readInt()
        val offset = readInt()
        ctx.emit("if (${var1.varBlockOrImm()} < ${var2.varBlockOrImm()})")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
//            0x08 -> { //this opcode is problematic, not entirely accurate and often gives "Parser tried to parse non opcode value at XXXX, this may mean that some previous opcode was parsed incorrectly."
      // maybe second offset is added to first, need to check asm
//                val var1 = readInt()
//                val var2 = readInt()
//                val offset1 = readInt()
//                val offset2 = readInt()
//                ctx.emit("if (${var2.varBlockOrImm()} [???] ${var1.varBlockOrImm()})")
//                ctx.emitText("\t\t\t\t\tgoto ${offset1.toHex()}")
//                ctx.emitText("\t\t\t\t else")
//                ctx.emitText("\t\t\t\t\tgoto ${offset2.toHex()}")
//                ctx.addJumpBranch(offset1)
//                ctx.addJumpBranch(offset2)
//            }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup04(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("datParser.variableBlock.set(newValue = ${readInt().varBlockOrImm()}, wordNumber = ${readInt().subtract0x7FFFF00IfPossible()})")
      }
      0x01 -> {
        ctx.emit("datParser.variableBlock.addUnsigned(value = ${readInt().varBlockOrImm()}, wordNumber = ${readInt().subtract0x7FFFF00IfPossible()})")
      }
      0x03 -> {
        ctx.emit("datParser.variableBlock.multiply(by = ${readInt().varBlockOrImm()}, wordNumber = ${readInt().subtract0x7FFFF00IfPossible()})")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup05(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup0C(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup0D(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup0E(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup0F(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup10(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("opcode1000(${fetchValue(input)})")
      }
      0x01 -> {
        ctx.emit("opcode1001(${fetchValue(input)})")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup11(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit(
          "bgm.opcode1100(${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)})"
        )
      }
      0x01 -> {
        ctx.emit("bgm.opcode1101(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x19 -> {
        ctx.emit("bgm.opcode1119(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x1A -> {
        ctx.emit("bgm.opcode111A(${fetchValue(input)})")
      }
      0x1B -> {
        ctx.emit("bgm.opcode111B(${fetchValue(input)})")
      }
      0x21 -> {
        ctx.emit("bgm.opcode1121(${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x22 -> {
        ctx.emit("bgm.opcode1122(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x23 -> {
        ctx.emit("bgm.setPointerToBgmVoiceTable(${readInt().datPtr(0x4)})")
      }
      0x24 -> {
        ctx.emit("bgm.waitVoice?()")
      }
      0x27 -> {
        ctx.emit("bgm.opcode1127(${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)})")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup12(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup13(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x02 -> {
        ctx.emit("opcode1302(${fetchValue(input)})")
      }
      0x04 -> {
        ctx.emit("opcode1304(${fetchValue(input)}, ${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x08 -> {
        ctx.emit("opcode1308(${fetchValue(input)}, ${readFloat()})")
      }
      0x0F -> {
        ctx.emit("opcode130F(${fetchValue(input)}, '${readDatString()}', '${readDatString()}')")
      }
      0x10 -> {
        ctx.emit("opcode1310('${readDatString()}')")
      }
      0x13 -> {
        ctx.emit("opcode1313(${fetchValue(input)}, ${fetchValue(input)}, '${readDatString()}')")
      }
      0x14 -> {
        ctx.emit("opcode1314(${fetchValue(input)}, '${readDatString()}')")
      }
      0x15 -> {
        ctx.emit("opcode1315(${fetchValue(input)}, ${fetchValue(input)}, '${readDatString()}')")
      }
      0x16 -> {
        ctx.emit("opcode1316(${fetchValue(input)}, ${fetchValue(input)}, '${readDatString()}')")
      }
      0x17 -> {
        ctx.emit("opcode1317(${fetchValue(input)}, ${fetchValue(input)}, '${readDatString()}')")
      }
      0x18 -> {
        ctx.emit("opcode1318('${readDatString()}')")
      }
      0x1A -> {
        ctx.emit("opcode131A('${readDatString()}')")
      }
      0x1B -> {
        ctx.emit("opcode131B('${readDatString()}')")
      }
      0x21 -> {
        ctx.emit("opcode1321(${fetchValue(input)}, '${readDatString()}')")
      }
      0x22 -> {
        ctx.emit("opcode1322_updateField_40(${fetchValue(input)}, '${readDatString()}')")
      }
      0x23 -> {
        ctx.emit("opcode1323(${fetchValue(input)}, '${readDatString()}')")
      }
      0x24 -> {
        ctx.emit("opcode1324(${fetchValue(input)}, '${readDatString()}')")
      }
      0x25 -> {
        ctx.emit("opcode1325_updateField_1C(${fetchValue(input)}, '${readDatString()}')")
      }
      0x26 -> {
        ctx.emit("opcode1326(${fetchValue(input)}, '${readDatString()}')")
      }
      0x2B -> {
        ctx.emit("opcode132B('${readDatString()}', ${readInt().toWHex()})")
      }
      0x31 -> {
        ctx.emit("opcode1331(${fetchValue(input)}, ${fetchValue(input)}, '${readDatString()}')")
      }
      0x32 -> {
        ctx.emit("opcode1332()")
      }
      0x33 -> {
        ctx.emit("opcode1333()")
      }
      0x34 -> {
        ctx.emit("opcode1334(${fetchValue(input)})")
      }
      0x36 -> {
        ctx.emit("opcode1336('${readDatString()}', '${readDatString()}')")
      }
      0x37 -> {
        ctx.emit(
          "3dModels.rotateCharsFacingEachOther(steps = ${fetchValue(input)}, model1Name = '${readDatString()}', " +
            "model2Name = '${readDatString()}')"
        )
      }
      0x3F -> {
        ctx.emit("opcode133F('${readDatString()}')")
      }
      0x3E -> {
        ctx.emit("opcode133E('${readDatString()}')")
      }
      0x42 -> {
        ctx.emit("opcode1342('${readDatString()}')")
      }
      0x43 -> {
        ctx.emit("opcode1343('${readDatString()}')")
      }
      0x44 -> {
        ctx.emit("opcode1344(${fetchValue(input)}, ${fetchValue(input)}, '${readDatString()}')")
      }
      0x45 -> {
        ctx.emit("opcode1345(${fetchValue(input)}, ${fetchValue(input)}, '${readDatString()}')")
      }
      0x48 -> {
        ctx.emit("opcode1348(${fetchValue(input)}, '${readDatString()}')")
      }
      0x54 -> {
        ctx.emit("opcode1354()")
      }
      0x58 -> {
        ctx.emit("opcode1358('${readDatString()}', ${readFloat()}, ${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x5B -> {
        ctx.emit("opcode135B(${fetchValue(input)}, '${readDatString()}', ${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x5D -> {
        ctx.emit(
          "3dModels.rotateObjectsFacingEachOther(model1Id? = ${fetchValue(input)}, steps = ${fetchValue(input)}, " +
            "model2Name = '${readDatString()}')"
        )
      }
      0x60 -> {
        ctx.emit("opcode1360()")
      }
      0x70 -> {
        ctx.emit(
          "opcode1370(${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "'${readDatString()}')"
        )
      }
      0x71 -> {
        ctx.emit(
          "paramMng.opcode1371(${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()})"
        )
      }
      0x73 -> {
        ctx.emit("paramMng.opcode1373('${readDatString()}')")
      }
      0x74 -> {
        ctx.emit("paramMng.opcode1374()")
      }
      0x76 -> {
        ctx.emit("opcode1376('${readDatString()}', ${readInt().toWHex()})")
      }
      0x77 -> {
        ctx.emit("opcode1377('${readDatString()}', ${readInt().toWHex()})")
      }
      0x78 -> {
        ctx.emit(
          "opcode1378(${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, '${readDatString()}')"
        )
      }
      0x7F -> {
        ctx.emit("opcode137F(${readFloat()}, ${readFloat()}, ${readFloat()}, ${readFloat()}, '${readDatString()}', '${readDatString()}')")
      }
      0x85 -> {
        ctx.emit("opcode1385('${readDatString()}', ${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x99 -> {
        ctx.emit("opcode1399('${readDatString()}', ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0xA1 -> {
        ctx.emit(
          "opcode13A1('${readDatString()}', ${readInt().toWHex()}, ${readFloat()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()})"
        )
      }
      0xAB -> {
        ctx.emit("opcode13AB()")
      }
      0xB2 -> {
        ctx.emit("opcode13B2()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup14(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup15(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("opcode1500(${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x01 -> {
        ctx.emit("opcode1501(${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x02 -> {
        ctx.emit("opcode1502(${readFloat()} * 0.017453)")
      }
      0x05 -> {
        ctx.emit("opcode1505(${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x06 -> {
        ctx.emit("opcode1506(${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x07 -> {
        ctx.emit("opcode1507(${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x08 -> {
        ctx.emit("opcode1508(${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x09 -> {
        ctx.emit("opcode1509(${readInt().toWHex()})")
      }
      0x0A -> {
        ctx.emit("opcode150A(${readInt().toWHex()})")
      }
      0x0B -> {
        ctx.emit("opcode150B()")
      }
      0x0C -> {
        ctx.emit("opcode150C()")
      }
      0x0D -> {
        ctx.emit("opcode150D(${fetchValue(input)})")
      }
      0x0E -> {
        ctx.emit("opcode150E(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x0F -> {
        ctx.emit("opcode150F(${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x11 -> {
        ctx.emit("opcode1511(${fetchValue(input)})")
      }
      0x12 -> {
        ctx.emit("opcode1512()")
      }
      0x13 -> {
        ctx.emit("opcode1513(${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x14 -> {
        ctx.emit("opcode1514(${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x15 -> {
        ctx.emit("opcode1515(${readFloat()}, ${readFloat()})")
      }
      0x16 -> {
        ctx.emit("opcode1516(${readInt().toWHex()}, ${readInt().toWHex()}, ${readFloat()}, ${readFloat()})")
      }
      0x18 -> {
        ctx.emit("opcode1518()")
      }
      0x19 -> {
        ctx.emit("opcode1519(${fetchValue(input)})")
      }
      0x1B -> {
        ctx.emit("opcode151B(${readFloat()}, ${readFloat()}, ${readFloat()}, ${readFloat()}, ${readFloat()}, ${readFloat()})")
      }
      0x1C -> {
        ctx.emit("opcode151C()")
      }
      0x1D -> {
        ctx.emit("opcode151D(${readFloat()})")
      }
      0x1E -> {
        ctx.emit("opcode151E(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x1F -> {
        ctx.emit("opcode151F(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x2C -> {
        ctx.emit("opcode152C(${readFloat()})")
      }
      0x2E -> {
        ctx.emit("opcode152E()")
      }
      0x2F -> {
        ctx.emit("opcode152F()")
      }
      0x35 -> {
        ctx.emit("opcode1535()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup16(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x02 -> {
        ctx.emit("opcode1602(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x03 -> {
        val ptr = readInt()
        ctx.emit("g_setUpdateCollisionAnimationCallback1?(methodPtr = ${ptr.toWHex()})")
        ctx.addFunctionEntryPoint(ptr)
      }
      0x04 -> {
        val ptr = readInt()
        ctx.emit("g_setUpdateCollisionCallback2?(${ptr.toWHex()})")
        ctx.addFunctionEntryPoint(ptr)
      }
      0x07 -> {
        ctx.emit("opcode1607()")
      }

      0x0B -> {
        ctx.emit(
          "gField.field_54.setPointerToDatFunctionTable(pointerToDatFile = " +
            "${readInt().datPtr(input, this@CccDatOpcodeParser::parseGFieldPosTable)})})"
        )
        return DatParseState.FurtherAnalysisNeeded
      }
      0x0E -> {
        ctx.emit(
          "gField.field_58.setPointerToDat(pointerToDatFile = ${readInt().datPtr(input, this@CccDatOpcodeParser::parseGFieldTextTable)})})"
        )
      }
      0x0F -> {
        ctx.emit("opcode160F(${fetchValue(input)}, ${fetchValue(input)}, '${readDatString()}')")
      }
      0x19 -> {
        ctx.emit("opcode1619(${fetchValue(input)})")
      }
      0x29 -> {
        ctx.emit("opcode1629()")
      }
      0x2B -> {
        ctx.emit("opcode162B_nop?(${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x30 -> {
        ctx.emit("opcode1630(${readInt().datPtr(0x38)})")
      }
      0x31 -> {
        ctx.emit("opcode1631()")
      }
      0x50 -> {
        ctx.addTextPointerLoc(pos())
        val datTextPtr = readInt()
        val text = input.readDatString(at = datTextPtr, maintainStreamPos = true)
        ctx.emit(
          "dialog.prepareSaveDataRelatedDialogPopup(datTextPtr = " +
            "${datTextPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) [text: $text]"
        )
      }
      0x51 -> {
        ctx.emit("dialog.stopCurrentScriptExecution()")
      }
      0x52 -> {
        ctx.emit("dialog.retrieveYesNoDialogResult() [1: yes, 0: no]")
      }
      0x53 -> {
        ctx.addTextPointerLoc(pos())
        val datTextPtr = readInt()
        val text = input.readDatString(at = datTextPtr, maintainStreamPos = true)
        ctx.emit(
          "dialog.prepareSaveRelatedDialogPopup(datTextPtr = " +
            "${datTextPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) [text: $text]"
        )
      }
      0x54 -> {
        ctx.emit(
          "gField.field_5C.setPointerToDat(pointerToDatFile = " +
            "${readInt().datPtr(input, this@CccDatOpcodeParser::parseBackLogCallbackFunctions)})})"
        )
      }
      0x59 -> {
        val arg = readInt()
        ctx.addTextPointerLoc(pos())
        val datTextPtr = readInt()
        val text = input.readDatString(at = datTextPtr, maintainStreamPos = true)
        ctx.emit(
          "dialog.prepareItemReceivedGivenDialogPopup(arg = ${arg.toWHex()}, datTextPtr = " +
            "${datTextPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) [text: $text]"
        )
      }
      0x5E -> {
        ctx.addTextPointerLoc(pos())
        val datTextPtr = readInt()
        val text = input.readDatString(at = datTextPtr, maintainStreamPos = true)
        ctx.emit(
          "dialog.prepareOneLineDialogYesNoChoice(datTextPtr = ${datTextPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) " +
            "[text: $text]"
        )
      }
      0x5F -> {
        ctx.addTextPointerLoc(pos())
        val datTextPtr = readInt()
        ctx.addTextPointerLoc(pos())
        val datTextPtr2 = readInt()
        val text = input.readDatString(at = datTextPtr, maintainStreamPos = true)
        val text2 = input.readDatString(at = datTextPtr2, maintainStreamPos = true)
        ctx.emit(
          "dialog.prepareTwoLineDialogYesNoChoice(datTextPtr = ${datTextPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}, " +
            "datTextPtr2 = ${datTextPtr2.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) [text: $text, text2: $text2]"
        )
      }
      0x60 -> {
        ctx.addTextPointerLoc(pos())
        val datTextPtr = readInt()
        val text = input.readDatString(at = datTextPtr, maintainStreamPos = true)
        ctx.emit(
          "dialog.prepareOneLineDialogPopup(datTextPtr = ${datTextPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) [text: $text]"
        )
      }
      0x61 -> {
        ctx.addTextPointerLoc(pos())
        val datTextPtr = readInt()
        ctx.addTextPointerLoc(pos())
        val datTextPtr2 = readInt()
        val text = input.readDatString(at = datTextPtr, maintainStreamPos = true)
        val text2 = input.readDatString(at = datTextPtr2, maintainStreamPos = true)
        ctx.emit(
          "dialog.prepareTwoLineDialogPopup(datTextPtr = ${datTextPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}, " +
            "datTextPtr2 = ${datTextPtr2.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) [text: $text, text2: $text2]"
        )
      }
      0x64 -> {
        ctx.addTextPointerLoc(pos())
        val datTextPtr = readInt()
        ctx.addTextPointerLoc(pos())
        val datTextPtr2 = readInt()
        val text = input.readDatString(at = datTextPtr, maintainStreamPos = true)
        val text2 = input.readDatString(at = datTextPtr2, maintainStreamPos = true)
        ctx.emit(
          "dialog.prepareSaveRelatedDialogPopup(datTextPtr = ${datTextPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}, " +
            "datTextPtr2 = ${datTextPtr2.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) [text: $text, text2: $text2]"
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup17(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("opcode1700()")
      }
      0x01 -> {
        ctx.emit("opcode1701()")
      }
      0x05 -> {
        ctx.emit("opcode1705()")
      }
      0x64 -> {
        ctx.emit("save.flagSection.updateField(wordNumber = ${fetchValue(input)}, newValue = ${fetchValue(input)})")
      }
      0x65 -> {
        ctx.emit("save.flagSection.readField(wordNumber = ${fetchValue(input)})")
      }
      0x66 -> {
        ctx.emit("save.varSection.updateField(wordNumber = ${fetchValue(input)}, newValue = ${fetchValue(input)})")
      }
      0x67 -> {
        ctx.emit("save.varSection.readField(wordNumber = ${fetchValue(input)})")
      }
      0x6C -> {
        ctx.emit("if(save.flagSection.readField(number = ${readInt().toWHex()})) != 0")
        ctx.emitText("\t\t\t\t\tsave.varSection.updateField(wordNumber = ${readInt().toWHex()}, newValue = ${readInt().toWHex()})")
      }
      0x6D -> {
        ctx.emit("if(save.flagSection.readField(number = ${readInt().toWHex()})) == 0")
        ctx.emitText("\t\t\t\t\tsave.varSection.updateField(wordNumber = ${readInt().toWHex()}, newValue = ${readInt().toWHex()})")
      }
      0x7B -> {
        val value = "save.varSection.readField(wordNumber = ${readInt().toWHex()})"
        val arg1 = readInt().toWHex()
        val arg2 = readInt().toWHex()
        ctx.emit("if($value >= $arg1 && $value <= $arg2)")
        ctx.emitText("\t\t\t\t\topcodeResult = $value")
      }
      0x78 -> {
        ctx.emit("datParser.clearOpCodeResult0And1()")
      }
      0x79 -> {
        ctx.emit("datParser.clearOpCodeResult0And1()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup18(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x01 -> {
        ctx.emit("proc.updateInputBuffer()")
      }
      0x02 -> {
        ctx.emit("proc.updateAnimations()")
      }
      0x03 -> {
        ctx.emit("proc.updatePositions()")
      }
      0x04 -> {
        ctx.emit("proc.updateCollisions()")
      }
      0x05 -> {
        ctx.emit("proc.render3D()")
      }
      0x0B -> {
        ctx.emit("opcode180B()")
      }
      0x12 -> {
        ctx.emit("opcode1812_animRelated()")
      }
      0x32 -> {
        ctx.emit("opcode1832_anim(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x33 -> {
        ctx.emit("opcode1833_3dModels(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x34 -> {
        ctx.emit("opcode1834_animRelated()")
      }
      0x36 -> {
        ctx.emit("opcode1836(operation? = ${fetchValue(input)}, value? = ${fetchValue(input)})")
      }
      0x37 -> {
        ctx.emit("opcode1837()")
      }
      0x38 -> {
        ctx.emit("opcode1838_modelHandsManip(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x3B -> {
        ctx.emit("opcode183B_3dModels(${fetchValue(input)}, ${fetchValue(input)})")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup19(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup1A(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x02 -> {
        ctx.emit(
          "opcode1A02(${fetchValue(input)}, ${fetchValue(input)}, ${readFloat()}, ${readFloat()}, ${readFloat()}, ${readInt()}, ${readInt()}, " +
            "${readInt()}, '${readDatString()}')"
        )
      }
      0x09 -> {
        ctx.emit("opcode1A09(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x0E -> {
        ctx.emit("opcode1A0E(${fetchValue(input)}, ${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x0F -> {
        ctx.emit("opcode1A0F(${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x13 -> {
        ctx.emit("opcode1A13(${fetchValue(input)})")
      }
      0x21 -> {
        ctx.emit("opcode1A21()")
      }
      0x2D -> {
        ctx.emit("opcode1A2D(${fetchValue(input)}, ${fetchValue(input)}, '${readDatString()}')")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup1B(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup1C(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x33 -> {
        ctx.emit("opcode1C33(${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)})")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup1E(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x05 -> {
        ctx.emit("opcode1E05(${fetchValue(input)}, ${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x0F -> {
        ctx.emit("opcode1E0F(${fetchValue(input)})")
      }
      0x64 -> {
        ctx.emit(
          "opcode1E64(${fetchValue(input)}, ${fetchValue(input)}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()})"
        )
      }
      0x65 -> {
        ctx.emit(
          "opcode1E65(${fetchValue(input)}, ${fetchValue(input)}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()})"
        )
      }
      0x68 -> {
        ctx.emit("opcode1E68(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x6D -> {
        ctx.emit("opcode1E6D(${readInt().toWHex()})")
      }
      0x6E -> {
        ctx.emit("opcode1E6E(${readInt().toWHex()})")
      }
      0x6F -> {
        ctx.emit("opcode1E6F('${readDatString()}', ${readInt().toWHex()})")
      }
      0x70 -> {
        ctx.emit("opcode1E70(${readInt().toWHex()})")
      }
      0x71 -> {
        ctx.emit("opcode1E71()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup1F(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("pak.loadPakFileAsync(${fetchValue(input)}, '${readDatString()}')")
      }
      0x01 -> {
        ctx.emit("opcodeResult = pak.getPendingLoadDescriptorListSize()")
      }
      0x03 -> {
        ctx.emit("pak.returnNullIfPendingLoadDescriptorListIsNotEmpty()")
      }
      0x0C -> {
        ctx.emit("pak.releasePakByPath('${readDatString()}')")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup20(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("setupRenderTaskStructures(howMany = ${readInt().varBlockOrImm()})")
      }
      0x03 -> {
        ctx.emit("tex.unloadAndFreeTextureBySlotId(usageId = ${readInt().varBlockOrImm()})")
      }
      0x06 -> {
        ctx.emit("tex.loadAndLinkTxbFile(usageId = ${readInt().varBlockOrImm()}, txbPath = '${readDatString()}')")
      }
      0x0A -> {
        // renderTask.state = renderTask.state | ACTIVE
        ctx.emit(
          "renderTask.setTexture(structId = ${readInt().varBlockOrImm()}, textureId = ${readInt().varBlockOrImm()}, " +
            "texRenderMode = ${readInt().varBlockOrImm()}, texRenderMode2 = ${readInt().varBlockOrImm()})"
        )
      }
      0x0B -> {
        ctx.emit("renderTask.clear(structId = ${readInt().varBlockOrImm()})")
      }
      0x0C -> {
        // renderTask.state = renderTask.state | ACTIVE
        ctx.emit("renderTask.setTexture(structId = ${readInt().varBlockOrImm()}, textureId = ${readInt().varBlockOrImm()})")
      }
      0x0E -> {
        // renderTask.state = renderTask.state | ACTIVE
        ctx.emit("renderTask.update(structId = ${readInt().varBlockOrImm()}, texRenderMode2 = ${readInt().varBlockOrImm()})")
      }
      0x0F -> {
        // renderTask.state = renderTask.state | ACTIVE
        ctx.emit("renderTask.update(structId = ${readInt().varBlockOrImm()}, field_c = ${readInt().varBlockOrImm()})")
      }
      0x10 -> {
        ctx.emit("renderTask?.opcode2010(structId? = ${readInt().varBlockOrImm()}, ${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x11 -> {
        ctx.emit("renderTask.enableMode(structId = ${readInt().varBlockOrImm()}, mode = mode | ${readInt().varBlockOrImm()})")
      }
      0x12 -> {
        ctx.emit("opcode2012()")
      }
      0x13 -> {
        ctx.emit("renderTask.setTextureEntryId(${readInt().varBlockOrImm()}, ${readInt().varBlockOrImm()})")
      }
      0x14 -> {
        ctx.emit("opcode2014()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup21(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        // renderTask.state = renderTask.state | (ACTIVE | DIRTY)
        // renderTask.pos{X,Y,Z}State = renderTask.pos{X,Y,Z}State | 0x8000
        ctx.emit(
          "renderTask.setPosition(structId = ${readInt().varBlockOrImm()}, xyzSelector = ${readInt().varBlockOrImm()}, " +
            "pos = ${readInt().varBlockOrImm(printAsInteger = true)})"
        )
      }
      0x01 -> {
        ctx.emit(
          "renderTask.opcode2101(structId = ${readInt().varBlockOrImm()}, xyzSelector = ${readInt().varBlockOrImm()}, " +
            "v1 = ${readInt().varBlockOrImm(printAsInteger = true)}, v2 = ${readInt().varBlockOrImm(printAsInteger = true)})"
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup22(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup23(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup24(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        // renderTask.state = renderTask.state | ACTIVE
        ctx.emit("renderTask.updateAnimation(structId = ${readInt().varBlockOrImm()}, animation_fromColor = ${readInt().varBlockOrImm()})")
      }
      0x01 -> {
        // renderTask.state = renderTask.state | ACTIVE
        // renderTask.animation_currentTime = 0
        // renderTask.animation_enable = (animation_enable & 0xFFFFFFF0) OR 0x1
        // renderTask.animation_currentColor? = animation.fromColor
        ctx.emit(
          "renderTask.activateColorAnimation(structId = ${readInt().varBlockOrImm()}, animation_toColor = ${readInt().varBlockOrImm()}, " +
            "animation_endTime = ${readInt().varBlockOrImm()})"
        )
      }
      0x03 -> {
        // renderTask.state = renderTask.state | ACTIVE
        // renderTask.animation_currentTime = 0
        // renderTask.animation_enable = (animation_enable & 0xFFFFFFF0) OR 0x8 (!)
        // renderTask.animation_currentColor? = animation.fromColor
        ctx.emit(
          "renderTask.activateColorAnimation(structId = ${readInt().varBlockOrImm()}, animation_toColor = ${readInt().varBlockOrImm()}, " +
            "animation_endTime = ${readInt().varBlockOrImm()})"
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup25(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup26(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("renderTask.setMotion(structId = ${readInt().varBlockOrImm()}, motionMode = ${readInt().varBlockOrImm()})")
      }
      0x01 -> {
        val structId = readInt()
        val motionMode = readInt()
        ctx.addTextPointerLoc(pos())
        val datMotionControllerPtr = readInt()
        if (motionMode == 9) {
          ctx.emit(
            "renderTask.setMotion(structId = ${structId.varBlockOrImm()}, motionMode = ${motionMode.varBlockOrImm()}, " +
              "datMotionControllerPtr = ${datMotionControllerPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)})"
          )
        } else {
          ctx.emit(
            "renderTask.setMotion(structId = ${structId.varBlockOrImm()}, motionMode = ${motionMode.varBlockOrImm()}, " +
              "datMotionControllerPtr = ${datMotionControllerPtr.datPtr(input, this@CccDatOpcodeParser::parseMobLikeEntries)})"
          )
        }
      }
      0x02 -> {
        // this probably can't set motion data ptr to inside current dat file since datParser.fileBeginPtr is not accessed by opcode
        ctx.emit(
          "renderTask.setMotion(structId = ${readInt().varBlockOrImm()}, motionMode = ${readInt().varBlockOrImm()}, " +
            "mobMotionControllerPtr = ${readInt().varBlockOrImm()})"
        )
      }
      0x03 -> {
        ctx.emit(
          "renderTask.setMotion(structId = ${readInt().varBlockOrImm()}, motionMode = ${readInt().varBlockOrImm()}, " +
            "mobMotionControllerPtr = ${readInt().varBlockOrImm()}, field_28 = ${readInt().varBlockOrImm()})"
        )
      }
      0x04 -> {
        ctx.emit("renderTask.setMotion(structId = ${readInt().varBlockOrImm()}, mobMotionControllerPtr = ${readInt().varBlockOrImm()})")
      }
      0x06 -> {
        ctx.emit("renderTask.setMotion(structId = ${readInt().varBlockOrImm()}, mobEntryId = ${readInt().varBlockOrImm()})")
      }
      0x0A -> {
        val structId = readInt().varBlockOrImm()
        ctx.addTextPointerLoc(pos())
        val textPtr = readInt()
        val text = input.readDatString(at = textPtr, maintainStreamPos = true)
        ctx.emit(
          "renderTask.update(structId = $structId, datMotionControllerPtr = " +
            "${textPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) " +
            "[text: $text]"
        )
      }
      0x0B -> {
        ctx.emit(
          "renderTask(structId = ${readInt().toWHex()}).datMotionControllerPtr = *dynamicDAT(${fetchValue(input)})"
        )
      }
      0x0C -> {
        ctx.emit("renderTask(structId = ${readInt().varBlockOrImm()}).datMotionControllerPtr = 0")
      }
      0x33 -> {
        ctx.emit("opcodeResult = renderTask(structId = ${readInt().varBlockOrImm()}).mobMotionControllerPtr.field_C")
      }
      0x36 -> {
        ctx.emit("renderTask(structId = ${readInt().varBlockOrImm()}).mobMotionControllerPtr.field_C = ${readInt().varBlockOrImm()}")
      }
      0x38 -> {
        val structNum = readInt().varBlockOrImm()
        val textPtr = readInt()
        ctx.emit(
          "dialogBox.opcode2638(structNum = $structNum, " +
            "datEntryPtr = ${textPtr.datPtr(input, this@CccDatOpcodeParser::parseDialogRelatedEntries)})"
        )
      }
      0x39 -> {
        ctx.addTextPointerLoc(pos())
        val textPtr = readInt()
        val lineWidth = readInt().varBlockOrImm()
        val text = input.readDatString(at = textPtr, maintainStreamPos = true)
        ctx.emit(
          "dialogBox.pushText(datTextPtr = ${textPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}, lineWidth = $lineWidth) " +
            "[text: ${text.replace("\n", " ")}]"
        )
      }
      0x3A -> {
        ctx.emit("renderTask(structId = ${readInt().varBlockOrImm()}).mobMotionControllerPtr.field_0 = ${readInt().varBlockOrImm()}")
      }
      0x40 -> {
        ctx.emit(
          "renderTask.opcode2640(structId = ${readInt().varBlockOrImm()}, ${readInt().subtract0x7FFFF00IfPossible()}, " +
            "${readInt().subtract0x7FFFF00IfPossible()})"
        )
      }
      0x41 -> {
        ctx.emit(
          "renderTask(structId = ${readInt().varBlockOrImm()}).mobMotionControllerPtr.setTextLineRenderProperties(" +
            "letterSpacing = ${readInt().varBlockOrImm()}, verticalLineSpacing = ${readInt().varBlockOrImm()})"
        )
      }
      0x44 -> {
        ctx.emit("renderTask(structId = ${readInt().varBlockOrImm()}).mobMotionControllerPtr.opcode2644()")
      }
      0x45 -> {
        ctx.emit("renderTask(structId = ${readInt().varBlockOrImm()}).mobMotionControllerPtr..opcode2645()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup28(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x02 -> {
        // this for some reason changes motion mote to 0x1D if datId is 0x34 or 0x35
        // side: renderTask.mobMotionControllerPtr = 0
        // side: renderTask.motionMode = 0xA
        ctx.emit(
          "renderTask.linkExternalDatFile(structId = ${readInt().varBlockOrImm()}, " +
            "datMotionControllerPtr=getExternalDatParserPtr(datId = ${readInt().varBlockOrImm()}, flag=${readInt().varBlockOrImm()}))"
        )
      }
      0x05 -> {
        ctx.emit(
          "renderTask.get(structId = ${readInt().varBlockOrImm()}).datMotionControllerPtr.renderTasks.forEach " +
            "{ it.texRenderMode2 = ${readInt().varBlockOrImm()} }"
        )
      }
      0x0E -> {
        // reserve 0x20 bytes for each entry
        ctx.emit("prepareExternalDataBlockForRenderTasks(howMany = ${readInt().varBlockOrImm()})")
      }
      0x0F -> {
        // max text length is 0x20
        ctx.emit("copyTextureNameToExternalRenderTasksDataBlock(structId? = ${readInt().varBlockOrImm()}, '${readDatString()}')")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup29(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x02 -> {
        ctx.emit(
          "renderTask.createAndLinkMobController(structId = ${readInt().varBlockOrImm()}, " +
            "numberOfSubControllers = ${readInt().varBlockOrImm()})"
        )
      }
      0x03 -> {
        ctx.emit("renderTask.get(structNum = ${readInt().varBlockOrImm()}).mobMotionControllerPtr.releaseAndUnlink()")
      }
      0x04 -> {
        ctx.emit(
          "renderTask.updateMobSubController(structId = ${readInt().varBlockOrImm()}, subControllerId = ${readInt().varBlockOrImm()}, " +
            "field_0 = ${readInt().varBlockOrImm()})"
        )
      }
      0x05 -> {
        ctx.emit(
          "renderTask.updateMobController(structNum = ${readInt().varBlockOrImm()}, mobEntry? = ${readInt().varBlockOrImm()}, " +
            "enable = ${readInt().varBlockOrImm()})"
        )
      }
      0x06 -> {
        ctx.emit("renderTask.waitForMobMotionController(structNum = ${readInt().varBlockOrImm()})")
      }
      0x07 -> {
        ctx.emit("renderTask.updateMobController(structNum = ${readInt().varBlockOrImm()}, field_0 = field_0 OR ${readInt().varBlockOrImm()})")
      }
      0x08 -> {
        ctx.emit(
          "renderTask.updateMobController(structNum = ${readInt().varBlockOrImm()}, " +
            "field_0 = field_0 AND NOR(${readInt().varBlockOrImm()}, 0))"
        )
      }
      0x09 -> {
        ctx.emit("renderTask.updateMobController_opcode2909(structNum = ${readInt().varBlockOrImm()}, ${readInt().varBlockOrImm()})")
      }
      0x0A -> {
        ctx.emit("renderTask.get(structNum = ${readInt().varBlockOrImm()}).mobMotionControllerPtr.field_24 = ${readInt().varBlockOrImm()}")
      }
      0x0B -> {
        ctx.emit("opcodeResult = renderTask.get(structNum = ${readInt().varBlockOrImm()}).mobMotionControllerPtr.checkModeBit0And1Disabled()")
      }
      0x0C -> {
        ctx.emit(
          "renderTask.get(structNum = ${readInt().varBlockOrImm()}).mobMotionControllerPtr." +
            "activateAnim_opcode290C(${readInt().varBlockOrImm()}, ${readInt().varBlockOrImm()})"
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup30(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val arg = fetchValue(input)
        ctx.addTextPointerLoc(pos())
        val textPtr = readInt()
        val text = input.readDatString(at = textPtr, maintainStreamPos = true)
        ctx.emit(
          "bpt.initAndSet(type = $arg, textPtr = ${textPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) " +
            "[text: $text] [type: 0 - move, 1 - search, 2 - talk]"
        )
      }
      0x01 -> {
        ctx.emit("bpt.checkStillPresent()")
      }
      0x02 -> {
        ctx.emit("progressTime.init(${readInt().toWHex()})")
      }
      0x03 -> {
        ctx.emit("progressTime.checkStillPresent()")
      }
      0x04 -> {
        ctx.emit("opcode3004(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0x08 -> {
        ctx.emit("opcode3008(${readInt().toWHex()})")
      }
      0x09 -> {
        ctx.emit("opcode3009()")
      }
      0x0A -> {
        val arg = fetchValue(input)
        val arg2 = fetchValue(input)
        ctx.addTextPointerLoc(pos())
        val textPtr = readInt()
        val text = input.readDatString(at = textPtr, maintainStreamPos = true)
        ctx.emit(
          "choice.prepareAnswer(id = $arg, $arg2, textPtr = ${textPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) [text: $text]"
        )
      }
      0x0B -> {
        ctx.emit("choice.prepare(answerCount = ${fetchValue(input)})")
      }
      0x0C -> {
        ctx.emit("choice.clearBg?()")
      }
      0x0D -> {
        ctx.emit("choice.commit(${fetchValue(input)})")
      }
      0x0E -> {
        ctx.emit("choice.checkStillPresent()")
      }
      0x10 -> {
        ctx.emit("externalDatParser0x20and0x23.setField_D0(${readInt().toWHex()}) [i_con_sen and i_con_win]")
      }
      0x11 -> {
        ctx.emit("opcode3011(${readInt().toWHex()})")
      }
      0x12 -> {
        ctx.emit("externalDatParser0x34and0x0x35.setField_D0(${readInt().toWHex()}) [arrow and arrow2]")
      }
      0x14 -> {
        ctx.emit(
          "fieldBacklog.setDatDataBlock(pointerToDat = ${readInt().datPtr(input, this@CccDatOpcodeParser::parseBackLogStorageMemory)})"
        )
      }
      0x15 -> {
        ctx.addTextPointerLoc(pos())
        val textPtr = readInt()
        val letterSpacing = readInt().varBlockOrImm()
        val entryId = readInt().varBlockOrImm()
        val text = input.readDatString(at = textPtr, maintainStreamPos = true)
        ctx.emit(
          "fieldBacklog.pushText(datTextPtr = ${textPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}, " +
            "letterSpacing = $letterSpacing, entryId? = $entryId) [text: ${text.replace("\n", " ")}]"
        )
      }
      0x16 -> {
        ctx.emit("fieldBacklog.clearField_E0()") // E0 is pointerToDat data block
      }
      0x17 -> {
        ctx.emit("fieldBacklog.field_E0.clearField0And8()")
      }
      0x18 -> {
        ctx.emit("fieldBacklog.opcode3018()")
      }
      0x19 -> {
        ctx.emit("fieldBacklog.opcode3019()")
      }
      0x1A -> {
        ctx.emit("opcode301A(${readInt().toWHex()})")
      }
      0x1B -> {
        ctx.emit("opcode301B()")
      }
      0x21 -> {
        ctx.emit("opcode3021()")
      }
      0x22 -> {
        ctx.emit("opcode3022()")
      }
      0x23 -> {
        ctx.emit("sg?.init?(${readInt().toWHex()})")
      }
      0x24 -> {
        ctx.emit("sg?.checkStillPresent?()")
      }
      0x32 -> {
        ctx.emit("opcode3032()")
      }
      0x33 -> {
        ctx.emit("opcode3033()")
      }
      0x3C -> {
        ctx.emit("nameEntry.init()")
      }
      0x3D -> {
        ctx.emit("nameEntry.checkStillPresent()")
      }
      0x3E -> {
        ctx.emit("reizyu.init(commandSpellsLeftCount = ${readInt().toWHex()})")
      }
      0x3F -> {
        ctx.emit("reizyu.checkStillPresent()")
      }
      0x40 -> {
        ctx.emit("save?.section1.setField_84(newValue = ${readInt().toWHex()})")
      }
      0x50 -> {
        val arg = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        ctx.addTextPointerLoc(pos())
        val datTextPtr = readInt()
        val text = input.readDatString(at = datTextPtr, maintainStreamPos = true)
        ctx.emit(
          "inDungeonDialog_pushText?(arg = ${arg.toWHex()}, arg2 = ${arg2.toWHex()}, arg3 = ${arg3.toWHex()}," +
            " datTextPtr = ${datTextPtr.datPtr(input, this@CccDatOpcodeParser::parseShiftJisText)}) [text: $text]"
        )
      }
      0x52 -> {
        ctx.emit("opcode3052()")
      }
      0x72 -> {
        ctx.emit("opcode3072()")
      }
      0x73 -> {
        ctx.emit("opcode3073()")
      }
      0x96 -> {
        ctx.emit("opcode3096(${fetchValue(input)})")
      }
      0xAB -> {
        ctx.emit("opcode30AB()")
      }
      0xAD -> {
        ctx.emit("opcode30AD()")
      }
      0xB4 -> {
        ctx.emit("opcode30B4(${fetchValue(input)}, ${fetchValue(input)})")
      }
      0xB6 -> {
        ctx.emit("opcode30B6(${fetchValue(input)})")
      }
      0xB7 -> {
        ctx.emit("opcode30B7()")
      }
      0xB8 -> {
        ctx.emit("opcode30B8()")
      }
      0xC7 -> {
        ctx.emit("servantSelect.init()")
      }
      0xC8 -> {
        ctx.emit("servantSelect.checkStillPresent()")
      }
      0xCE -> {
        ctx.emit("opcode30CE()")
      }
      0xCF -> {
        ctx.emit("opcode30CF()")
      }
      0xD0 -> {
        ctx.emit("opcode30D0(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0xD1 -> {
        ctx.emit("opcodeResult = opcode30D1()")
      }
      0xD8 -> {
        ctx.emit("opcode30D8()")
      }
      0xFA -> {
        ctx.emit("opcode30FA()")
      }
      0xF6 -> {
        ctx.emit("opcode30F6()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup31(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup32(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup33(input: KioInputStream, opcode: Int): DatParseState = DatParseState.StopUnrecognizedOpcode

  private fun parseOpcodeGroup35(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x03 -> {
        ctx.emit("opcode3503()")
        return DatParseState.FurtherAnalysisNeeded
      }
      0x05 -> {
        ctx.emit("bgFld.opcode3505()")
      }
      0x06 -> {
        ctx.emit("gField.opcode3506()")
      }
      0x07 -> {
        ctx.emit("gField.opcode3507()")
      }
      0x08 -> {
        // allocated two memory blocks of specified size, updated 4 global variables
        ctx.emit("opcode3508(sizeInWords = ${readInt().toWHex()}, ${readInt().toWHex()})")
        return DatParseState.FurtherAnalysisNeeded
      }
      0x0D -> {
        ctx.emit("opcode350D(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x0E -> {
        ctx.emit("opcode350E(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x0F -> {
        ctx.emit("opcode350F(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x10 -> {
        ctx.emit(
          "opcode3510_effectPlay??(${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, '${readDatString()}')"
        )
      }
      0x13 -> {
        ctx.emit("opcode3513()")
      }
      0x14 -> {
        ctx.emit("nop()")
      }
      0x15 -> {
        ctx.emit("nop()")
      }
      0x18 -> {
        ctx.emit("opcode3518(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x1A -> {
        ctx.emit("opcode351A(${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x1E -> {
        ctx.emit("gField.field_48.enableBit0()")
      }
      0x1F -> {
        ctx.emit("gField.field_48.checkBit0Set()")
      }
      0x20 -> {
        ctx.emit("opcode3520(${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()})")
      }
      0x21 -> {
        ctx.emit("bgFld.opcode3521_loadDynamicModel?(${readInt().toWHex()}, ${readInt().toWHex()}, '${readDatString()}')")
      }
      0x23 -> {
        ctx.emit("opcode3523()")
      }
      0x24 -> {
        ctx.emit("opcode3524()")
      }
      0x25 -> {
        ctx.emit("opcode3525()")
      }
      0x27 -> {
        ctx.emit("gField.field_1F1.readByteUnsigned()")
      }
      0x2A -> {
        ctx.emit("gField.field_1F1.setByteToZero()")
      }
      0x2E -> {
        ctx.emit("gField.field_48.changeFlag0x08000000(useOr = ${readInt().toWHex()})")
      }
      0x2F -> {
        ctx.emit("gField.field_48.changeFlag0x10000000(useOr = ${readInt().toWHex()})")
      }
      0x31 -> {
        ctx.emit("opcode3531()")
      }
      0x32 -> {
        ctx.emit(
          "gField.opcode3532(${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)}, ${fetchValue(input)})"
        )
      }
      0x35 -> {
        ctx.emit("gField.opcode3535()")
      }
      0x36 -> {
        ctx.emit("gField.opcode3536()")
      }
      0x37 -> {
        ctx.emit("gField.opcode3537()")
      }
      0x39 -> {
        ctx.emit("gField.opcode3539(${readInt().toWHex()})")
      }
      0x3A -> {
        ctx.emit("gField.opcode353A()")
      }
      0x3B -> {
        ctx.emit("gField.clearFields(listOf(0x1F8, 0x1F4, 0x1FC))")
      }
      0x45 -> {
        ctx.emit(
          "quest.opcode3545(${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
            "${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, '${readDatString()}')"
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup36(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x01 -> {
        ctx.emit("addMoney(howMuch = ${fetchValue(input)})")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup37(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val ptr = readInt()
        ctx.emit("setupSubParserForEvent?(${ptr.datPtr()})")
        ctx.addJumpBranch(ptr)
      }
      0x01 -> {
        ctx.emit("waitUntilSubParserEvent?()")
      }
      0x02 -> {
        ctx.emit("event.prepareAndLoadEventPAK('${readDatString()}')")
      }
      0x03 -> {
        ctx.emit("event.checkEventPAKLoaded?()")
      }
      0x0F -> {
        // 02d_arg is probably chapter number set from other opcode
        ctx.emit("event.prepareFormattedString(04d_arg = ${readInt().toWHex()}) [ev_d_man_%04d_%02d or ev_d_wma_%04d_%02d]")
      }
      0x12 -> {
        ctx.emit("opcode3712(${readInt().toWHex()}, '${readDatString()}')")
      }
      0x18 -> {
        ctx.emit("opcode3718()")
      }
      0x19 -> {
        ctx.emit("opcode3719()")
      }
      0x1A -> {
        ctx.emit("opcode371A()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun fetchValue(input: KioInputStream): String {
    when (val mode = input.readInt()) {
      0 -> {
        return input.readInt().toWHex()
      }
      1 -> {
        return input.readFloat().toString()
      }
      3 -> {
        return "fetchVarBlock(${input.readInt().toWHex()})"
      }
      else -> {
        val arg = input.readInt()
        return if (mode == 2) {
          val group = arg shr 24
          val variable = arg and 0xFFFFFF
          "fetchValue(${mode.toWHex()}, group = ${group.toWHex()}, variable = ${variable.toWHex()})"
        } else {
          "fetchValue(${mode.toWHex()}, ${arg.toWHex()})"
        }
      }
    }
  }

  private fun Int.datPtr(coveredBytes: Int = -1): String {
    if (coveredBytes > 0) {
      ctx.addCoverage(this until this + coveredBytes)
    }
    return "*DAT(${this.toWHex()})"
  }

  private fun Int.datPtr(input: KioInputStream, parser: (StringBuilder, KioInputStream) -> Unit): String {
    val lastPos = input.pos()
    val textBuilder = StringBuilder()
    input.setPos(this)
    parser(textBuilder, input)
    val coveredBytes = input.pos() - this
    if (coveredBytes > 0) {
      ctx.addCoverage(this until this + coveredBytes)
    }
    input.setPos(lastPos)
    if (textBuilder.isNotEmpty()) {
      ctx.emitDataBlock(this, textBuilder.toString())
    }
    return "*DAT(${this.toWHex()})"
  }

  private fun Int.varBlockOrImm(printAsInteger: Boolean = false): String {
    if (this > 0x7FFFFF00) {
      return "varBlock(${(this - 0x7FFFFF00).toWHex()})"
    }
    return this.toWHex() + if (printAsInteger) " [as int: $this]" else ""
  }

  private fun Int.subtract0x7FFFF00IfPossible(): String {
    if (this > 0x7FFFFF00) {
      return (this - 0x7FFFFF00).toWHex()
    }
    return this.toWHex()
  }

  private fun parseDialogRelatedEntries(text: StringBuilder, input: KioInputStream): Unit = with(input) {
    text.appendLine(
      "DialogRelatedEntry(${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, ${readInt().toWHex()}, " +
        "${readInt().toWHex()}, ${readInt().toWHex()})"
    )
  }

  private fun parseMobLikeEntries(text: StringBuilder, input: KioInputStream): Unit = with(input) {
    val unk1 = readInt()
    val count = readInt()
    val unk2 = readShort().toInt()
    val unk3 = readShort().toInt()
    text.appendLine(
      "MobLikeHeader(unk1 = ${unk1.toWHex()}, count = ${count.toWHex()}, unk2(textureId?) = " +
        "${unk2.toWHex()}, unk3(time?) = ${unk3.toWHex()})"
    )
    if (count == 0) {
      return
    }
    if (count > 1000000) {
      ctx.emitWarning("Mob like header count is too big, skipping parsing mob entries")
      return
    }
    repeat(count) {
      val unkEntry1 = readFloat()
      val unkEntry2 = readFloat()
      val unkEntry3 = readInt()
      text.appendLine("MobLikeEntry(unkEntry1(originX?) = $unkEntry1, (originY?) = $unkEntry2, unkEntry3 = ${unkEntry3.toWHex()})")
    }
  }

  private fun parseBackLogStorageMemory(text: StringBuilder, input: KioInputStream): Unit = with(input) {
    // this is a DAT section where backlog will store pointers to previous texts
    // layout is kind of guessed but it appears that header holds number of max entries and font render properties
    val unk1 = readInt()
    val maxEntries = readInt()
    val unk2 = readInt()
    val unk3 = readInt()
    val lineWidth = readInt()
    val unk4 = readInt()
    val unk5 = readInt()
    val unk6 = readInt()
    val unk7 = readInt()
    text.appendLine(
      "BacklogHeader(unk1 = ${unk1.toWHex()}, maxEntries = ${maxEntries.toWHex()}, unk2 = ${unk2.toWHex()}, unk3 = ${unk3.toWHex()}, " +
        "lineWidth = ${lineWidth.toWHex()}, unk4 = ${unk4.toWHex()}, unk5 = ${unk5.toWHex()}, unk6 = ${unk6.toWHex()}, unk7 = ${unk7.toWHex()})"
    )
    var skippedEmptyEntries = 0
    repeat(maxEntries) {
      val entryId = readInt()
      val textPtr = readInt()
      if (entryId == 0 && textPtr == 0) {
        skippedEmptyEntries++
      } else {
        text.appendLine("BacklogEntry(entryId = ${entryId.toWHex()}, textPtr = ${textPtr.toWHex()})")
      }
    }
    text.appendLine("Skipped ${skippedEmptyEntries.toWHex()} empty backlog entries")
  }

  private fun parseGFieldPosTable(text: StringBuilder, input: KioInputStream): Unit = with(input) {
    val count = readInt()
    repeat(count) {
      val tablePtr = readInt()
      text.appendLine("GFieldPosTableEntry(tablePosPtr = ${tablePtr.toWHex()})")
      temporaryJump(tablePtr) {
        val entries = readInt()
        text.appendLine("\tEntries(count = $entries)")
        repeat(entries) {
          text.appendLine("\tEntry(${readFloat()}, ${readFloat()}, ${readFloat()})")
        }
        ctx.addCoverage(tablePtr until pos())
      }
    }
  }

  private fun parseGFieldTextTable(text: StringBuilder, input: KioInputStream): Unit = with(input) {
    val count = readInt()
    repeat(count) {
      val ptr = readInt()
      val textContent = readDatString(at = ptr, maintainStreamPos = true)
      text.appendLine("GFieldTextTableEntry(textPtr = ${ptr.toWHex()}) [text: '$textContent']")
      ctx.addCoverage(ptr..ptr + textContent.length)
    }
  }

  private fun parseBackLogCallbackFunctions(text: StringBuilder, input: KioInputStream): Unit = with(input) {
    while (true) {
      val possibleJumpBranch = readInt()
      // this is not perfect, we don't know where number of entries for this table is stored
      if (possibleJumpBranch <= 0 || possibleJumpBranch > ctx.getFileSize()) {
        break
      }
      text.appendLine("BacklogCallback(methodPtr = ${possibleJumpBranch.datPtr()})")
      ctx.addJumpBranch(possibleJumpBranch)
    }
    setPos(pos() - 0x4)
  }

  private fun parseShiftJisText(text: StringBuilder, input: KioInputStream): Unit = with(input) {
    input.readDatString()
  }
}
