package tl.extra.ccc.patcher.eboot

import kmips.Assembler
import kmips.FpuReg.f12
import kmips.FpuReg.f4
import kmips.FpuReg.f5
import kmips.Label
import kmips.Reg.*
import kmipsx.elf.CompileResult
import kmipsx.elf.ElfPatchSuite
import kmipsx.util.align16
import kmipsx.util.callerSavedRegisters
import kmipsx.util.float
import kmipsx.util.preserve
import kmipsx.util.region
import kmipsx.util.word

internal fun Assembler.includeInfomatrixVars(vars: PatchVars) {
  align16()
  vars.dInfomatrixMtxStRankOffsetTable = virtualPc
  float(99f) // offset for Endurance
  float(64f) // offset for Agility
  float(62f) // offset for Mana
  float(60f) // offset for Luck
  vars.bInfomatrixActive = word(0)
  vars.iInfomatrixCurrentId = word(0)
}

internal fun Assembler.includeExtInfomatrixPatch(funcs: PatchFuncs, vars: PatchVars, compileResult: CompileResult) {
  funcs.infomatrixMtxStRankOffsetApplier = region {
    val ctx = preserve(listOf(t8, t9))
    la(t8, vars.dInfomatrixMtxStRankOffsetTable)
    move(t9, a0)
    sll(t9, t9, 0x2) // multiply by 4
    addu(t8, t8, t9)
    lw(t8, 0x0, t8)
    data(0x44986000) // 	mtc1	t8,f12
    addiu(a0, a0, 1)
    data(0x460C6BC0) // 	add.s	f15,f13,f12
    ctx.restoreAndExit()
  }

  funcs.infomatrixSkillOffsetApplier = region {
    val loadIntoFpu = Label()
    val moveToBottomLine = Label()
    val ctx = preserve(listOf(t7, t8))

    li(t8, 0x2) // compare loop counter, only offset last skill
    bne(t8, s2, loadIntoFpu)
    nop()

    // begin of comparison blocks
    lw(t8, 0x0, s5)
    li(t7, 0x0) // M1
    beq(t7, t8, moveToBottomLine)
    li(t7, 0x1) // M2
    beq(t7, t8, moveToBottomLine)
    li(t7, 0x2) // M3
    beq(t7, t8, moveToBottomLine)
    li(t7, 0x3) // M4
    beq(t7, t8, moveToBottomLine)
    li(t7, 0x5) // M6
    beq(t7, t8, moveToBottomLine)
    li(t7, 0x6) // M7
    beq(t7, t8, moveToBottomLine)
    nop()

    b(loadIntoFpu)
    nop()

    // begin of offset loaders block

    label(moveToBottomLine)
    lui(t7, 0x437E)
    sw(t7, 0x414 + ctx.byteSize(), sp) // Y offset location
    b(loadIntoFpu)
    nop()

    // function exit

    label(loadIntoFpu)
    ctx.restore()
    word(0xC7AD0414) // 	lwc1	f13,0x414(sp)
    ctx.exit()
  }

  funcs.infomatrixOnInit = region {
    sw(zero, 0x1300, s0)
    val ctx = preserve(listOf(s0, s1))
    la(s0, vars.bInfomatrixActive)
    li(s1, 1)
    sw(s1, 0x0, s0)
    ctx.restoreAndExit()
  }

  funcs.infomatrixOnRender = region {
    lui(a1, 0x89B)
    val ctx = preserve(listOf(t0, t1))
    la(t0, vars.iInfomatrixCurrentId)
    lb(t1, 0x1224, s1)
    sw(t1, 0x0, t0)
    ctx.restore()
    j(0x08881240)
    nop()
  }

  funcs.infomatrixOnDispose = region {
    move(t0, zero)
    val ctx = preserve(listOf(s0))
    la(s0, vars.bInfomatrixActive)
    sw(zero, 0x0, s0)
    ctx.restoreAndExit()
  }

  funcs.infomatrixSkill3FixerDispatch = region {
    val ctx = preserve(callerSavedRegisters)
    // a0 here is default offset value
    lw(a1, 0x6B8 + ctx.byteSize(), sp)
    lw(a1, 0x0, a1) // matrix index
    move(a2, s2) // skill loop index
    jal(compileResult.functions.getValue("cccInfomatrixSkill3Fixer"))
    nop()
    move(a0, v0)
    ctx.restore(skipRegs = listOf(a0))
    sw(a0, 0x0, a3)
    jr(ra)
    nop()
  }

  funcs.infomatrixSkill3NewInfoOffsetApplier = region {
    val stdExit = Label()
    val moveToBottomLine = Label()

    lwc1(f12, 0x1D0, sp) // orig code
    val ctx = preserve(listOf(s0, t7))
    lw(s0, 0x6B8 + ctx.byteSize(), sp)
    lw(s0, 0x0, s0) // matrix index

    li(t7, 0x0) // M1
    beq(t7, s0, moveToBottomLine)
    li(t7, 0x1) // M2
    beq(t7, s0, moveToBottomLine)
    li(t7, 0x2) // M3
    beq(t7, s0, moveToBottomLine)
    li(t7, 0x3) // M4
    beq(t7, s0, moveToBottomLine)
    li(t7, 0x5) // M6
    beq(t7, s0, moveToBottomLine)
    li(t7, 0x6) // M7
    beq(t7, s0, moveToBottomLine)
    nop()

    b(stdExit)
    nop()

    label(moveToBottomLine)
    lui(s0, 18.0f.toRawBits() ushr 16)
    mtc1(s0, f5)
    lwc1(f4, 0x1A4 + ctx.byteSize(), sp)
    add.s(f4, f4, f5)
    swc1(f4, 0x1A4 + ctx.byteSize(), sp)

    label(stdExit)
    ctx.restoreAndExit()
  }
}

