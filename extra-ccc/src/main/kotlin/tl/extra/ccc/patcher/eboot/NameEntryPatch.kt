package tl.extra.ccc.patcher.eboot

import kio.util.highBits
import kio.util.lowBits
import kmips.Assembler
import kmips.Label
import kmips.Reg.*
import kmipsx.elf.ElfPatchSuite
import kmipsx.util.preserve
import kmipsx.util.region
import kmipsx.util.zeroTerminatedString

internal fun Assembler.includeExtNamePatch(funcs: PatchFuncs, vars: PatchVars) {
  funcs.nameEntryCurrentRowCompare = region {
    val notCurrentRow = Label()
    val ctx = preserve(listOf(s0, s1))
    li(s1, 1)
    bne(a2, a1, notCurrentRow)
    nop()

    la(s0, vars.bNameEntryIsCurrentRow)
    sw(s1, 0x0, s0)
    ctx.restoreAndExit()

    label(notCurrentRow)
    la(s0, vars.bNameEntryIsCurrentRow)
    sw(zero, 0x0, s0)
    ctx.restoreAndExit()
  }

  funcs.nameEntryIsActiveRowColorChange = region {
    val notCurrentRow = Label()

    la(t0, vars.bNameEntryIsCurrentRow)
    lw(t0, 0x0, t0)
    beq(t0, zero, notCurrentRow)
    nop()

    // current row
    sb(fp, 0x54, sp)
    sb(s7, 0x55, sp)
    sb(s6, 0x56, sp)
    jr(ra)
    nop()

    label(notCurrentRow)
    sb(s2, 0x54, sp)
    sb(s2, 0x55, sp)
    sb(s2, 0x56, sp)
    jr(ra)
    nop()
  }

  funcs.nameEntryCursorMovingDownSkipEmpty = region {
    val resumeStdCursorChange = Label()
    val ctx = preserve(listOf(t1))

    lw(t1, 0x310, s1) // load X pos
    bne(t1, zero, resumeStdCursorChange) // X cursor is on keyboard do nothing
    nop()

    li(t1, 1)
    beq(a0, t1, resumeStdCursorChange)
    li(a0, 0x3)

    addiu(a0, s7, 1) // cursor doesn't need change, load value that would be used normally

    label(resumeStdCursorChange)
    sw(a0, 0x314, s1)
    slti(a0, a0, 0x5)
    ctx.restoreAndExit()
  }

  funcs.nameEntryCursorMovingUpSkipEmpty = region {
    val defBranch = Label()
    val resumeStdCursorChange = Label()
    val changeCursor = Label()

    val ctx = preserve(listOf(t1))

    lw(t1, 0x310, s1) // load X pos
    bne(t1, zero, resumeStdCursorChange) // X cursor is on keyboard do nothing
    nop()

    li(t1, 2)
    beq(a0, t1, changeCursor)
    nop()

    b(resumeStdCursorChange)
    nop()

    label(changeCursor)
    li(a0, 0)
    b(resumeStdCursorChange)
    nop()

    label(resumeStdCursorChange)
    ctx.restore()

    bgez(a0, defBranch)
    sw(a0, 0x314, s1)

    // defNotBranch
    j(0x08892C10)
    nop()
    label(defBranch)
    j(0x08892C20)
    nop()
  }

  funcs.nameEntryCursorMovingFromKeyboardLeftToTabPane = region {
    val defBranch = Label()
    val resumeStdCursorChange = Label()
    val fixCursorDown = Label()

    val ctx = preserve(listOf(t1, t2))

    bne(a0, zero, resumeStdCursorChange) // not switching from keyboard to tab, nothing to do
    nop()

    lw(t1, 0x314, s1)
    li(t2, 0x1)
    beq(t1, t2, fixCursorDown)
    nop()
    li(t2, 0x2)
    beq(t1, t2, fixCursorDown)
    nop()

    b(resumeStdCursorChange)
    nop()

    label(fixCursorDown)
    li(t2, 0x3)
    sw(t2, 0x314, s1)

    label(resumeStdCursorChange)
    ctx.restore()

    bgez(a0, defBranch)
    sw(a0, 0x310, s1)

    // defNotBranch
    j(0x088929CC)
    nop()
    label(defBranch)
    j(0x088929DC)
    nop()
  }

  funcs.nameEntryCursorMovingFromKeyboardRightToTabPane = region {
    val resumeStdCursorChange = Label()
    val fixCursorDown = Label()
    val ctx = preserve(listOf(t1, t2))

    li(t1, 0x15)
    bne(t1, a0, resumeStdCursorChange) // no adjustment needed
    nop()

    lw(t1, 0x314, s1)
    li(t2, 0x1)
    beq(t1, t2, fixCursorDown)
    nop()
    li(t2, 0x2)
    beq(t1, t2, fixCursorDown)
    nop()

    b(resumeStdCursorChange)
    nop()

    label(fixCursorDown)
    li(t2, 0x3)
    sw(t2, 0x314, s1)

    label(resumeStdCursorChange)
    sw(a0, 0x310, s1)
    slti(a0, a0, 0x15)
    ctx.restoreAndExit()
  }

  funcs.nameEntryLastNameCursorPosFix = region {
    lui(a1, 0x40E0)
    data(0x44846000) // mtc1    a0, f12
    data(0x46806320) // 	cvt.s.w	f12,f12
    data(0xE61402F4.toInt()) // swc1	f20,0x2F4(s0)
    jr(ra)
    nop()
  }
}

