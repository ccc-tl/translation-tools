package tl.extra.ccc.patcher.translation

import java.io.File

class InfomatrixTTranslationProcessor(pakExtract: File, outDir: File, unitDir: File) : PatcherBasedProcessor(pakExtract, outDir, unitDir) {
  init {
    patchInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_01.bin", 206)
    patchInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_02.bin", 238)
    patchInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_03.bin", 270)
    patchInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_04.bin", 302)
    patchInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_05.bin", 334)
    patchInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_06.bin", 366)
    patchInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_07.bin", 398)
    patchInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_08.bin", 430)
  }
}
