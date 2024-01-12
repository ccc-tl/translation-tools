package tl.extra.ccc.patcher.translation

import tl.extra.ccc.util.CccWidthLimit
import tl.extra.util.TextMeasure
import java.io.File

class SgTranslationProcessor(
  pakExtract: File,
  outDir: File,
  unitDir: File,
  textMeasure: TextMeasure,
  warn: (String) -> Unit
) :
  PatcherBasedProcessor(pakExtract, outDir, unitDir) {
  init {
    patchSgCommonTextBinFile("interface/sg/0000.bin", 0)
    patchSgNamedTextBinFile("interface/sg/sg_eli.bin", 12)
    patchSgTextBinFile("interface/sg/sg_eli_text1.bin", 34)
    patchSgTextBinFile("interface/sg/sg_eli_text2.bin", 36)
    patchSgTextBinFile("interface/sg/sg_eli_text3.bin", 38)
    patchSgTextBinFile("interface/sg/sg_eli_text4.bin", 40)
    patchSgNamedTextBinFile("interface/sg/sg_emi.bin", 42)
    patchSgTextBinFile("interface/sg/sg_emi_text1.bin", 64)
    patchSgTextBinFile("interface/sg/sg_emi_text2.bin", 66)
    patchSgTextBinFile("interface/sg/sg_emi_text3.bin", 68)
    patchSgTextBinFile("interface/sg/sg_emi_text4.bin", 70)
    patchSgNamedTextBinFile("interface/sg/sg_gil.bin", 72)
    patchSgTextBinFile("interface/sg/sg_gil_text1.bin", 94)
    patchSgTextBinFile("interface/sg/sg_gil_text2.bin", 96)
    patchSgTextBinFile("interface/sg/sg_gil_text3.bin", 98)
    patchSgTextBinFile("interface/sg/sg_gil_text4.bin", 100)
    patchSgNamedTextBinFile("interface/sg/sg_kia.bin", 102)
    patchSgTextBinFile("interface/sg/sg_kia_text1.bin", 124)
    patchSgTextBinFile("interface/sg/sg_kia_text2.bin", 126)
    patchSgTextBinFile("interface/sg/sg_kia_text3.bin", 128)
    patchSgTextBinFile("interface/sg/sg_kia_text4.bin", 130)
    patchSgTextBinFile("interface/sg/sg_kia_textEx.bin", 132)
    patchSgNamedTextBinFile("interface/sg/sg_ksk.bin", 134)
    patchSgTextBinFile("interface/sg/sg_ksk_text1.bin", 156)
    patchSgTextBinFile("interface/sg/sg_ksk_text2.bin", 158)
    patchSgTextBinFile("interface/sg/sg_ksk_text3.bin", 160)
    patchSgTextBinFile("interface/sg/sg_ksk_text4.bin", 162)
    patchSgNamedTextBinFile("interface/sg/sg_mll.bin", 164)
    patchSgTextBinFile("interface/sg/sg_mll_text1.bin", 186)
    patchSgTextBinFile("interface/sg/sg_mll_text2.bin", 188)
    patchSgTextBinFile("interface/sg/sg_mll_text3.bin", 190)
    patchSgTextBinFile("interface/sg/sg_mll_text4.bin", 192)
    patchSgNamedTextBinFile("interface/sg/sg_ner.bin", 194)
    patchSgTextBinFile("interface/sg/sg_ner_text1.bin", 216)
    patchSgTextBinFile("interface/sg/sg_ner_text2.bin", 218)
    patchSgTextBinFile("interface/sg/sg_ner_text3.bin", 220)
    patchSgTextBinFile("interface/sg/sg_ner_text4.bin", 222)
    patchSgNamedTextBinFile("interface/sg/sg_psl.bin", 224)
    patchSgTextBinFile("interface/sg/sg_psl_text1.bin", 246)
    patchSgTextBinFile("interface/sg/sg_psl_text2.bin", 248)
    patchSgTextBinFile("interface/sg/sg_psl_text3.bin", 250)
    patchSgTextBinFile("interface/sg/sg_psl_text4.bin", 252)
    patchSgNamedTextBinFile("interface/sg/sg_ran.bin", 254)
    patchSgTextBinFile("interface/sg/sg_ran_text1.bin", 276)
    patchSgTextBinFile("interface/sg/sg_ran_text2.bin", 278)
    patchSgTextBinFile("interface/sg/sg_ran_text3.bin", 280)
    patchSgTextBinFile("interface/sg/sg_ran_text4.bin", 282)
    patchSgNamedTextBinFile("interface/sg/sg_rin.bin", 284)
    patchSgTextBinFile("interface/sg/sg_rin_text1.bin", 306)
    patchSgTextBinFile("interface/sg/sg_rin_text2.bin", 308)
    patchSgTextBinFile("interface/sg/sg_rin_text3.bin", 310)
    patchSgTextBinFile("interface/sg/sg_rin_text4.bin", 312)
    patchSgNamedTextBinFile("interface/sg/sg_tam.bin", 314)
    patchSgTextBinFile("interface/sg/sg_tam_text1.bin", 336)
    patchSgTextBinFile("interface/sg/sg_tam_text2.bin", 338)
    patchSgTextBinFile("interface/sg/sg_tam_text3.bin", 340)
    patchSgTextBinFile("interface/sg/sg_tam_text4.bin", 342)
    patchSgNamedTextBinFile("interface/sg/sg_zin.bin", 344)
    patchSgTextBinFile("interface/sg/sg_zin_text1.bin", 366)
    patchSgTextBinFile("interface/sg/sg_zin_text2.bin", 368)
    patchSgTextBinFile("interface/sg/sg_zin_text3.bin", 370)
    patchSgTextBinFile("interface/sg/sg_zin_text4.bin", 372)
    translation.checkForTooLongTranslations(textMeasure, warn, "SG", CccWidthLimit.sg)
  }
}