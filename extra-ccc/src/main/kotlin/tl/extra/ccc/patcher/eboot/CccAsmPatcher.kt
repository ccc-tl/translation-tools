package tl.extra.ccc.patcher.eboot

import kio.util.child
import kio.util.toHex
import kio.util.toWHex
import kmips.FpuReg.f0
import kmips.FpuReg.f12
import kmips.Label
import kmips.Reg.*
import kmips.assembleAsByteArray
import kmipsx.elf.CompileResult
import kmipsx.elf.ElfPatchSuite
import kmipsx.elf.pspCodeCompiler
import kmipsx.elf.writeElfSectionsInto
import kmipsx.util.align16
import kmipsx.util.callerSavedRegisters
import kmipsx.util.preserve
import kmipsx.util.region
import kmipsx.util.word
import kmipsx.util.zeroTerminatedString
import tl.extra.ccc.patcher.ItemParam01Remapper
import tl.extra.ccc.patcher.ItemParam04Remapper
import tl.extra.file.PakFile
import tl.extra.file.replaceEntry
import tl.extra.file.sizeInGame
import java.io.File
import java.time.LocalDateTime

class CccAsmPatcher(private val publicBuild: Boolean = false, private val nativeDir: File, pspSdkDir: File) {
  private val nativeCodeCompiler = pspCodeCompiler(pspSdkDir)
  private val nativeSrcFiles = listOf(
    "bb.cpp", "infomatrix.cpp", "patch.cpp", "generated.cpp", "subs.cpp", "subs_draw.cpp", "hook.cpp", "game.cpp", "platform.cpp", "util.cpp",
  )
  private var vars = PatchVars()
  private var funcs = PatchFuncs()

  val bootPatches = ElfPatchSuite(baseAddress = 0x8804000, useRelativeChangeAddress = true)
  lateinit var auxPatchBytes: ByteArray
  lateinit var compileResult: CompileResult

  fun assemble(
    preloadPak: PakFile,
    pakReplacements: Map<String, File>,
    remapper01: ItemParam01Remapper,
    remapper04: ItemParam04Remapper,
    credits: String,
  ) {
    val baseLoadAddress = 0x08D5F0F0
    val pakEntries = preloadPak.entries

    println("First patcher run...") // first phase text remapping is only needed for files that replaces PRELOAD.pak files
    val phase1RelocationResult01 = remapper01.run(0)
    remapper04.run(phase1RelocationResult01.relocationEndAddress)

    pakEntries.forEach {
      val replacement = pakReplacements[it.path] ?: return@forEach
      println("Patched file replaces PRELOAD.pak entry ${it.path}")
      pakEntries.replaceEntry(it.path, replacement.readBytes())
    }

    val debugDumpPreloadContent = false
    if (debugDumpPreloadContent) {
      var fileAddress = baseLoadAddress
      pakEntries.forEach {
        println("${it.path} ${fileAddress.toWHex()}")
        fileAddress += 0x40 + it.bytes.size
      }
      println("Next file at ${fileAddress.toWHex()}")
    }

    val preloadInjectStart = baseLoadAddress + pakEntries.sizeInGame()

    println("Calculated that patch will be placed at ${preloadInjectStart.toWHex()}")

    println("Second patcher run...")
    val patchStart = preloadInjectStart + 0x40
    val patchBytes = mutableListOf<Byte>()
    val virtualAddr: (Int) -> Int = { patchStart + it }
    println("Code will start at ${patchStart.toWHex()}")
    println("Compile native code...")
    compileResult = nativeCodeCompiler.compile(
      patchBaseAddress = patchStart,
      outDir = nativeDir.child("out"),
      srcFiles = nativeSrcFiles.map { nativeDir.child(it) },
      additionalGccFlags = listOf("-D", "MODE_CCC=1"),
    )
    writeElfSectionsInto(patchBytes, compileResult, virtualAddr)

    println("ASM patch will start at ${virtualAddr(patchBytes.size).toHex()}, overhead: ${patchBytes.size} bytes")
    val cccPatch = createPreloadCodePatch(virtualAddr(patchBytes.size), credits)
    patchBytes.addAll(cccPatch.toList())

    println("Text patch will start at ${virtualAddr(patchBytes.size).toHex()}, overhead: ${patchBytes.size} bytes")
    val relocationResult01 = remapper01.run(virtualAddr(patchBytes.size))
    val relocationResult04 = remapper04.run(relocationResult01.relocationEndAddress)
    patchBytes.addAll(relocationResult01.relocationBytes.toList())
    println("Added remapper 01 result, overhead: ${patchBytes.size} bytes")
    patchBytes.addAll(relocationResult04.relocationBytes.toList())
    println("Added remapper 04 result, overhead: ${patchBytes.size} bytes")

    auxPatchBytes = patchBytes.toByteArray()
    println("Aux patch total overhead: ${auxPatchBytes.size} bytes")
    createBootPatches()
  }

