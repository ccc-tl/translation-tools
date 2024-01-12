package tl.extra.ccc.patcher.eboot

import kio.util.highBits
import kio.util.lowBits
import kmips.Assembler
import kmips.Label
import kmips.Reg.*
import kmipsx.elf.CompileResult
import kmipsx.elf.ElfPatchSuite
import kmipsx.util.align16
import kmipsx.util.float
import kmipsx.util.preserve
import kmipsx.util.region
import kmipsx.util.word
import kmipsx.util.zeroTerminatedString

private const val sgQuestion2Title = "qes2_title"
private const val sgQuestion3Title = "qes3_title"
private const val sgRinDat = "sg_rin.dat"
private const val sgRanDat = "sg_ran.dat"
private const val sgPslDat = "sg_psl.dat"
private const val sgZinDat = "sg_zin.dat"
private const val sgEliDat = "sg_eli.dat"
private const val sgMllDat = "sg_mll.dat"
private const val sgGilDat = "sg_gil.dat"
private const val sgKskDat = "sg_ksk.dat"
private const val sgKiaDat = "sg_kia.dat"
private const val sgNerDat = "sg_ner.dat"
private const val sgTamDat = "sg_tam.dat"
private const val sgEmiDat = "sg_emi.dat"

internal fun Assembler.includeSgVars(vars: PatchVars) {
  vars.sSgQuestion2Title = zeroTerminatedString(sgQuestion2Title)
  vars.sSgQuestion3Title = zeroTerminatedString(sgQuestion3Title)

  vars.sSgCurrentElementOffset = word(0)
  vars.sSgRinDat = zeroTerminatedString(sgRinDat)
  vars.sSgRanDat = zeroTerminatedString(sgRanDat)
  vars.sSgPslDat = zeroTerminatedString(sgPslDat)
  vars.sSgZinDat = zeroTerminatedString(sgZinDat)
  vars.sSgEliDat = zeroTerminatedString(sgEliDat)
  vars.sSgMllDat = zeroTerminatedString(sgMllDat)
  vars.sSgGilDat = zeroTerminatedString(sgGilDat)
  vars.sSgKskDat = zeroTerminatedString(sgKskDat)
  vars.sSgKiaDat = zeroTerminatedString(sgKiaDat)
  vars.sSgNerDat = zeroTerminatedString(sgNerDat)
  vars.sSgTamDat = zeroTerminatedString(sgTamDat)
  vars.sSgEmiDat = zeroTerminatedString(sgEmiDat)

  // begin of SG offset table
  align16()
  vars.dSgOffsetTable = virtualPc
  // sg01_a; sg02_a; sg03_a; sg04_a (often unused)
  float(0f); float(0f); float(0f); float(0f) // fallback table (failed to resolve dat filename)
  float(10f); float(-25f); float(-10f); float(0f) // 1: Rin, sg_rin.dat table
  float(-15f); float(-8f); float(-30f); float(0f) // 2: Rani, sg_ran.dat table
  float(-3f); float(-25f); float(-25f); float(0f) // 3: Passionlip, sg_psl.dat table
  float(-3f); float(-10f); float(-18f); float(0f) // 4: Jinako Crigiri, sg_zin.dat table
  float(-3f); float(0f); float(-2f); float(0f) // 5: Elizabeth, sg_eli.dat table
  float(-24f); float(-15f); float(-10f); float(0f) // 6: Meltlilith, sg_mll.dat table
  float(-7f); float(-7f); float(-3f); float(0f) // 7: Gilgamesh, sg_gil.dat table
  float(0f); float(-22f); float(-20f); float(0f) // 8: BB, sg_ksk.dat table
  float(0f); float(-10f); float(-5f); float(-12f) // 9: Kiara Sessyoin, sg_kia.dat table
  float(-10f); float(7f); float(-22f); float(0f) // 10: Nero, sg_ner.dat table
  float(-17f); float(-27f); float(-20f); float(0f) // 11: Tamamo, sg_tam.dat table
  float(-2f); float(-10f); float(-12f); float(0f) // 12: Archer, sg_emi.dat table
}

