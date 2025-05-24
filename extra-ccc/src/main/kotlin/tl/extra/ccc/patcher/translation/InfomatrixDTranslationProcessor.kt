package tl.extra.ccc.patcher.translation

import tl.extra.ccc.util.CccWidthLimit
import tl.extra.util.TextMeasure
import java.io.File

class InfomatrixDTranslationProcessor(
  pakExtract: File,
  outDir: File,
  unitDir: File,
  textMeasure: TextMeasure,
  warn: (String) -> Unit,
) : PatcherBasedProcessor(pakExtract, outDir, unitDir) {
  init {
    patchTextBinFile("interface/infomatrixex/msg.bin", 0)
    patchInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_01.bin", 94)
    patchInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_02.bin", 108)
    patchInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_03.bin", 122)
    patchInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_04.bin", 136)
    patchInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_05.bin", 150)
    patchInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_06.bin", 164)
    patchInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_07.bin", 178)
    patchInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_08.bin", 192)
    translation.checkForTooLongTranslations(textMeasure, warn, "Infomatrix", CccWidthLimit.infomatrix)
  }
}