  private fun createPreloadCodePatch(patchStart: Int, credits: String) = assembleAsByteArray(patchStart) {
    run {
      vars.address = virtualPc

      fun writeCreditString(text: String) {
        zeroTerminatedString(text)
        align16()
      }

      align16()
      writeCreditString("~ CCC Translation Team ~")
      writeCreditString(credits)
      writeCreditString("Build timestamp: ${LocalDateTime.now().withNano(0)}")

      vars.sDoubleSpace = word(0x2020)
      vars.sSingleSpace = word(0x20)
      vars.bNameEntryIsCurrentRow = word(0)

      vars.sNameEntryInitialValue = word(0x20202020)
      word(0x20202020)
      word(0x20202020)
      word(0x00202020)

      vars.sHakuno = zeroTerminatedString("Hakuno")
      vars.sKishinami = zeroTerminatedString("Kishinami")
      vars.sHakunon = zeroTerminatedString("Hakunon")

      includeSgVars(vars)
      align16()
      includeInfomatrixVars(vars)
      align16()
      nop()
    }

    funcs.textRemapperBase = region {
      val ctx = preserve(callerSavedRegisters)
      jal(compileResult.functions.getValue("textRemapper"))
      nop()
      ctx.restoreAndExit(skipRegs = listOf(v0))
    }

    funcs.mainTextRemapper = region {
      sw(s0, 0x80, sp)
      sw(s6, 0x98, sp)

      val ctx = preserve(listOf(ra, v0, a0))
      move(a0, t2)
      jal(funcs.textRemapperBase)
      nop()
      move(t2, v0)
      ctx.restoreAndExit()
    }

    funcs.secondaryTextRemapper = region {
      sw(s6, 0x48, sp)
      sw(s7, 0x4C, sp)

      val ctx = preserve(listOf(ra, v0, a0))

      jal(funcs.textRemapperBase)
      move(a0, a2)

      move(a2, v0)

      ctx.restoreAndExit()
    }

    funcs.itemTITM0001TextRemapperTextParserCaller = region {
      val ctx = preserve(listOf(ra))
      jal(funcs.textRemapperBase)
      move(a0, v0)
      move(a0, v0)
      ctx.restoreAndExit()
    }

    funcs.popupDialogMultilineCenterCalculationFix = region {
      val stdExit = Label()
      val ctx = preserve(listOf(a0, a1, a2, ra, s0))
      jal(funcs.textLengthMeasure) // measure first line
      nop()
      ctx.restore(skipRegs = listOf(ra, s0), adjustSp = false)
      move(s0, v0)
      jal(funcs.textLengthMeasure) // measure second line
      li(a2, 0x1)
      // s0 - first line len, v0 - second line len
      bge(v0, s0, stdExit)
      nop()
      move(v0, s0)
      label(stdExit)
      ctx.restoreAndExit()
    }

    funcs.itemTITM0001TextRemapperTextRenderCaller = region {
      val ctx = preserve(listOf(ra))
      data(0x0E20115E) // jal     getItemTextFromTITMrelated_sub_578
      move(a0, s5)
      jal(funcs.textRemapperBase)
      move(a0, v0)
      ctx.restoreAndExit()
    }

    includeExtNamePatch(funcs, vars)

    funcs.scrollableTextFieldHeightCalcFix = region {
      li(t1, 0)
      li(t3, 0)
      li(a3, 0x43) // line width for scrollable text field

      val ctx = preserve(listOf(ra, v0))
      // text pointer is in the a0 already!
      jal(funcs.textRemapperBase)
      nop()
      move(a0, v0)
      ctx.restore()

      jr(ra)
      nop()
    }

    includeExtSgPatch(funcs, vars, compileResult)

    funcs.fixDialogLangAndButtonSwap = region {
      val ctx = preserve(listOf(t0))
      li(t0, 0x1)
      sw(t0, 4, a1)
      sw(t0, 8, a1)
      ctx.restore()
      j(0x089147D4)
      nop()
    }

    funcs.crossCircleButtonSwap = region {
      val ctx = preserve(listOf(ra, s7, t0, t3))
      val exit = Label()
      move(s7, a0)
      jal(0x089A692C) // sceCtrlReadBufferPositive
      nop()
      li(t0, 0x6000)
      lw(t3, 0x4, s7) // load button bit mask
      and(t3, t3, t0) // only use CIRCLE and CROSS bit fields
      beq(t3, zero, exit) // if no CIRCLE or CROSS buttons are pressed then do nothing
      nop()
      beq(t3, t0, exit) // if both buttons are pressed do nothing
      nop()
      // swap buttons
      lw(t3, 0x4, s7) // load button bit mask
      xor(t3, t3, t0)
      sw(t3, 0x4, s7) // store new bit mask
      label(exit)
      ctx.restoreAndExit()
    }

    includeExtInfomatrixPatch(funcs, vars, compileResult)
    includeExtSubsPatch(funcs, compileResult)

    funcs.installIgnoreInstallOptions = region {
      val exit = Label()
      val movingUp = Label()
      val ctx = preserve(listOf(ra, s4, s5, s6))
      jal(0x088BACA4) // jump to original function
      lw(s4, 0x890, s0) // store old value
      lw(s6, 0x890, s0) // get new value
      li(s5, 0x6)
      bne(s5, s6, exit) // new value is not 6 nothing to do
      nop()

      slt(s4, s4, s6) // set if old value < new value
      beq(s4, zero, movingUp)
      nop()

      // moving down
      li(s6, 0x7)
      sw(s6, 0x890, s0)
      b(exit)
      nop()

      label(movingUp)
      li(s6, 0x5)
      sw(s6, 0x890, s0)

      label(exit)
      ctx.restoreAndExit()
    }

    funcs.conditionalDlRender = region {
      val ctx = preserve(listOf(s0))
      val normalRender = Label()
      la(s0, vars.bInfomatrixActive)
      lw(s0, 0x0, s0)
      beq(s0, zero, normalRender)
      nop()
      la(s0, vars.iInfomatrixCurrentId)
      lw(s0, 0x0, s0)
      beq(s0, zero, normalRender)
      nop()
      ctx.restoreAndExit()

      label(normalRender)
      ctx.restore()
      j(0x088E3760)
      nop()
    }

    funcs.rubySpacingController = region {
      val stdExit = Label()
      val ctx = preserve(listOf(s0))
      beq(v0, zero, stdExit)
      nop()

      la(s0, vars.bInfomatrixActive)
      lw(s0, 0x0, s0)
      li(t2, 0x5)
      beq(s0, zero, stdExit)
      nop()
      li(t2, 0x4)

      label(stdExit)
      ctx.restore()
      j(0x088EC4DC)
      nop()
    }

    funcs.initMemoryFix = region {
      lui(a1, 0x89B)
      val ctx = preserve(listOf(s0))
      la(s0, 0x08a27468)
      lw(s0, 0x0, s0)
      sw(zero, 0x39C, s0)
      ctx.restoreAndExit()
    }
  }