internal fun ElfPatchSuite.includeNamePatch(funcs: PatchFuncs, vars: PatchVars) {
  val neMaxCharCount = 11

  patch("NameEntryInit") {
    // init v2 - rewritten name init loop - copy new blank name template from var store
    // Reference: nameEntry_initInternal
    change(0x0008CFB0) { lui(s2, vars.address.highBits()) }
    change(0x0008CFB4) {
      ori(a1, s2, vars.sNameEntryInitialValue.lowBits())
      move(a0, s1)
      jal(funcs.memcpy)
      li(a2, neMaxCharCount)
      ori(a1, s2, vars.sNameEntryInitialValue.lowBits())
      move(a0, s3)
      jal(funcs.memcpy)
      li(a2, neMaxCharCount)
      ori(a1, s2, vars.sNameEntryInitialValue.lowBits())
      move(a0, s4)
      jal(funcs.memcpy)
      li(a2, neMaxCharCount)
      nop()
      nop()
      nop()
      nop()
      nop()
      nop()
    }
  }

  patch("NameEntryInput") {
    // first name - write single character only
    // Reference: nameEntry_writeFirstNameCharacter
    change(0x0008BDE0) { li(a1, neMaxCharCount) } // increase max char count
    change(0x0008BDEC) { slti(a0, a0, neMaxCharCount + 1) }
    change(0x0008BE98) { move(a1, a2) } // shift-jis fix for wide characters - do NOT multiply by char count
    change(0x0008BEAC) { nop() } // do NOT store second byte

    // last name - write single character only
    // Reference: nameEntry_writeLastNameCharacter
    change(0x0008BF10) { slti(t3, a1, neMaxCharCount) } // increase max char count
    change(0x0008C034) { nop() } // shift-jis fix for wide characters - do NOT multiply by char count
    change(0x0008C048) { nop() } // do NOT store second byte
    change(0x0008BF9C) {
      sb(t1, 0x34, a0) // indefinitely replace last character when cursor is at the end
      nop() // do NOT modify second byte
    }

    // nickname - write single character only
    // Reference: nameEntry_writeNickNameCharacter
    change(0x0008C0AC) { slti(t3, a1, neMaxCharCount) } // increase max char count
    change(0x0008C1D0) { nop() } // shift-jis fix for wide characters - do NOT multiply by char count
    change(0x0008C1E4) { nop() } // do NOT store second byte
    change(0x0008C138) {
      sb(t1, 0x4C, a0) // indefinitely replace last character when cursor is at the end
      nop() // do NOT modify second byte
    }
  }

  patch("NameEntryDeletion") {
    // first name
    change(0x0008C234) { la(a1, vars.sSingleSpace) } // use single space for clear character
    change(0x0008C2AC) { nop() } // shift-jis fix for wide characters
    change(0x0008C2C4) { li(a2, 0x1) } // copy only single character

    // last name
    change(0x0008C33C) { lui(s1, vars.address.highBits()) }
    change(0x0008C34C) { ori(a1, s1, vars.sSingleSpace.lowBits()) }
    change(0x0008C3D8) { nop() } // shift-jis fix for wide characters
    change(0x0008C3F0) { li(a2, 1) } // copy only one character

    change(0x0008C418) {
      val stdExit = Label()
      stdExit.address = 0x08890464

      li(a1, neMaxCharCount - 1)
      bne(a0, a1, stdExit)
      nop()
      sb(zero, 0xF, sp)
      j(0x08890458) // back up cursor one more
      sb(zero, 0xE, sp) // maybe fix: this ideally should store 0x20 - space
    }

    // remove character - nickname
    change(0x0008C4B0) { lui(s1, vars.address.highBits()) }
    change(0x0008C4BC) { ori(a1, s1, vars.sSingleSpace.lowBits()) }
    change(0x0008C530) { nop() } // shift-jis fix for wide characters
    change(0x0008C548) { li(a2, 1) } // copy only one character

    change(0x0008C564) {
      val stdExit = Label()
      stdExit.address = 0x088905B0

      li(a1, neMaxCharCount - 1)
      bne(a0, a1, stdExit)
      nop()
      sb(zero, 0x27, sp)
      j(0x088905A4) // back up cursor one more
      sb(zero, 0x26, sp) // maybe fix: this ideally should store 0x20 - space
    }
  }

  patch("NameEntryAutoFill") {
    // Reference: EBOOT constant section
    change(0x001B447C) { zeroTerminatedString("Kishinami  ") } // last name
    change(0x001B4488) { zeroTerminatedString("Hakuno     ") } // first name
    change(0x001B4494) { zeroTerminatedString("Kishinami  ") } // male nickname
    change(0x001B44A0) { zeroTerminatedString("Hakunon    ") } // female nickname
  }

  patch("NameEntryNameCursor") {
    // fix manual cursor move for first name
    // Reference: nameEntry_moveFirstNameEntryCursor
    change(0x0008C5E8) { li(a1, neMaxCharCount) }
    change(0x0008C5F8) { slti(a0, a0, neMaxCharCount + 1) }

    // fix manual cursor move for last name
    // Reference: nameEntry_lastNameManualCursorMove
    change(0x0008C6D8) { slti(a0, a0, neMaxCharCount) }

    // fix manual cursor move for nickname
    // Reference: nameEntry_nickNameManualCursorMove
    change(0x0008C7B0) { slti(a0, a0, neMaxCharCount) }

    // first name - cursor pos fix
    change(0x0008D9E8) { lui(a1, 0x40E0) } // cursor location calculation for first name, as float: 7.0

    // names spacing fix
    change(0x0008B7F4) { lui(a0, 0x42AA) } // increase spacing between names to 85.0

    // last name - cursor pos fix
    change(0x0008DA08) {
      slti(a1, a0, neMaxCharCount)
      data(0x50A00001) // 	beql	a1,zero,pos_08891A14
      li(a0, neMaxCharCount - 1)

      jal(funcs.nameEntryLastNameCursorPosFix)
      nop()
      data(0x44856800) // mtc1	a1,f13
      data(0x460D6302) // 	mul.s	f12,f12,f13
      lui(a1, 0x42AA)
      data(0x44856800) // mtc1	a1,f13
      data(0x460D6300) // add.s	f12,f12,f13
      data(0xE60C02F0.toInt()) // swc1	f12,0x2F0(s0)
    }

    // nickname - cursor pos fix
    change(0x0008DE4C) { slti(a1, a0, neMaxCharCount) } // increase cursor pos limit
    change(0x0008DE54) { li(a0, neMaxCharCount - 1) }
    change(0x0008DE64) { lui(a0, 0x40E0) } // cursor location calculation for nickname, as float: 7.0
  }

  patch("NameEntryTabHack") {
    // force ascii tab to be active at the start
    // Reference: nameEntry_initInternal
    change(0x0008CCF4) { li(a1, 0x2) }
    change(0x0008CCF8) { sw(a1, 0x33C, s6) } // move setting active tab id to the top so a1 reg value can't be used
    change(0x0008CD24) { sw(zero, 0x318, s6) } // move instruction replaced by above change to the bottom

    // cursor fixes
    // Reference: nameEntry_cursorPositionUpdate
    change(0x0008E560) { j(0x08892810) } // do not allow to change active tab and move tab_add texture
    // cheat check that active tab is set to 0, this makes tab_add go away when cursor is moved on ascii tab
    // and fixes tab_add disappearing when cursor is on real tab 2
    change(0x0008ED18) { li(a1, 0x0) }
  }

  patch("NameEntryKeyboardRenderPatch") {
    change(0x0008F6E0) { jal(funcs.nameEntryCurrentRowCompare) }
    change(0x0008F748) {
      jal(funcs.nameEntryIsActiveRowColorChange)
      nop()
      nop()
    }
  }

  patch("NameEntryKeyCursorMovement") {
    // cursor moving down on tab section - skip empty tabs patch
    change(0x0008EB4C) {
      jal(funcs.nameEntryCursorMovingDownSkipEmpty)
      nop()
    }

    // cursor moving up on tab section - skip empty tabs patch
    change(0x0008EC08) {
      j(funcs.nameEntryCursorMovingUpSkipEmpty)
      nop()
    }

    // cursor moving from keyboard left side section to tab pane - make sure it won't end up on empty tabs
    change(0x0008E9C4) {
      j(funcs.nameEntryCursorMovingFromKeyboardLeftToTabPane)
      nop()
    }

    // cursor moving from keyboard right side section to tab pane - make sure it won't end up on empty tabs
    change(0x0008E998) {
      jal(funcs.nameEntryCursorMovingFromKeyboardRightToTabPane)
      nop()
    }
  }

  patch("NameEntryEmptyCheck") {
    change(0x0008B678) { la(a1, vars.sNameEntryInitialValue) }
    change(0x0008B68C) { li(a2, neMaxCharCount) }

    change(0x0008B6A8) { la(a1, vars.sNameEntryInitialValue) }
    change(0x0008B6BC) { li(a2, neMaxCharCount) }

    change(0x0008B6D8) { la(a1, vars.sNameEntryInitialValue) }
    change(0x0008B6EC) { li(a2, neMaxCharCount) }
  }

  patch("NameEntryTrim") {
    // first name
    change(0x0008E34C) { addiu(s3, v0, -1) }
    change(0x0008E364) { lui(fp, vars.address.highBits()) }
    change(0x0008E368) { ori(a1, fp, vars.sSingleSpace.lowBits()) }
    change(0x0008E374) { li(a2, 1) }
    change(0x0008E384) { nop() }
    change(0x0008E388) { addiu(s3, s3, -1) }
    change(0x0008E38C) { addiu(s6, s6, -1) }
    change(0x0008E394) { addiu(s7, s7, -1) }

    // last name
    change(0x0008E3A0) { addiu(s3, v0, -1) }
    change(0x0008E3B0) { lui(fp, vars.address.highBits()) }
    change(0x0008E3B4) { ori(a1, fp, vars.sSingleSpace.lowBits()) }
    change(0x0008E3C0) { li(a2, 1) }
    change(0x0008E3D0) { nop() }
    change(0x0008E3D4) { addiu(s3, s3, -1) }
    change(0x0008E3D8) { addiu(s6, s6, -1) }
    change(0x0008E3E0) { addiu(s7, s7, -1) }

    // nickname
    change(0x0008E3EC) { addiu(s6, v0, -1) }
    change(0x0008E400) { lui(s3, vars.address.highBits()) }
    change(0x0008E404) { ori(a1, s3, vars.sSingleSpace.lowBits()) }
    change(0x0008E410) { li(a2, 1) }
    change(0x0008E420) { nop() }
    change(0x0008E424) { addiu(s6, s6, -1) }
    change(0x0008E428) { addiu(s7, s7, -1) }
    change(0x0008E430) { addiu(fp, fp, -1) }
  }
}
