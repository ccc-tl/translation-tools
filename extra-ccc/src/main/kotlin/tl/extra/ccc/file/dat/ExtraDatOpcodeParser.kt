package tl.extra.ccc.file.dat

import kio.KioInputStream
import kio.util.toWHex
import tl.util.readDatString

/**
 * Experimental .DAT opcode parsing for Extra.
 * May not be accurate, a lot of guesswork here.
 */
class ExtraDatOpcodeParser(private val ctx: DatParsingContext) : DatOpcodeParser {
  override fun parseOpcode(input: KioInputStream, opcodeGroup: Int, opcode: Int): DatParseState {
    return when (opcodeGroup) {
      0x00 -> parseOpcodeGroup00(input, opcode)
      0x01 -> parseOpcodeGroup01(input, opcode)
      0x02 -> parseOpcodeGroup02(input, opcode)
      0x03 -> parseOpcodeGroup03(input, opcode)
      0x04 -> parseOpcodeGroup04(input, opcode)
      0x0C -> parseOpcodeGroup0C(input, opcode)
      0x0E -> parseOpcodeGroup0E(input, opcode)
      0x0F -> parseOpcodeGroup0F(input, opcode)
      0x10 -> parseOpcodeGroup10(input, opcode)
      0x11 -> parseOpcodeGroup11(input, opcode)
      0x13 -> parseOpcodeGroup13(input, opcode)
      0x15 -> parseOpcodeGroup15(input, opcode)
      0x16 -> parseOpcodeGroup16(input, opcode)
      0x17 -> parseOpcodeGroup17(input, opcode)
      0x18 -> parseOpcodeGroup18(input, opcode)
      0x1A -> parseOpcodeGroup1A(input, opcode)
      0x1E -> parseOpcodeGroup1E(input, opcode)
      0x1F -> parseOpcodeGroup1F(input, opcode)
      0x20 -> parseOpcodeGroup20(input, opcode)
      0x21 -> parseOpcodeGroup21(input, opcode)
      0x24 -> parseOpcodeGroup24(input, opcode)
      0x26 -> parseOpcodeGroup26(input, opcode)
      0x28 -> parseOpcodeGroup28(input, opcode)
      0x29 -> parseOpcodeGroup29(input, opcode)
      0x30 -> parseOpcodeGroup30(input, opcode)
      else -> DatParseState.StopUnrecognizedOpcode
    }
  }

  /** DatParser management */
  private fun parseOpcodeGroup00(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("datParser.currentPosPtr = 0")
      }
      0x01 -> {
        ctx.emit("enableParserMode(1); datParser.currentPosPtr = 0")
        return DatParseState.StopParserRequest
      }
      0x02 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("resumeGameExecution(forTime = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x03 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("setup256ByteVariableBlock(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x05 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val pakPath = readDatString()
        ctx.emit("loadNestedDATFile(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), \"$pakPath\")")
      }
      0x06 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("updateDatParserField6C(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()})) [side: suspend execution]")
      }
      0x09 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("suspendExecutionUntil(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x10 -> {
        val ptr = readInt()
        ctx.emit("setErrorHandler?(ptrToDATFile = ${ptr.toWHex()})")
        if (ptr != 0) {
          ctx.addFunctionEntryPoint(ptr)
        }
      }
      0x0A -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("setupNestedDatParserInIesCmd1(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x0C -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val addr = readInt()

        ctx.emit(
          "iesCmd1ControlStructureOperation_prepareNested(structNum = fetchValue(${getMemoryMode.toWHex()}, " +
            "${getMemoryArg.toWHex()}), addr = ${addr.toWHex()})",
        )
        ctx.addJumpBranch(addr)
      }
      0x0D -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()