  private fun createBootPatches() {
    with(bootPatches) {
      patch("BacklogTextXPosition") {
        change(0x00154C30) { li(s2, 0x76) }
      }
      patch("BacklogSpacing", enabled = false) {
        change(0x00154D48) { li(a0, 0x7) }
      }
      patch("ChoiceSpacing", enabled = false) {
        change(0x000C8D24) { li(a0, 0x7) }
      }
      patch("DefaultLineWidth") {
        change(0x000B44F4) { li(t1, 0x3B) }
      }

      includeInfomatrixPatch(funcs)

      patch("DialogBoxLineWidth") {
        // change(0x0011AC5C) { li(a1, 0x50) } // dat override, doesn't seem to do anything
        change(0x001382D0) { li(t1, 0x50) } // text box
        change(0x00138398) { li(t1, 0x50) } // text box (second load, used at least for BB drop shadow)
        change(0x00154C88) { li(t1, 0x50) } // backlog
      }
      patch("AsciiPatch") {
        change(0x000E9BD0) { li(a0, 0x7) }
        change(0x000E9BE0) { li(a0, 0x7) }
      }
      patch("AsciiSpacing") {
        change(0x000E96A4) { j(0x88ED6B4) } // always force to use fixed spacing
        change(0x000E96B4) { li(a1, 0x7) } // set spacing
      }
      patch("TextCenteringGlyphWidth") {
        // used for at least BB channel
        change(0x000E8BC8) { li(v0, 0x7) }
        change(0x000E95E4) { li(v0, 0x7) }
      }
      patch("MainTextRemapper") {
        change(0x000E7C34) {
          sw(ra, 0xA4, sp)
          jal(funcs.mainTextRemapper)
        }
        change(0x000E7C5C) { nop() } // nop old ra store
      }
      patch("FullScreenDialogBoxArrowCalculation") {
        // Reference: opcode2640_drawingFullScreenArrowRel
        change(0x00137AD0) {
          li(a1, 0x7)
        }
        change(0x000E6A74) {
          // instead of using 0x7 for both ASCII and Shift-JIS, this sets Shift-JIS to use 0xF while using 0x7 for ASCII.
          // This changes function which is shared and may affect some other things
          li(s7, 0xF)
        }
      }
      patch("SecondaryTextRemapper") {
        change(0x000E3C60) {
          sw(ra, 0x54, sp)
          jal(funcs.secondaryTextRemapper)
        }
        change(0x000E3C98) { nop() } // nop old ra store
      }
      patch("ItemTITM0001TextRemapperTextParserCaller") {
        change(0x000E73A4) {
          jal(funcs.itemTITM0001TextRemapperTextParserCaller)
          lw(fp, 0xE4, sp)
        }
      }
      patch("ItemTITM0001TextRemapperTextRenderCaller") {
        change(0x000E6368) {
          jal(funcs.itemTITM0001TextRemapperTextRenderCaller)
          nop()
        }
      }
      patch("PopupDialogMultilineCenterCalculationFix") {
        change(0x00070ABC) {
          jal(funcs.popupDialogMultilineCenterCalculationFix)
        }
      }
      patch("DnsDecrypted") {
        change(0x00107144) {
          li(v0, 0)
          jr(ra)
        }
        change(0x0017533C) { nop() }
        change(0x00175344) { nop() }
      }
      patch("LogoSkip", enabled = !publicBuild) {
        change(0x00085658) {
          li(t9, 0x14)
          sw(t9, 0xA0, s0) // change state field to 0x14 - logo ended
        }
      }
      patch("PopupDialogAlign") {
        change(0x000B650C) {
          li(s3, 0x7) // use correct size to properly calculate text offset
        }
      }

      patch("RenderFullName") {
        // Reference textRendering / textParsing_calcWidth
        // render full name, simply don't divide char count by 2 because we are using ascii
        change(0x000E76E0) { move(a0, v0) } // last name
        change(0x000E7658) { move(a0, v0) } // first name
        change(0x000E7760) { move(a0, v0) } // nick
      }

      includeNamePatch(funcs, vars)

      patch("RubyPatch") {
        change(0x000E8ED0) { li(t1, 0x7) } // use correct letter size for calculating base text width
        // change(0x000E84E4) { slti(t0, t3, 0x6) } // how many ruby characters - 1 can be used for auto center

        // change(0x000E84D8) { li(t2, 0x5) } // ruby letters spacing
        change(0x000E84D4) { // ruby letters spacing v2 (dynamic depending on current state)
          j(funcs.rubySpacingController)
          nop()
        }
      }
      patch("RubyDisableDynamicSpacing", enabled = false) {
        change(0x000E84D0) { li(v0, 0x1) }
      }
      patch("Ruby: Use main font as ruby font") {
        // WARNING:
        // DO NOT MODIFY THIS WITHOUT CHANGING NAME REPLACEMENT GENERATION
        change(0x001BA14C) {
          zeroTerminatedString("font.txb")
        }
        change(0x001BA164) {
          zeroTerminatedString("kanzi.bin")
        }
        // use Extra font size
        change(0x000E339C) { li(a1, 16) }
        change(0x000E33A4) { li(a2, 16) }
      }
      patch("Font render disable 1px subtract") {
        change(0x000E998C) {
          nop()
        }
        change(0x000E99F4) {
          nop()
        }
        change(0x000E9AB8) {
          nop()
        }
        change(0x000E9AF8) {
          nop()
        }
      }
      patch("BptResizePatch") {
        change(0x0006E38C) { li(a0, 0x7) }
        change(0x0006E4D0) { li(a0, 0x7) }
      }
      patch("ScrollableTextFieldHeightCalcFix") {
        change(0x000B660C) {
          jal(funcs.scrollableTextFieldHeightCalcFix)
          nop()
        }
      }

      includeSgPatch(funcs)

      patch("ConfirmButtonSwitch") {
        change(0x00168370) { li(a0, 1) }
        change(0x00168378) { li(a1, 1) }
      }
      patch("SaveMenuEnglishLanguageAndButtonOrder") {
        change(0x001107CC) {
          j(funcs.fixDialogLangAndButtonSwap)
          nop()
        }
        change(0x001107D4) {
          li(a0, 0x12)
        }
      }
      patch("CrossCircleButtonSwapHook1") {
        change(0x00112260) {
          jal(funcs.crossCircleButtonSwap)
        }
      }
      patch("CrossCircleButtonSwapHook2") {
        change(0x0010AA5C) {
          jal(funcs.crossCircleButtonSwap)
        }
      }

      includeSubsPatch(funcs)

      patch("Install: Don't load saved GAME.DNS") {
        change(0x001078CC) {
          move(a2, t0)
        }
      }
      patch("Install: X button will exit dialog") {
        change(0x000B19F0) {
          // jump to section for O button
          j(0x088B59B0)
          nop()
        }
        change(0x000B19D4) {
          li(a0, 0x3) // play confirm sound (instead of cancel sound)
        }
      }
      patch("Install: No install options in menu") {
        change(0x00092ADC) {
          jal(funcs.installIgnoreInstallOptions)
        }
      }
      patch("Conditional DL render") {
        change(0x000DF6E4) {
          jal(funcs.conditionalDlRender)
        }
        change(0x000DF6F4) {
          jal(funcs.conditionalDlRender)
        }
      }
      patch("Allow to load Extra US save data") {
        change(0x0021909C) {
          zeroTerminatedString("ULUS10576")
        }
      }

      patch("BB Channel - alt drop shadow") {
        change(0x0013836C) {
          jal(compileResult.functions.getValue("cccBbDropShadowDraw"))
        }
        change(0x001383F8) {
          jal(compileResult.functions.getValue("cccBbDropShadowResetCounter"))
        }
      }

      patch("Battle UI glitch fix") {
        change(0x00056DC8) {
          nop()
        }
      }

      patch("Status: fix first name text measure") {
        change(0x000AE5E8) {
          jal(compileResult.functions.getValue("cccCalcBasicTextWidth"))
          li(a1, 7)
          mov.s(f12, f0)
          nop()
          nop()
          nop()
        }
        change(0x000AE60C) {
          nop()
        }
      }
      patch("Status: fix nickname text measure") {
        change(0x000AE660) {
          jal(compileResult.functions.getValue("cccCalcBasicTextWidth"))
          li(a1, 7)
          mov.s(f12, f0)
          nop()
          nop()
        }
        change(0x000AE680) {
          nop()
        }
      }
      patch("Init memory fix for intro") {
        change(0x000856A0) {
          jal(funcs.initMemoryFix)
        }
      }
    }
  }
}