internal fun ElfPatchSuite.includeInfomatrixPatch(funcs: PatchFuncs) {
  patch("Infomatrix: change line widths") {
    change(0x00080EE8) { li(t0, 0x25) } // for header text
    change(0x000799CC) { li(t0, 0x25) } // for header text in full screen mode
    change(0x000810E8) { li(t0, 0x43) } // for abbreviation text
    change(0x00079AAC) { li(t0, 0x43) } // for full screen text view
  }
  patch("Infomatrix: scissor adjust") {
    change(0x000799E8) { lui(a1, 0x43D7) } // full width box 1 px more
  }
  patch("Infomatrix: offset appliers") {
    change(0x0007B74C) {
      jal(funcs.infomatrixMtxStRankOffsetApplier)
      nop()
    }
    change(0x0007D0F0) {
      jal(funcs.infomatrixSkillOffsetApplier)
    }
  }
  patch("Infomatrix: details element placement") {
    change(0x0007840C) { li(a0, 0x91) } // M1 skill tex 0
    change(0x00078424) { // M1 skill tex 1
      j(funcs.infomatrixSkill3FixerDispatch)
      nop()
    }
    change(0x0007843C) { li(a0, 0) } // M1 skill tex 2

    change(0x00078454) { li(a0, 0xB5) } // M2 skill tex 0
    change(0x00078470) { // M2 skill tex 1
      j(funcs.infomatrixSkill3FixerDispatch)
      nop()
    }
    change(0x00078484) { li(a0, 0) } // M2 skill tex 2

    change(0x0007849C) { li(a0, 0x47) } // M3 skill tex 0
    change(0x000784B8) { // M3 skill tex 1
      j(funcs.infomatrixSkill3FixerDispatch)
      nop()
    }
    change(0x000784CC) { li(a0, 0) } // M3 skill tex 2

    change(0x000784E4) { li(a0, 0x90) } // M4 skill tex 0
    change(0x00078500) { // M4 skill tex 1
      j(funcs.infomatrixSkill3FixerDispatch)
      nop()
    }
    change(0x00078514) { li(a0, 0) } // M4 skill tex 2

    change(0x0007852C) { li(a0, 0xA0) } // M5 skill tex 0
    change(0x00078544) { li(a0, 0x66) } // M5 skill tex 1
    change(0x0007855C) { li(a0, 0) } // M5 skill tex 2

    change(0x00078574) { li(a0, 0x69 + 0xF) } // M6 skill tex 0
    change(0x00078590) { // M6 skill tex 1
      j(funcs.infomatrixSkill3FixerDispatch)
      nop()
    }
    change(0x000785A4) { li(a0, 0) } // M6 skill tex 2

    change(0x000785D4) { li(a0, 0xB3) } // M7 skill tex 0
    change(0x000785F0) { // M7 skill tex 1
      j(funcs.infomatrixSkill3FixerDispatch)
      nop()
    }
    change(0x00078604) { li(a0, 0) } // M7 skill tex 2

    change(0x0007861C) { li(a0, 0x65) } // M8 skill tex 0
    change(0x00078634) { li(a0, 0x6B) } // M8 skill tex 1
    change(0x0007864C) { li(a0, 0) } // M8 skill tex 2

    change(0x00078668) { // M any locked skill tex
      j(funcs.infomatrixSkill3FixerDispatch)
      nop()
    }
  }
  patch("Infomatrix: fix status position of new info") {
    change(0x0007BF80) { lui(a0, 90.0f.toRawBits() ushr 16) } // endurance
    change(0x0007C0C4) { lui(a0, 170.0f.toRawBits() ushr 16) } // agility
    change(0x0007C208) { lui(a0, 234.0f.toRawBits() ushr 16) } // mana
    change(0x0007C34C) { lui(a0, 298.0f.toRawBits() ushr 16) } // luck
  }
  patch("Infomatrix: fix skill 3 position of new info 1") {
    change(0x0007C958) {
      jal(funcs.infomatrixSkill3NewInfoOffsetApplier)
    }
  }
  patch("Infomatrix: fix skill 3 position of new info 2") {
    change(0x0007CAD8) {
      jal(funcs.infomatrixSkill3NewInfoOffsetApplier)
    }
  }
  patch("Infomatrix: On init") {
    change(0x00078858) {
      jal(funcs.infomatrixOnInit)
    }
  }
  patch("Infomatrix: On render") {
    change(0x0007D238) {
      j(funcs.infomatrixOnRender)
    }
  }
  patch("Infomatrix: On dispose") {
    change(0x000789D4) {
      jal(funcs.infomatrixOnDispose)
    }
  }
}