internal fun Assembler.includeExtSgPatch(funcs: PatchFuncs, vars: PatchVars, compileResult: CompileResult) {
  funcs.sgLineHeightAndShiftJisSpacingFix = region {
    val ctx = preserve(listOf(t1))
    lui(t1, 0x4170)
    sw(t1, 0x0, a3)
    ctx.restoreAndExit()
  }

  funcs.sgProfileCursorFix = region {
    data(0x44056800) // 	mfc1	a1,f13
    move(a0, s2)

    val exit = Label()
    val secondCompare = Label()
    val ctx = preserve(listOf(a0, a2, a3, t0, t1, s1, s7, ra)) // also preserve registers modified by memcmp
    move(s7, a1)

    lw(a0, 0, s1) // load pointer to currently highlighted text
    beq(a1, zero, exit)
    nop()

    la(a1, vars.sSgQuestion2Title)
    jal(funcs.memcmp)
    li(a2, sgQuestion2Title.length)
    bne(v0, zero, secondCompare)
    nop()
    lui(s7, 0x41a0) // currently active question is number 2 - move cursor
    b(exit)
    nop()

    label(secondCompare)
    lw(a0, 0, s1) // load pointer to currently highlighted text
    la(a1, vars.sSgQuestion3Title)
    jal(funcs.memcmp)
    li(a2, sgQuestion3Title.length)
    bne(v0, zero, exit)
    nop()
    lui(s7, 0x4204) // currently active question is number 3 - move cursor

    label(exit)
    move(a1, s7)
    ctx.restoreAndExit()
  }

  funcs.sgNameResolver = region {
    move(s0, a0)
    addiu(a0, s0, 0x244)
    lw(a0, 0x4, a0)

    val exit = Label()
    val resolveEnd = Label()
    val ctx = preserve(listOf(a0, a2, a3, v0, t0, t1, s0, s7, ra)) // also preserve registers modified by memcmp

    fun compareSg(ptr: Int, len: Int, sgTableIndex: Int) {
      move(a0, s0)
      la(a1, ptr)
      jal(funcs.memcmp)
      li(a2, len)
      beq(v0, zero, resolveEnd)
      li(t1, sgTableIndex)
    }

    // obtain pointer to currently loaded DAT file
    lw(a0, 0x27C + 0x30 + 0x24, sp) // 0x24 will change if context above is changed!
    lw(a0, 0, a0) // some magic
    beq(a0, zero, exit)
    nop()
    lw(a0, 0x4, a0) // get pointer to DatParser
    lw(a0, 0x44, a0) // get pointer to begin DAT file
    addi(a0, a0, -0x33) // subtract 0x33 to get to the SG .dat file name, it will point right after interface/sg/
    move(s0, a0)

    // sg compares
    compareSg(vars.sSgRinDat, sgRinDat.length, 1)
    compareSg(vars.sSgRanDat, sgRanDat.length, 2)
    compareSg(vars.sSgPslDat, sgPslDat.length, 3)
    compareSg(vars.sSgZinDat, sgZinDat.length, 4)
    compareSg(vars.sSgEliDat, sgEliDat.length, 5)
    compareSg(vars.sSgMllDat, sgMllDat.length, 6)
    compareSg(vars.sSgGilDat, sgGilDat.length, 7)
    compareSg(vars.sSgKskDat, sgKskDat.length, 8)
    compareSg(vars.sSgKiaDat, sgKiaDat.length, 9)
    compareSg(vars.sSgNerDat, sgNerDat.length, 10)
    compareSg(vars.sSgTamDat, sgTamDat.length, 11)
    compareSg(vars.sSgEmiDat, sgEmiDat.length, 12)

    li(t1, 0) // failed to resolve, use fallback table

    // get pointer to SG offset table
    label(resolveEnd)
    sll(t1, t1, 0x4) // multiply by 16
    lui(t0, vars.dSgOffsetTable.highBits())
    ori(t0, t0, vars.dSgOffsetTable.lowBits())
    addu(t0, t0, t1) // calculate final pointer int offset table
    // t0 now stores pointer int to offset table

    // find what SG texture is requested and store it in s7
    lw(a0, 0x0, sp) // recover index of requested SG texture (profile, sg_01a etc.)
    beq(a0, zero, exit) // ignore profile, no offset required
    li(s7, 0) // offset 0
    addi(a0, a0, -1) // hacked table starts with sg_01a so use offset
    sll(a0, a0, 0x2) // multiply by 4
    addu(t0, t0, a0)
    // t0 now stores ptr to word of the offset that should be applied
    lw(s7, 0x0, t0) // load offset to be applied

    label(exit)
    // store offset in global var
    lui(t0, vars.sSgCurrentElementOffset.highBits())
    ori(t0, t0, vars.sSgCurrentElementOffset.lowBits())
    sw(s7, 0x0, t0)
    ctx.restoreAndExit()
  }

  funcs.sgOffsetApplier = region {
    val ctx = preserve(listOf(t0))
    lui(t0, vars.sSgCurrentElementOffset.highBits())
    ori(t0, t0, vars.sSgCurrentElementOffset.lowBits())
    data(0xC50D0000.toInt()) // lwc1 f13,0x0(t0) //load offset into f13 register
    data(0x460D6300) // add.s f12, f12, f13
    data(0x44056000) // mfc1 a1,f12
    addi(sp, sp, 4)
    data(0xC7AD0004.toInt()) // lwc1 f13,0x4(sp) //recover f13
    addi(sp, sp, -4)
    ctx.restoreAndExit()
  }

  funcs.sgProfileSubsTextAlphaUpdate = region {
    lw(a0, 0x248, sp)
    val ctx = preserve(listOf(s0, a0))
    la(s0, compileResult.variables.getValue("extSgSubsTextAlpha"))
    srl(a0, a0, 24)
    sb(a0, 0x0, s0)
    ctx.restoreAndExit()
  }
}

internal fun ElfPatchSuite.includeSgPatch(funcs: PatchFuncs) {
  patch("SgLineWidth") {
    change(0x0009C450) { li(t0, 0x43) }
    change(0x0009C564) { li(t0, 0x43) }
    change(0x0009C784) { li(t0, 0x43) }
    change(0x0009C670) { li(t0, 0x43) }
  }
  patch("SgLineHeightShiftJisSpacingFix") {
    change(0x0009C324) {
      lui(a1, 0x41D8) // 27 as float
      data(0x44856000) // 	mtc1	a1,f12
    }
    change(0x0009C334) {
      jal(funcs.sgLineHeightAndShiftJisSpacingFix)
      // no nop here - original instruction is correct
    }
  }
  patch("SgProfileCursorFix") {
    change(0x0009C180) {
      jal(funcs.sgProfileCursorFix)
      nop()
    }
  }
  patch("SgNameAndOffsetResolver") {
    change(0x0009AB6C) {
      sw(ra, 0x28, sp)
      jal(funcs.sgNameResolver)
      nop()
    }
    change(0x0009AB88) {
      nop() // nop old ra store
    }
  }
  patch("SgOffsetApplier") {
    change(0x0009AC7C) {
      jal(funcs.sgOffsetApplier)
    }
  }
  patch("SgProfileSubsTextAlphaUpdate") {
    change(0x0009BF58) {
      jal(funcs.sgProfileSubsTextAlphaUpdate)
    }
  }
}