internal class PatchVars {
  var address: Int = 0

  var sDoubleSpace = 0
  var sSingleSpace = 0
  var bNameEntryIsCurrentRow = 0
  var sNameEntryInitialValue = 0
  var sHakuno = 0
  var sHakunon = 0
  var sKishinami = 0
  var sSgQuestion2Title = 0
  var sSgQuestion3Title = 0

  var sSgCurrentElementOffset = 0
  var sSgRinDat = 0
  var sSgRanDat = 0
  var sSgPslDat = 0
  var sSgZinDat = 0
  var sSgEliDat = 0
  var sSgMllDat = 0
  var sSgGilDat = 0
  var sSgKskDat = 0
  var sSgKiaDat = 0
  var sSgNerDat = 0
  var sSgTamDat = 0
  var sSgEmiDat = 0
  var dSgOffsetTable = 0
  var dInfomatrixMtxStRankOffsetTable = 0
  var bInfomatrixActive = 0
  var iInfomatrixCurrentId = 0
}

internal class PatchFuncs {
  val memcpy = 0x0898CF24
  val memcmp = 0x0898CE90
  val textRenderer = 0x088E7C28
  val textLengthMeasure = 0x088BA4D8

  var textRemapperBase = 0
  var mainTextRemapper = 0
  var secondaryTextRemapper = 0

