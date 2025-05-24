package tl.extra.patcher.eboot

import kio.util.child
import kio.util.toHex
import kio.util.toWHex
import kmips.Label
import kmips.Reg.*
import kmips.assembleAsByteArray
import kmipsx.elf.CompileResult
import kmipsx.elf.ElfPatchSuite
import kmipsx.elf.pspCodeCompiler
import kmipsx.elf.writeElfSectionsInto
import kmipsx.util.callerSavedRegisters
import kmipsx.util.preserve
import kmipsx.util.region
import kmipsx.util.zeroTerminatedString
import tl.extra.file.PakFile
import tl.extra.file.replaceEntry
import tl.extra.file.sizeInGame
import java.io.File

internal class ExtraAsmPatcher(private val nativeDir: File, pspSdkDir: File) {
  private val nativeCodeCompiler = pspCodeCompiler(pspSdkDir)
  private val nativeSrcFiles = listOf("patch.cpp", "generated.cpp", "subs.cpp", "subs_draw.cpp", "hook.cpp", "game.cpp", "platform.cpp", "util.cpp")

  val bootPatches = ElfPatchSuite(baseAddress = 0x8804000, useRelativeChangeAddress = true)
  lateinit var compileResult: CompileResult

  lateinit var auxPatchBytes: ByteArray

  private var vars = PatchVars()
  private var funcs = PatchFuncs()

  fun assemble(stockPreloadPak: PakFile, pakReplacements: Map<String, File>) {
    val baseLoadAddress = 0x08C61CE0
    val pakEntries = stockPreloadPak.entries
    // first phase text remapping is only needed for files that replaces PRELOAD.pak files
    println("First patcher run...")
    pakEntries.forEach {
      val replacement = pakReplacements[it.path] ?: return@forEach
      println("Patched file replaces PRELOAD.pak entry ${it.path}")
      pakEntries.replaceEntry(it.path, replacement.readBytes())
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
    )
    writeElfSectionsInto(patchBytes, compileResult, virtualAddr)
    println("ASM patch will start at ${virtualAddr(patchBytes.size).toHex()}, overhead: ${patchBytes.size} bytes")
    val extraPatch = createPreloadCodePatch(virtualAddr(patchBytes.size))
    patchBytes.addAll(extraPatch.toList())

    auxPatchBytes = patchBytes.toByteArray()
    println("Aux patch total overhead: ${auxPatchBytes.size} bytes")
    createBootPatches()
  }