        ctx.emit(
          "iesCmd1ControlStructureOperation_forkNested?(structNum = fetchValue(${getMemoryMode.toWHex()}, " +
            "${getMemoryArg.toWHex()})) [is structNum relative?]",
        )
      }
      0x0F -> {
        val addr = readInt()
        ctx.emit("opcode_000F(address = ${addr.toWHex()})")
        ctx.addJumpBranch(addr)
      }
      0x0E -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("iesCmd1ControlStructureOperation2_andSuspendExec(structNum = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x12 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("enableParserMode(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x32 -> {
        val text = readDatString().replace("\n", "")
        ctx.emit("//$text")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** Conditional goto using result of if instructions or opcode results, function calls, returns */
  @Suppress("UNUSED_VARIABLE")
  private fun parseOpcodeGroup01(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val offset = readInt()
        ctx.emit("goto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x01 -> {
        val offset = readInt()
        ctx.emit("if (datParser.field70 == 0)")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x02 -> {
        val offset = readInt()
        ctx.emit("if (datParser.field70 != 0)")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x14 -> {
        val entryPoint = readInt()
        ctx.addFunctionEntryPoint(entryPoint)
        ctx.emit("sub_${entryPoint.toWHex()}()")
      }
      0x1E -> {
        ctx.emit("return")
        return DatParseState.StopFunctionEnd
      }
      0x1F -> {
        val pos = pos()
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val offset1 = readInt()
        val offset2 = readInt()
        val offset3 = readInt()
        val offset4 = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
//                ctx.addJumpBranch(pos + offset1)
//                ctx.addJumpBranch(pos + offset2)
//                ctx.addJumpBranch(pos + offset3)
//                ctx.addJumpBranch(pos + offset4)
        ctx.emit(
          "conditionalRelativeJump(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}), " +
            "offset1 = ${offset1.toWHex()}, offset2 = ${offset1.toWHex()}, offset3 = ${offset1.toWHex()}, offset4 = ${offset1.toWHex()}, " +
            "arg1 = ${arg1.toWHex()}, arg2 = ${arg2.toWHex()}",
        )
        ctx.emitWarning("Can't calculate conditional jump offset, some script data might not be processed")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** If instructions */
  private fun parseOpcodeGroup02(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "updateDatParserField70AndField74(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}) - " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }
      0x01 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "updateMemoryValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}, " +
            "newValue = fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()})) [side: new value in field_70 and field_74]",
        )
      }
      0x02 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "updateDatParserField70AndField74(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}) AND " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }
      0x03 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "updateDatParserField70AndField74AndMemoryValue(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}) AND " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }
      0x04 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "updateMemoryValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}, " +
            "newValue = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}) OR fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()})) " +
            "[side: new value in field_70 and field_74]",
        )
      }
      0x07 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "updateMemoryValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}, " +
            "newValue = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}) + (ADDU) fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()})) " +
            "[side: new value in field_70 and field_74]",
        )
      }
      0x0C -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "updateMemoryValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}, " +
            "newValue = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}) AND (NOT fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}))) " +
            "[side: new value in field_70 and field_74]",
        )
      }
      0x15 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode0215_floats(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}, ${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()})",
        )
      }
      0x17 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode0217(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}, ${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()})",
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** Conditional goto optionally using external variables block values */
  private fun parseOpcodeGroup03(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        val offset = readInt()
        ctx.emit("if (varBlockOrImm(${arg1.toWHex()}) == varBlockOrImm(${arg2.toWHex()}))")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x01 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        val offset = readInt()
        ctx.emit("if (varBlockOrImm(${arg1.toWHex()}) != varBlockOrImm(${arg2.toWHex()}))")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x03 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        val offset = readInt()
        ctx.emit("if (arg2 = varBlockOrImm(${arg2.toWHex()}) < arg1 = varBlockOrImm(${arg1.toWHex()}))")
        ctx.emitText("\t\t\t\t\tgoto ${offset.toWHex()}")
        ctx.addJumpBranch(offset)
      }
      0x08 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        val offset1 = readInt()
        val offset2 = readInt()
        ctx.emit("if (arg2 = varBlockOrImm(${arg2.toWHex()}) [???] arg1 = varBlockOrImm(${arg1.toWHex()}))")
        ctx.emitText("\t\t\t\t\tgoto ${offset1.toWHex()}")
        ctx.emitText("\t\t\t\telse")
        ctx.emitText("\t\t\t\t\tgoto ${offset2.toWHex()}")
        ctx.addJumpBranch(offset1)
        ctx.addJumpBranch(offset2)
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** DatParser external variables block management */
  private fun parseOpcodeGroup04(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit(
          "updateParserVariableBlockValue_set(newValue = varBlockOrImm(${arg1.toWHex()}), " +
            "fieldNumber = ${(arg2 - 0x7FFFFF00).toWHex()}) [raw fieldNumber: ${arg2.toWHex()}]",
        )
      }
      0x01 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit(
          "updateParserVariableBlockValue_addUnsigned(addedValue = varBlockOrImm(${arg1.toWHex()}), " +
            "fieldNumber = ${(arg2 - 0x7FFFFF00).toWHex()}) [raw fieldNumber: ${arg2.toWHex()}]",
        )
      }
      0x03 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit(
          "opcode0403(arg1 = varBlockOrImm(${arg1.toWHex()}), arg2 = ${(arg2 - 0x7FFFFF00).toWHex()}) " +
            "[raw fieldNumber: ${arg2.toWHex()}]",
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup0C(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode0C00(fetchValue?(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup0E(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val path = readDatString()
        ctx.emit("loadFLDFile('$path')")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup0F(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x06 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val string = readDatString()
        val bytes = readBytes(0x2C).joinToString(separator = "", transform = { it.toWHex() })
        ctx.emit(
          "opcode0F06_setupIesPSPModelPacketRelated(fetchValue(${getMemoryMode.toWHex()}, " +
            "${getMemoryArg.toWHex()}), $string, $bytes)",
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup10(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1000(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x01 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit(
          "updateDatParserField70andField74(getValue(base = 0x3D81F8, which = fetchValue?(${getMemoryMode.toWHex()}, " +
            "${getMemoryArg.toWHex()})))",
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** Sound and BGM management */
  private fun parseOpcodeGroup11(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val getMemoryMode3 = readInt()
        val getMemoryArg3 = readInt()
        val getMemoryMode4 = readInt()
        val getMemoryArg4 = readInt()
        ctx.emit(
          "opcode1100_BGMRelated(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}), " +
            "fetchValue(${getMemoryMode3.toWHex()}, ${getMemoryArg3.toWHex()}), " +
            "fetchValue(${getMemoryMode4.toWHex()}, ${getMemoryArg4.toWHex()}))",
        )
      }
      0x01 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode1101(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}))",
        )
      }
      0x14 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1114_setupSoundManager?(fetchValue?(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x15 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1115(fetchValue?(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x1B -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode111B_musicRelated(fetchValue?(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x21 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val getMemoryMode3 = readInt()
        val getMemoryArg3 = readInt()
        ctx.emit(
          "opcode1121(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}), " +
            "fetchValue(${getMemoryMode3.toWHex()}, ${getMemoryArg3.toWHex()}))",
        )
      }
      0x23 -> {
        val ptr = readInt()
        ctx.emit("opcode1123_dialogRelated(pointerToDATFile = ${ptr.toWHex()})")
      }
      0x24 -> {
        ctx.emit("opcode1124()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** 3D model management */
  private fun parseOpcodeGroup13(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x02 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1302(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x04 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val x = readFloat()
        val y = readFloat()
        val z = readFloat()
        ctx.emit("opcode1304(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), x = $x, y = $y, z = $z)")
      }
      0x08 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val imm = readInt()
        ctx.emit("opcode1308(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), ${imm.toWHex()})")
      }
      0x09 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val getMemoryMode3 = readInt()
        val getMemoryArg3 = readInt()
        val modelName = readDatString()
        ctx.emit(
          "opcode1309_loadCharacter?(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}), " +
            "fetchValue(${getMemoryMode3.toWHex()}, ${getMemoryArg3.toWHex()}), '$modelName')",
        )
      }
      0x0F -> {
        val arg1 = readInt()
        val arg2 = readFloat()
        val s1 = readDatString()
        val s2 = readDatString()
        ctx.emit("opcode130F(${arg1.toWHex()}, $arg2, '$s1', '$s2')")
      }
      0x10 -> {
        val string = readDatString()
        ctx.emit("opcode1310('$string')")
      }
      0x11 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val getMemoryMode3 = readInt()
        val getMemoryArg3 = readInt()
        val getMemoryMode4 = readInt()
        val getMemoryArg4 = readInt()
        ctx.emit(
          "opcode1311_loadCharacterRelated?(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}), " +
            "fetchValue(${getMemoryMode3.toWHex()}, ${getMemoryArg3.toWHex()}), fetchValue(${getMemoryMode4.toWHex()}, " +
            "${getMemoryArg4.toWHex()}))",
        )
      }
      0x13 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val string = readDatString()
        ctx.emit(
          "opcode1313(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}), '$string')",
        )
      }
      0x14 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val string = readDatString()
        ctx.emit("opcode1314_characterRelated(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), '$string')")
      }
      0x15 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val string = readDatString()
        ctx.emit(
          "opcode1315(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}), '$string')",
        )
      }
      0x16 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val string = readDatString()
        ctx.emit(
          "opcode1316(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}), '$string')",
        )
      }
      0x17 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val string = readDatString()
        ctx.emit(
          "opcode1317(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}), '$string')",
        )
      }
      0x18 -> {
        val string = readDatString()
        ctx.emit("opcode1318('$string')")
      }
      0x1A -> {
        val string = readDatString()
        ctx.emit("opcode131A_scheduleCharacterForLoading?('$string')")
      }
      0x1B -> {
        val string = readDatString()
        ctx.emit("opcode131B_characterRelated($string)")
      }
      0x1E -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val modelName = readDatString()
        ctx.emit(
          "opcode131E(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}), " +
            "'$modelName')",
        )
      }
      0x1F -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val getMemoryMode3 = readInt()
        val getMemoryArg3 = readInt()
        val modelName = readDatString()
        ctx.emit(
          "opcode131F(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}), " +
            "fetchValue(${getMemoryMode3.toWHex()}, ${getMemoryArg3.toWHex()}), '$modelName')",
        )
      }
      0x21 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val string = readDatString()
        ctx.emit("opcode1321_characterRelated(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), $string)")
      }
      0x22 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val string = readDatString()
        ctx.emit("opcode1322(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), '$string')")
      }
      0x23 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val string = readDatString()
        ctx.emit("opcode1323(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), '$string')")
      }
      0x24 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val string = readDatString()
        ctx.emit("opcode1324(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), '$string')")
      }
      0x25 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val string = readDatString()
        ctx.emit("opcode1325(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), '$string')")
      }
      0x2E -> {
        val string = readDatString()
        ctx.emit("opcode132E('$string')")
      }
      0x32 -> {
        ctx.emit("opcode1332()")
      }
      0x33 -> {
        ctx.emit("opcode1333_gCharFloatOperations()")
      }
      0x34 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1334(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x42 -> {
        val string = readDatString()
        ctx.emit("opcode1342_characterRelated($string)")
      }
      0x51 -> {
        val string = readDatString()
        ctx.emit("opcode1351($string)")
      }
      0x54 -> {
        ctx.emit("opcode1354_floatOperations()")
      }
      0x61 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1364(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x66 -> {
        ctx.emit("opcode1366_gField_releaseControls?()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup15(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val arg1 = readFloat()
        val arg2 = readFloat()
        val arg3 = readFloat()
        ctx.emit("opcode1500(arg1 = $arg1, arg2 = $arg2, arg3 = $arg3)")
      }
      0x01 -> {
        val arg1 = readFloat()
        val arg2 = readFloat()
        val arg3 = readFloat()
        ctx.emit("opcode1501(arg1 = $arg1, arg2 = $arg2, arg3 = $arg3)")
      }
      0x02 -> {
        val arg1 = readInt()
        ctx.emit("opcode150F(arg1 = ${arg1.toWHex()}) [ptrToDATFile?]")
      }
      0x07 -> {
        val arg1 = readFloat()
        val arg2 = readFloat()
        val arg3 = readFloat()
        ctx.emit("opcode1507(arg1 = $arg1, arg2 = $arg2, arg3 = $arg3)")
      }
      0x08 -> {
        val arg1 = readFloat()
        val arg2 = readFloat()
        val arg3 = readFloat()
        ctx.emit("opcode1508(arg1 = $arg1, arg2 = $arg2, arg3 = $arg3)")
      }
      0x09 -> {
        val arg1 = readInt()
        ctx.emit("opcode1509(arg1 = ${arg1.toWHex()}) [ptrToDATFile?]")
      }
      0x0A -> {
        val arg1 = readInt()
        ctx.emit("opcode150A(arg1 = ${arg1.toWHex()})")
      }
      0x0C -> {
        ctx.emit("opcode150C()")
      }
      0x0B -> {
        val arg1 = readInt()
        ctx.emit("opcode150B(arg1 = ${arg1.toWHex()})")
      }
      0x0D -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode150D(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x0F -> {
        val arg1 = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        ctx.emit("opcode150F(arg1 = ${arg1.toWHex()}, arg2 = ${arg2.toWHex()}, arg3 = ${arg3.toWHex()})")
      }
      0x12 -> {
        ctx.emit("opcode1512()")
      }
      0x13 -> {
        val arg1 = readFloat()
        val arg2 = readFloat()
        val arg3 = readFloat()
        ctx.emit("opcode1513_storeInMainCpp(arg1 = $arg1, arg2 = $arg2, arg3 = $arg3)")
      }
      0x14 -> {
        val arg1 = readFloat()
        val arg2 = readFloat()
        val arg3 = readFloat()
        ctx.emit("opcode1514_storeInMainCpp(arg1 = $arg1, arg2 = $arg2, arg3 = $arg3)")
      }
      0x18 -> {
        ctx.emit("opcode1518()")
      }
      0x19 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1519(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** Field (map) properties management */
  private fun parseOpcodeGroup16(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("loadMoreFieldRelatedPAKFiles(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x02 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode1602(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}))",
        )
      }
      0x05 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val getMemoryMode3 = readInt()
        val getMemoryArg3 = readInt()
        val path = readDatString()
        ctx.emit(
          "updateGlobalFieldDatFileTable(slot = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "tableEntry.field0 = fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}), " +
            "tableEntry.field4 = fetchValue(${getMemoryMode3.toWHex()}, ${getMemoryArg3.toWHex()}), " +
            "tableEntry.path = $path)",
        )
      }
      0x07 -> {
        ctx.emit("opcode1607()")
      }
      0x0B -> {
        val ptr = readInt()
        ctx.emit("opcode160B_gFieldStorePointerToDATFileAt0x1D4(pointerToDatFile = ${ptr.toWHex()})")
      }
      0x0E -> {
        val ptr = readInt()
        ctx.emit("opcode160E_gFieldStorePointerToDATFileAt0x1D8(pointerToDatFile = ${ptr.toWHex()})")
      }
      0x10 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode1610_gFieldUpdateField0x260(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }
      0x14 -> {
        val immediate = readInt()
        ctx.emit("g_FieldStorePointerToDatFile(ptr = ${immediate.toWHex()})")
      }
      0x15 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit(
          "getPointerToCFieldInstance(unknown = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()})) " +
            "[side: pointer in field_70 and field_74",
        )
      }
      0x1F -> {
        val x = readFloat()
        val y = readFloat()
        val z = readFloat()
        ctx.emit("opcode161F_updateGFieldFields0x200,0x204,0x208(x? = $x, y? = $y, z? = $z)")
      }
      0x20 -> {
        val ptr = readInt()
        ctx.emit("opcode1620_gFieldStorePointer(orValue?)ToDATFileAt0x220(pointerToDatFile = ${ptr.toWHex()})")
      }
      0x21 -> {
        val ptr = readInt()
        ctx.emit("opcode1621_gFieldStorePointer(orValue?)ToDATFileAt0x1F8(pointerToDatFile = ${ptr.toWHex()})")
      }
      0x22 -> {
        val x = readFloat()
        val y = readFloat()
        val z = readFloat()
        ctx.emit("opcode1622_updateGFieldFields0x210,0x214,0x218(x? = $x, y? = $y, z? = $z)")
      }
      0x28 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode1628(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}))",
        )
      }
      0x29 -> {
        ctx.emit("opcode1629_arena()")
      }
      0x2A -> {
        ctx.emit("opcode162A_unloadCurrentField?()")
      }
      0x30 -> {
        val offset = readInt()
        ctx.emit("copyDataFromDatFile(ptr = ${offset.toWHex()})") // 0x38 bytes?
      }
      0x31 -> {
        ctx.emit("opcode1631()")
      }
      0x32 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode1632(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), fetchValue(${getMemoryMode2.toWHex()}, " +
            "${getMemoryArg2.toWHex()}))",
        )
      }
      0x37 -> {
        ctx.emit("copySomeGlobalVariablesIntoGlobalFieldDatFileTable()")
      }
      0x38 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("loadSchoolPakFiles(loadMode? = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x3A -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val path = readDatString()
        ctx.emit("opcode163A_linkOrLoadTxbFile?(id? = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), path='$path')")
      }
      0x54 -> {
        val ptr = readInt()
        ctx.emit("opcode1654_linkDialogTextDataToGField(pointerToDatFile = ${ptr.toWHex()})")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** Save file manipulation, clearing parser opcode result */
  private fun parseOpcodeGroup17(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        ctx.emit("opcode1700()")
      }
      0x01 -> {
        ctx.emit("opcode1701_ifmodel()")
      }
      0x04 -> {
        ctx.emit("setup_iesCmd4OnStackAndLoadDAYIntoParsing()")
      }
      0x05 -> {
        ctx.emit("Save::opcode1705()")
      }
      0x0B -> {
        ctx.emit("Save::loadDAYFileBasedOnSavedData()")
      }
      0x0C -> {
        ctx.emit("Save::opcode170C()")
      }
      0x0F -> {
        ctx.emit("Save::opcode170F_canAlsoLoadField022Dat()")
      }
      0x10 -> {
        ctx.emit("Save::opcode1710()")
      }
      0x11 -> {
        ctx.emit("loadNextDAYFileIntoPreviousIesCmd4Parser()")
      }
      0x64 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode1764_updateSomeGlobalValue??(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "a2 = fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }
      0x65 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1765_dayCheckWithSave?(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x66 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode_1766_updateSaveField?(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }
      0x67 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode_1767(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x78 -> {
        ctx.emit("clearField70AndField74InDatParser()")
      }
      0x79 -> {
        ctx.emit("clearField70AndField74InDatParser()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** General game updates (opcodes 0x1-0x5) and 3D model manipulation */
  private fun parseOpcodeGroup18(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x01 -> {
        ctx.emit("proc::updateInputBuffer()")
      }
      0x02 -> {
        ctx.emit("proc::updateAnimations()")
      }
      0x03 -> {
        ctx.emit("proc::updatePositions()")
      }
      0x04 -> {
        ctx.emit("proc::updateCollisions()")
      }
      0x05 -> {
        ctx.emit("proc::render3D()")
      }
      0x12 -> {
        ctx.emit("opcode1812()")
      }
      0x32 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode1832(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }
      0x33 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit("opcode1833_3dModels(${arg1.toWHex()}, ${arg2.toWHex()})")
      }
      0x34 -> {
        ctx.emit("opcode1834()")
      }
      0x38 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode1838_modelHandsManip(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }
      0x3B -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode183B_3dModels(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }

      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup1A(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x0A -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val modelName = readDatString()
        ctx.emit(
          "loadModel_gObj(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}), '$modelName')",
        )
      }
      0x1D -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        val arg4 = readInt()
        val arg5 = readInt()
        val arg6 = readInt()
        ctx.emit(
          "opcode1A1D(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), ${arg1.toWHex()}, " +
            "${arg2.toWHex()}, ${arg3.toWHex()}, ${arg4.toWHex()}, ${arg5.toWHex()}, ${arg6.toWHex()})",
        )
      }
      0x1E -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1A1E_canMemCpy(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x1F -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1A1F_texBank(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x20 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("opcode1A20_texBank(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x21 -> {
        ctx.emit("opcode1A21_dialogsRelated?()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup1E(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x05 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        val arg4 = readInt()
        ctx.emit("opcode1E05(${arg1.toWHex()}, ${arg2.toWHex()}, ${arg3.toWHex()}, ${arg4.toWHex()})")
      }
      0x64 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        val arg4 = readInt()
        val arg5 = readInt()
        val arg6 = readInt()
        val arg7 = readInt()
        ctx.emit(
          "opcode1E64(${arg1.toWHex()}, ${arg2.toWHex()}, ${arg3.toWHex()}, ${arg4.toWHex()}, ${arg5.toWHex()}, " +
            "${arg6.toWHex()}, ${arg7.toWHex()})",
        )
      }
      0x65 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        val arg4 = readInt()
        val arg5 = readInt()
        val arg6 = readInt()
        val arg7 = readInt()
        ctx.emit(
          "opcode1E65(${arg1.toWHex()}, ${arg2.toWHex()}, ${arg3.toWHex()}, ${arg4.toWHex()}, ${arg5.toWHex()}, " +
            "${arg6.toWHex()}, ${arg7.toWHex()})",
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** Loaded PAK files management */
  private fun parseOpcodeGroup1F(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val pakPath = readDatString()
        ctx.emit("loadPAKFile(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), \"$pakPath\")")
      }
      0x01 -> {
        ctx.emit("checkPAKFileLoaded()")
      }
      0x02 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        ctx.emit(
          "opcode1F02_freeHeap?(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}))",
        )
      }
      0x0C -> {
        val pakPath = readDatString()
        ctx.emit("unloadPAKFile?(\"$pakPath\")")
      }
      0x0A -> {
        ctx.emit("CriFS::BeginRoundOrDayDataGroup()")
      }
      0x0B -> {
        ctx.emit("CriFS::EndRoundOrDayDataGroup()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** RenderTask manipulation: setup and texture management */
  private fun parseOpcodeGroup20(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val count = readInt()
        ctx.emit("setupRenderTaskStructs(howMany = ${count.toWHex()})")
      }
      0x03 -> {
        val arg1 = readInt()
        ctx.emit("opcode2003(${arg1.toWHex()})")
      }
      0x06 -> {
        val textureSlot = readInt()
        val texturePath = readDatString()
        ctx.emit("loadTXBFile(${textureSlot.toWHex()}, \"$texturePath\")")
      }
      0x0A -> {
        val structNum = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        ctx.emit(
          "updateRenderTask_activateTexture(structNum = ${structNum.toWHex()}, textureId = ${arg1.toWHex()}, " +
            "someTexRenderMode? = ${arg2.toWHex()}, " +
            "someTexRenderMode2? = ${arg3.toWHex()}) [side: state = state | ACTIVE]",
        )
      }
      0x0B -> {
        val structNum = readInt()
        ctx.emit("initRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}))")
      }
      0x0C -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit(
          "updateRenderTask_activateTexture(structNum = ${structNum.toWHex()}, " +
            "textureId = ${arg1.toWHex()}) [side: state = state | ACTIVE]",
        )
      }
      0x0E -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit("updateRenderTask(structNum = ${structNum.toWHex()}, field_10 = ${arg1.toWHex()}) [side: state = state | ACTIVE]")
      }
      0x0F -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit(
          "updateRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}), " +
            "fieldC = varBlockOrImm(${arg1.toWHex()}))  [side: state = state | ACTIVE]",
        )
      }
      0x11 -> {
        val structNum = readInt()
        val orMode = readInt()
        ctx.emit("updateRenderTask_orMode(structNum = varBlockOrImm(${structNum.toWHex()}), orMode = varBlockOrImm(${orMode.toWHex()}))")
      }
      0x12 -> {
        ctx.emit("opcode2012_updateSomeGlobalVar()")
      }
      0x13 -> {
        val structNum = readInt()
        val texId = readInt()
        ctx.emit(
          "updateRenderTask_activateTexture(structNum = varBlockOrImm(${structNum.toWHex()}), " +
            "newTexId = varBlockOrImm(${texId.toWHex()}))",
        )
      }
      0x14 -> {
        ctx.emit("opcode2014()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** RenderTask manipulation: position managing */
  private fun parseOpcodeGroup21(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val structNum = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit(
          "updateRenderTask_updatePosition(structNum = ${structNum.toWHex()}, xyzSelector = " +
            "${arg1.toWHex()}, value = ${arg2.toWHex()} (as Int: $arg2)) [side: state = state | DIRTY, field_48+offset = field_48+offset OR 0x8000]",
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** RenderTask manipulation: managing animations */
  private fun parseOpcodeGroup24(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit("updateRenderTask(structNum = ${structNum.toWHex()}, animation_fromColor = ${arg1.toWHex()}) [side: state = state | ACTIVE]")
      }
      0x01 -> {
        val structNum = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit(
          "updateRenderTask(structNum = ${structNum.toWHex()}, animation_toColor = ${arg1.toWHex()}, animation_endTime = ${arg2.toWHex()}) " +
            "[side: animation_currentTime? = 0, animation_enable = (animation_enable & 0xFFFFFFF0) OR 0x1, state = state | ACTIVE)",
        )
      }
      0x03 -> {
        val structNum = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit("updateRenderTask_animationRelated(structNum = ${structNum.toWHex()}, arg1 = ${arg1.toWHex()}, arg2 = ${arg2.toWHex()})")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** RenderTask manipulation: managing animations and other properties */
  private fun parseOpcodeGroup26(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x00 -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit("updateRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}), someRenderMode? = varBlockOrImm(${arg1.toWHex()}))")
      }
      0x01 -> {
        val structNum = readInt()
        val arg1 = readInt()
        val ptr = readInt()
        ctx.emit(
          "updateRenderTask(structNum = ${structNum.toWHex()}, someRenderMode? = ${arg1.toWHex()}, " +
            "field_1C_masterPosStruct = ${ptr.toWHex()} [pointerToTableInDATFile?])",
        )
      }
      0x02 -> {
        val structNum = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit("updateRenderTask(structNum = ${structNum.toWHex()}, someRenderMode? = ${arg1.toWHex()}, someWidth = ${arg2.toWHex()})")
      }
      0x03 -> {
        val structNum = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        ctx.emit(
          "updateRenderTask(structNum = ${structNum.toWHex()}, someRenderMode? = ${arg1.toWHex()}, " +
            "someWidth = ${arg2.toWHex()}, someHeight = ${arg3.toWHex()})",
        )
      }
      0x04 -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit("updateRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}), ie2DEngineMotionPtr? = varBlockOrImm(${arg1.toWHex()}))")
      }
      0x06 -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit("updateRenderTask(structNum = ${structNum.toWHex()}, field_2C_(mob_entryId?) = ${arg1.toWHex()})")
      }
      0x0A -> {
        val structNum = readInt()
        val ptr = readInt()
        val textInterp = "[at ${ptr.toWHex()}: ${readDatString(at = ptr, maintainStreamPos = true)}]"
        ctx.addCoverage(ptr..(ptr + textInterp.length))
        ctx.emit("updateRenderTask_set1CToCharacterOrLocationNamePtr(structNum = ${structNum.toWHex()}, ptr = $ptr) $textInterp")
      }
      0x0C -> {
        val structNum = readInt()
        ctx.emit("updateRenderTask_clearField1C(structNum = ${structNum.toWHex()})")
      }
      0x33 -> {
        val structNum = readInt()
        ctx.emit("opcode2633_IesCmd2Struct0x1E0_getFieldCFromIesMotion(structNum = varBlockOrImm(${structNum.toWHex()}))")
      }
      0x36 -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit("updateRenderTask_updateIe2DEngineMotion(structNum = ${structNum.toWHex()}, arg1 = ${arg1.toWHex()})")
      }
      0x38 -> {
        val structNum = readInt()
        val ptr = readInt()
        ctx.emit("updateRenderTask_updateIe2DEngineMotionPtr(structNum = ${structNum.toWHex()}, pointerToDATFile = ${ptr.toWHex()})")
      }
      0x39 -> {
        val ptr = readInt()
        val arg2 = readInt()
        val textInterp = "[at ${ptr.toWHex()}: ${readDatString(at = ptr, maintainStreamPos = true)}]"
        ctx.addCoverage(ptr..(ptr + textInterp.length))
        ctx.emit("opcode2639_pushText(pointerToDATFile = ${ptr.toWHex()}, arg2 = varBlockOrImm(${arg2.toWHex()})) $textInterp")
      }
      0x3A -> {
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit("opcode263A_pushAnim?(arg1 = varBlockOrImm(${arg1.toWHex()}), arg2 = varBlockOrImm(${arg2.toWHex()}))")
      }
      0x40 -> {
        val arg1 = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        ctx.emit(
          "opcode2640(arg1 = varBlockOrImm(${arg1.toWHex()}), arg2 = varBlockOrImm(${arg2.toWHex()}), " +
            "arg3 = varBlockOrImm(${arg3.toWHex()}))",
        )
      }
      0x41 -> {
        val structNum = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        ctx.emit(
          "opcode2641_animRelated(structNum = varBlockOrImm(${structNum.toWHex()}), arg2 = ${arg2.toWHex()}, " +
            "arg3 = ${arg3.toWHex()})",
        )
      }
      0x44 -> {
        val structNum = readInt()
        ctx.emit("opcode2644_pushAnim?0x4(structNum = varBlockOrImm(${structNum.toWHex()}))")
      }
      0x45 -> {
        val structNum = readInt()
        ctx.emit("opcode2645_pushAnim?0x8(structNum = varBlockOrImm(${structNum.toWHex()}))")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  private fun parseOpcodeGroup28(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x02 -> {
        val structNum = readInt()
        val arg2 = readInt()
        val arg3 = readInt()
        ctx.emit(
          "updateRenderTask_linkDATFile(structNum = ${structNum.toWHex()}, datFileId=${arg2.toWHex()}, " +
            "getPointerToDatFileFlag=${arg3.toWHex()}) [side: put 0xA in iesSubStruct.field20]",
        )
      }
      0x05 -> {
        val structNum = readInt()
        val newRenderMode = readInt()
        // getPointerToIesCmd2(callingDat, structNum)->linkedDatParser.iesCmd2Substructs.forEach { it.renderMode = newRenderMode }
        ctx.emit(
          "updateRenderTask_changeNestedDatSubStructs_changeRenderMode(structNum = varBlockOrImm(${structNum.toWHex()}), " +
            "newRenderMode? = varBlockOrImm(${newRenderMode.toWHex()}))",
        )
      }
      0x0E -> {
        val count = readInt()
        ctx.emit("setupRenderTaskStructs(howMany = ${count.toWHex()})")
      }
      0x0F -> {
        val structNum = readInt()
        val name = readDatString()
        ctx.emit("copyStringToExternalVariablesBlock(offset = ${structNum.toWHex()}, source = $name)")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** RenderTask manipulation: managing MOB animations */
  private fun parseOpcodeGroup29(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x02 -> {
        val structNum = readInt()
        val arg2 = readInt()
        ctx.emit("updateRenderTask_setupMOBFile(structNum = varBlockOrImm(${structNum.toWHex()}), varBlockOrImm(${arg2.toWHex()}))")
      }
      0x03 -> {
        val structNum = readInt()
        ctx.emit("opcode2903(structNum? = varBlockOrImm?(${structNum.toWHex()}))")
      }
      0x04 -> {
        val structNum = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit(
          "updateRenderTask_MOBSetupRelated(structNum = varBlockOrImm(${structNum.toWHex()}), varBlockOrImm(${arg1.toWHex()}), " +
            "varBlockOrImm(${arg2.toWHex()}))",
        )
      }
      0x05 -> {
        val structNum = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit(
          "updateRenderTask_MOB_stopMOB?(structNum = varBlockOrImm(${structNum.toWHex()}), varBlockOrImm(${arg1.toWHex()}), " +
            "varBlockOrImm(${arg2.toWHex()}))",
        )
      }
      0x06 -> {
        val structNum = readInt()
        ctx.emit("opcode2906_updateRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}))")
      }
      0x07 -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit("opcode2907_updateRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}), arg1 = ${arg1.toWHex()})")
      }
      0x08 -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit("opcode2908_updateRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}), arg1 = ${arg1.toWHex()})")
      }
      0x09 -> {
        val structNum = readInt()
        val arg1 = readInt()
        ctx.emit("opcode2909_updateRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}), arg1 = ${arg1.toWHex()})")
      }
      0x0B -> {
        val structNum = readInt()
        ctx.emit("opcode290B_updateRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}))")
      }
      0x0C -> {
        val structNum = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit(
          "updateRenderTask(structNum = varBlockOrImm(${structNum.toWHex()}), varBlockOrImm(${arg1.toWHex()}), " +
            "varBlockOrImm(${arg2.toWHex()}))",
        )
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }

  /** The 'native' group */
  private fun parseOpcodeGroup30(input: KioInputStream, opcode: Int): DatParseState = with(input) {
    when (opcode) {
      0x02 -> {
        ctx.emit("native::opcode3002_CSchoolTime_Init?()")
      }
      0x04 -> {
        ctx.emit("native::opcode3004_mobRelated?() [also CSchoolTime?]")
      }
      0x03 -> {
        ctx.emit("native::opcode3003()")
      }
      0x05 -> {
        ctx.emit("native::opcode3005()")
      }
      0x07 -> {
        ctx.emit("native::opcode3007_CSchoolTime_related()")
      }
      0x0C -> {
        ctx.emit("native::opcode300C()")
      }
      0x0A -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        val getMemoryMode2 = readInt()
        val getMemoryArg2 = readInt()
        val ptr = readInt()
        val textInterp = "[at ${ptr.toWHex()}: ${readDatString(at = ptr, maintainStreamPos = true)}]"
        ctx.addCoverage(ptr..(ptr + textInterp.length))
        ctx.emit(
          "native::dialog_pushDialogSelection(index = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()})), " +
            "fetchValue(${getMemoryMode2.toWHex()}, ${getMemoryArg2.toWHex()}), ptrToDATFile = ${ptr.toWHex()}) $textInterp",
        )
      }
      0x0B -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit(
          "native::dialog_prepareDialogResponseSelection(howManyResponses = fetchValue(${getMemoryMode.toWHex()}, " +
            "${getMemoryArg.toWHex()}))",
        )
      }
      0x0D -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("native::dialog_commitDialogSelection?(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x0E -> {
        ctx.emit("native::msgSelect_300E()")
      }
      0x14 -> {
        val ptr = readInt()
        ctx.emit("native::FieldBacklog_setField0xE8AndInit?(pointerToDat = ${ptr.toWHex()})")
      }
      0x15 -> {
        val ptr = readInt()
        val arg1 = readInt()
        val arg2 = readInt()
        ctx.emit("native::FieldBacklog_pushText(ptrToDATFile = ${ptr.toWHex()}, arg1 = ${arg1.toWHex()}, arg2 = ${arg2.toWHex()})")
      }
      0x16 -> {
        ctx.emit("native::FieldBacklog_clearField0xE0")
      }
      0x17 -> {
        ctx.emit("native::DisableFieldBackLog?()")
      }
      0x18 -> {
        ctx.emit("native::FieldBackLog_3018()")
      }
      0x19 -> {
        ctx.emit("native::FieldBackLog_3019()")
      }
      0x28 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("native::opcode3028(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x29 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("native::opcode3029(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x2A -> {
        ctx.emit("native::opcode302A_CCalendarRelated_suspendExecutionUntilAnimationEnd?()")
      }
      0x2B -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("native::opcode302B(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x3C -> {
        ctx.emit("native::initCNameEntry()")
      }
      0x3D -> {
        ctx.emit("native::checkCNameEntryStillPresent()")
      }
      0x96 -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("native::updateNowLoadingScreen(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0xA0 -> {
        ctx.emit("native::loadTitleInterfaceData()")
      }
      0xA1 -> {
        ctx.emit("native::unloadTitleInterfaceData()")
      }
      0xA2 -> {
        ctx.emit("native::checkCTitleInterfaceStillPresent?() [field_70 and field_74 = CTitle.field_54]")
      }
      0xC0 -> {
        ctx.emit("native::createAndSetupCAdvice()")
      }
      0xC1 -> {
        ctx.emit("native::checkCAdviceStillPresent()")
      }
      0xD2 -> {
        ctx.emit("native::createAndSetupCModSelect512OnHeap()")
      }
      0xD3 -> {
        ctx.emit("native::checkCModSelectStillPresent?()")
      }
      0x7A -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("native::opcode307A(fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x8F -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("native::loadTutorialPAKFile(whichOne = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x8C -> {
        val getMemoryMode = readInt()
        val getMemoryArg = readInt()
        ctx.emit("native::createCTutorial(whichOne = fetchValue(${getMemoryMode.toWHex()}, ${getMemoryArg.toWHex()}))")
      }
      0x8D -> {
        ctx.emit("native::waitTutorial?()")
      }
      else -> return DatParseState.StopUnrecognizedOpcode
    }
    return DatParseState.OK
  }
}