  var itemTITM0001TextRemapperTextRenderCaller = 0
  var itemTITM0001TextRemapperTextParserCaller = 0
  var popupDialogMultilineCenterCalculationFix = 0

  var nameEntryCurrentRowCompare = 0
  var nameEntryIsActiveRowColorChange = 0
  var nameEntryCursorMovingDownSkipEmpty = 0
  var nameEntryCursorMovingUpSkipEmpty = 0
  var nameEntryCursorMovingFromKeyboardLeftToTabPane = 0
  var nameEntryCursorMovingFromKeyboardRightToTabPane = 0
  var nameEntryLastNameCursorPosFix = 0

  var scrollableTextFieldHeightCalcFix = 0
  var sgLineHeightAndShiftJisSpacingFix = 0
  var sgProfileCursorFix = 0

  var sgNameResolver = 0
  var sgOffsetApplier = 0
  var sgProfileSubsTextAlphaUpdate = 0

  var fixDialogLangAndButtonSwap = 0
  var crossCircleButtonSwap = 0

  var infomatrixMtxStRankOffsetApplier = 0
  var infomatrixSkillOffsetApplier = 0
  var infomatrixOnInit = 0
  var infomatrixOnDispose = 0
  var infomatrixOnRender = 0
  var infomatrixSkill3FixerDispatch = 0
  var infomatrixSkill3NewInfoOffsetApplier = 0

  var subsRendererDispatch = 0
  var subsSoundPlaybackInterceptorDispatch1 = 0
  var subsSoundPlaybackInterceptorDispatch2 = 0
  var subsSoundEffectPlaybackInterceptorDispatch = 0
  var subsPlayDialogAt3 = 0
  var subsEnterBattleMode = 0
  var subsExitBattleMode = 0

  var installIgnoreInstallOptions = 0

  var conditionalDlRender = 0
  var rubySpacingController = 0

  var initMemoryFix = 0
}