  private fun createPreloadCodePatch(patchStart: Int) = assembleAsByteArray(patchStart) {
    run {
      vars.address = virtualPc
      nop()
    }

    funcs.nameEntryCursorMovingDown = region {
      val ctx = preserve(listOf(t0))
      val resumeStdCursorChange = Label()

      lw(t1, 0x310, s1) // load X pos
      bne(t1, zero, resumeStdCursorChange) // X cursor is on keyboard, do nothing
      nop()

      li(t1, 1)
      beq(a0, t1, resumeStdCursorChange)
      li(a0, 0x3)

      addiu(a0, s6, 1) // cursor doesn't need change, load value that would be used normally

      label(resumeStdCursorChange)
      sw(a0, 0x314, s1)
      slti(a0, a0, 0x5)
      ctx.restoreAndExit()
    }

    funcs.nameEntryCursorMovingUp = region {
      val defBranch = Label()
      val resumeStdCursorChange = Label()
      val changeCursor = Label()

      val ctx = preserve(listOf(t1))

      lw(t1, 0x310, s1) // load X pos
      bne(t1, zero, resumeStdCursorChange) // X cursor is on keyboard, do nothing
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
      j(0x0887E86C)
      nop()
      label(defBranch)
      j(0x0887E87C)
      nop()
    }

    funcs.nameEntryCursorMovingLeft = region {
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
      j(0x0887E62C)
      nop()
      label(defBranch)
      j(0x0887E63C)
      nop()
    }

    funcs.nameEntryCursorMovingRight = region {
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

    funcs.statusScreenNameSpacing = region {
      val ctx = preserve(listOf(t7))
      lui(t7, 0x4111)
      data(0x448F6800) // mtc1	t7,f13
      ctx.restoreAndExit()
    }

    funcs.uncompressedCmpLoader = region {
      val ctx = preserve(callerSavedRegisters + listOf(s6, s7))
      val continueStdDecompress = Label()
      val skipOutArgWrite = Label()

      lw(t0, 0x0, a0)
      la(t1, 0x50434549) // IECP magic string
      beq(t0, t1, continueStdDecompress)
      nop()

      // not dealing with a compressed file
      beq(a2, zero, skipOutArgWrite) // if a2 output argument is zero -> don't update it
      nop()
      sw(a1, 0x0, a2) // store decompressed size in a2
      label(skipOutArgWrite)
      move(s6, a0) // s6 = decompressed data ptr
      move(s7, a1) // s7 = decompressed data size
      // do the magic
      jal(funcs.getHeapCtlByType) // get game heap ctl
      li(a0, 0x3)
      move(a0, v0)
      move(a1, s7)
      li(a2, 0x3)
      li(a3, 0x10)
      la(t0, 0x0896AE60)
      jal(funcs.heapAllocate)
      li(t1, 0x30)
      // finally copy bytes to newly allocated space
      move(a0, v0) // dest
      move(a1, s6) // src
      jal(funcs.memcpy)
      move(a2, s7) // size

      ctx.restore(listOf(v0))
      j(0x000D6224.toLoadAddr())
      nop()

      // continue standard decompression routine
      label(continueStdDecompress)
      ctx.restore()
      addiu(a0, a0, 1)
      addiu(a0, a0, 1)
      j(0x000D6090.toLoadAddr())
    }

    includeExtSubsPatch(funcs, compileResult)

    nop()
  }

  private fun createBootPatches() {
    with(bootPatches) {
      val neMaxCharCount = 11
      patch("NameEntryCursorMovingDownFix") {
        change(0x0007A7A8) {
          jal(funcs.nameEntryCursorMovingDown)
          nop()
        }
      }
      patch("NameEntryCursorMovingUpFix") {
        change(0x0007A864) {
          jal(funcs.nameEntryCursorMovingUp)
          nop()
        }
      }
      patch("NameEntryCursorMovingRight") {
        change(0x0007A5F8) {
          jal(funcs.nameEntryCursorMovingRight)
          nop()
        }
      }
      patch("NameEntryCursorMovingLeft") {
        change(0x0007A624) {
          jal(funcs.nameEntryCursorMovingLeft)
          nop()
        }
      }
      patch("NameEntryCursor") {
        // fix last name spacing for entry screen
        change(0x00077748) {
          lui(a1, 0x424C)
        }
        // fix cursor spacing
        change(0x00079A34) {
          addiu(a0, a0, 0x61)
        }
        // increase cursor limit
        change(0x00079A24) { slti(a1, a0, neMaxCharCount) }
        change(0x00079A2C) { li(a0, neMaxCharCount - 1) }

        // fix manual cursor move
        // first name
        change(0x00078728) { li(a1, neMaxCharCount) }
        change(0x00078738) { slti(a0, a0, neMaxCharCount + 1) }
        // last name
        change(0x00078818) { slti(a0, a0, neMaxCharCount) }
      }
      patch("NameEntryInput") {
        // first name
        change(0x00077F60) { li(a1, neMaxCharCount) }
        change(0x00077F6C) { slti(a0, a0, neMaxCharCount + 1) }
        // last name
        change(0x00078080) { slti(t2, a1, neMaxCharCount + 1) }
        // indefinitely replace last character of name input
        change(0x0007810C) { sb(s1, 0x3F, a0) }
      }
      patch("NameEntryEmptyCheck") {
        // first name
        change(0x000775E4) { li(a2, neMaxCharCount) }
        // last name
        change(0x00077614) { li(a2, neMaxCharCount) }
        change(0x0014C074) {
          zeroTerminatedString(" ".repeat(neMaxCharCount)) // this is used to compare against when checking names
        }
      }
      patch("NameEntryDeletion") {
        change(0x00078564) {
          val stdExit = Label()
          stdExit.address = 0x0887C5AC

          li(a1, neMaxCharCount - 1)
          bne(a0, a1, stdExit)
          nop()
          li(s7, 0x20)
          sb(s7, 0xE, sp)
          sb(zero, 0xF, sp)
          j(0x0887C5A4)
          nop()
        }
      }
      patch("NameEntryInit") {
        change(0x00078F3C) { li(a2, 1) }
        change(0x00078F4C) { li(a2, 1) }
        change(0x00078F54) { addiu(s1, s1, 1) }
        change(0x00078F60) { addiu(s2, s2, 1) }
        change(0x00078F58) { slti(a0, s0, neMaxCharCount) }
      }
      patch("NameEntryConfirmPromptSpacingFix") {
        change(0x0007793C) { lui(a0, 0x4170) }
      }
      patch("StatusScreenNameSpacing") {
        change(0x0008C3C4) {
          jal(funcs.statusScreenNameSpacing)
        }
      }
      patch("DefaultLineWidth") {
        change(0x00090AD4) { li(t1, 0x33) }
      }
      patch("InfoMatrixTooMuchOneCharacter") {
        // fix memset cleared area size, otherwise one junk character is present there
        change(0x00069148) { li(a2, 0x19) }
      }

      patch("MaxCmpLoadSize") {
        // new max is as little as possible
        change(0x000D7C3C) { li(a1, 0x10) }
      }
      patch("AllowUncompressedCmp", enabled = false) {
        change(0x000D6088) {
          j(funcs.uncompressedCmpLoader)
          nop()
        }
      }
      patch("NoMoreCmp: pak load flag override") {
        change(0x0002C970) {
          li(a1, 0x0)
        }
      }
      patch("NoMoreCmp: hacks edition", enabled = false) {
        // flag check: if set then load new content into CMP buffer
        change(0x000D830C) {
          j(0x000D8324.toLoadAddr())
        }

        // if loading cmp file then run cmp decompressor
        change(0x000D7EC0) {
          nop()
          nop()
        }

        // check if loading cmp or pak file
        change(0x000D809C) {
          j(0x000D80CC.toLoadAddr())
        }

        // check how to link this pak file into pak loaded list
        change(0x000D8488) {
          j(0x000D84A8.toLoadAddr())
        }
      }

      patch("RubyPatch") {
        change(0x000B6D6C) { li(t1, 0x8) } // use correct letter size for calculating base text width
        change(0x000B63B4) { li(t2, 0x5) } // ruby letters spacing
      }
      patch("RubyDisableDynamicSpacing", enabled = true) {
        change(0x000B63AC) { li(v0, 0x1) }
      }
      patch("Ruby: Use main font as ruby font") {
        // WARNING:
        // DO NOT MODIFY THIS WITHOUT CHANGING NAME REPLACEMENT GENERATION
        change(0x0014FCF4) {
          zeroTerminatedString("font.txb")
        }
        change(0x0014FD0C) {
          zeroTerminatedString("kanzi.bin")
        }
        change(0x000B0FC0) { li(a1, 16) }
        change(0x000B0FC8) { li(a2, 16) }
      }

      includeSubsPatch(funcs)
    }
  }

  private fun Int.toLoadAddr(): Int {
    return this + 0x8804000
  }
}

internal class PatchVars {
  var address: Int = 0
}

internal class PatchFuncs {
  val memcpy = 0x0893CAE4
  val getHeapCtlByType = 0x088AFCD4
  val heapAllocate = 0x088DC7C4

  var nameEntryCursorMovingDown = 0
  var nameEntryCursorMovingUp = 0
  var nameEntryCursorMovingLeft = 0
  var nameEntryCursorMovingRight = 0
  var statusScreenNameSpacing = 0
  var uncompressedCmpLoader = 0

  var subsRendererDispatch = 0
  var subsSoundPlaybackInterceptorDispatch1 = 0
  var subsSoundPlaybackInterceptorDispatch2 = 0
  var subsSoundEffectPlaybackInterceptorDispatch = 0
  var subsEnterBattleMode = 0
  var subsExitBattleMode = 0
}
