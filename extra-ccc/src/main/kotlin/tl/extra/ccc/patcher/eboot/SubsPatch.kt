package tl.extra.ccc.patcher.eboot

import kmips.Assembler
import kmips.Reg.*
import kmipsx.elf.CompileResult
import kmipsx.elf.ElfPatchSuite
import kmipsx.util.callerSavedRegisters
import kmipsx.util.preserve
import kmipsx.util.region

internal fun Assembler.includeExtSubsPatch(funcs: PatchFuncs, compileResult: CompileResult) {
  funcs.subsRendererDispatch = region {
    sw(s2, 0x58, sp)
    val ctx = preserve(callerSavedRegisters)
    jal(compileResult.functions.getValue("subsRenderer"))
    nop()
    ctx.restoreAndExit()
  }

  funcs.subsSoundPlaybackInterceptorDispatch1 = region {
    sw(t0, 4, a1) // orig code
    val ctx = preserve(callerSavedRegisters)
    // a0 here is conveniently void** to audio ctl
    jal(compileResult.functions.getValue("subsSoundPlaybackInterceptor"))
    nop()
    ctx.restoreAndExit()
  }

  funcs.subsSoundPlaybackInterceptorDispatch2 = region {
    sw(a2, 0xC, s6) // orig code
    val ctx = preserve(callerSavedRegisters)
    move(a0, s2) // s2 is void** to audio ctl
    jal(compileResult.functions.getValue("subsSoundPlaybackInterceptor"))
    nop()
    ctx.restoreAndExit()
  }

  funcs.subsSoundEffectPlaybackInterceptorDispatch = region {
    move(t1, t3) // orig code
    val ctx = preserve(callerSavedRegisters)
    jal(compileResult.functions.getValue("subsSoundEffectPlaybackInterceptor"))
    nop()
    ctx.restoreAndExit()
  }

  funcs.subsPlayDialogAt3 = region {
    li(a3, 0) // orig code
    val ctx = preserve(listOf(s0, s1))
    la(s0, compileResult.variables.getValue("extInhibitSubsForFrames"))
    li(s1, 30)
    sb(s1, 0x0, s0)
    ctx.restoreAndExit()
  }

  funcs.subsEnterBattleMode = region {
    move(t0, zero) // orig code
    val ctx = preserve(listOf(s0, s1))
    la(s0, compileResult.variables.getValue("extInBattleMode"))
    li(s1, 0x1)
    sb(s1, 0x0, s0)
    ctx.restore()
    j(0x0004897C + 0x8804000)
    nop()
  }

  funcs.subsExitBattleMode = region {
    move(s1, a0) // orig code
    val ctx = preserve(listOf(s0))
    la(s0, compileResult.variables.getValue("extInBattleMode"))
    sb(zero, 0x0, s0)
    ctx.restore()
    j(0x0004A784 + 0x8804000)
    nop()
  }
}

internal fun ElfPatchSuite.includeSubsPatch(funcs: PatchFuncs) {
  patch("NextObjectiveRendererHook") {
    change(0x0008FAFC) {
      sw(ra, 0x60, sp)
      jal(funcs.subsRendererDispatch)
      sw(s3, 0x5C, sp)
    }
  }
  patch("SoundPlaybackInterceptorHook1") {
    change(0x0010E918) {
      jal(funcs.subsSoundPlaybackInterceptorDispatch1)
    }
  }
  patch("SoundPlaybackInterceptorHook2") {
    change(0x0010FC9C) {
      jal(funcs.subsSoundPlaybackInterceptorDispatch2)
    }
  }
  patch("SoundEffectPlaybackInterceptorHook") {
    change(0x0010DF68) {
      sw(ra, 0x0, sp)
      jal(funcs.subsSoundEffectPlaybackInterceptorDispatch)
      move(v1, t1)
      andi(t3, t0, 0xFF)
      move(t0, a2)
    }
  }
  patch("PlayDialogAt3Related") {
    change(0x0011DE78) {
      jal(funcs.subsPlayDialogAt3)
    }
  }
  patch("EnterBattleMode") {
    change(0x00048974) {
      j(funcs.subsEnterBattleMode)
    }
  }
  patch("ExitBattleMode") {
    change(0x0004A77C) {
      j(funcs.subsExitBattleMode)
    }
  }
}
