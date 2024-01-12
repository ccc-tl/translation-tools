package tl.extra.patcher.eboot

import kmips.Assembler
import kmips.FpuReg.f12
import kmips.Reg.*
import kmipsx.elf.CompileResult
import kmipsx.elf.ElfPatchSuite
import kmipsx.util.callerSavedRegisters
import kmipsx.util.preserve
import kmipsx.util.region

internal fun Assembler.includeExtSubsPatch(funcs: PatchFuncs, compileResult: CompileResult) {
  funcs.subsRendererDispatch = region {
    val ctx = preserve(callerSavedRegisters)
    jal(compileResult.functions.getValue("subsRenderer"))
    nop()
    jal(0x088BC36C)
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

  funcs.subsEnterBattleMode = region {
    mtc1(a0, f12) // orig code
    val ctx = preserve(listOf(s0, s1))
    la(s0, compileResult.variables.getValue("extInBattleMode"))
    li(s1, 0x1)
    sb(s1, 0x0, s0)
    ctx.restoreAndExit()
  }

  funcs.subsExitBattleMode = region {
    li(s2, 0) // orig code
    val ctx = preserve(listOf(s0))
    la(s0, compileResult.variables.getValue("extInBattleMode"))
    sb(zero, 0x0, s0)
    ctx.restoreAndExit()
  }
}

internal fun ElfPatchSuite.includeSubsPatch(funcs: PatchFuncs) {
  patch("SubsRenderHook") {
    change(0x00111E88) {
      jal(funcs.subsRendererDispatch)
    }
  }
  patch("SoundPlaybackInterceptorHook1") {
    change(0x000D4DF4) {
      jal(funcs.subsSoundPlaybackInterceptorDispatch1)
    }
  }
  patch("SoundPlaybackInterceptorHook2") {
    change(0x000D5FB0) {
      jal(funcs.subsSoundPlaybackInterceptorDispatch2)
    }
  }
  patch("SoundEffectPlaybackInterceptorHook") {
    change(0x000D4530) {
      sw(ra, 0x0, sp)
      jal(funcs.subsSoundEffectPlaybackInterceptorDispatch)
      move(v1, t1)
      andi(t3, t0, 0xFF)
      move(t0, a2)
    }
  }
  patch("EnterBattleMode") {
    change(0x00043F10) {
      jal(funcs.subsEnterBattleMode)
    }
  }
  patch("ExitBattleMode") {
    change(0x00044224) {
      jal(funcs.subsExitBattleMode)
    }
  }
}
