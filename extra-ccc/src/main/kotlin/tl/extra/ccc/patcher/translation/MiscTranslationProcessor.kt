package tl.extra.ccc.patcher.translation

import tl.extra.ccc.util.CccWidthLimit
import tl.extra.util.TextMeasure
import java.io.File

class MiscTranslationProcessor(
  pakExtract: File,
  outDir: File,
  unitDir: File,
  textMeasure: TextMeasure,
  warn: (String) -> Unit,
) : PatcherBasedProcessor(pakExtract, outDir, unitDir) {
  init {
    patchTextBinFile("interface/item/msg.bin", 0)
    patchTextBinFile("interface/mainmenu/help.bin", 1)
    patchTextBinFile("interface/modeselect/modeselect.bin", 44)
    patchTextBinFile("interface/myroom/msg.bin", 49)
    patchExtendedTextBinFile("interface/nameentry/msg.bin", 199)
    patchTextBinFile("interface/option/msg.bin", 265)
    patchTextBinFile("interface/osioki/msg.bin", 273)
    patchTextBinFile("interface/save/msg.bin", 364)
    patchExtendedTextBinFile("interface/select/i_move.bin", 372)
    patchTextBinFile("interface/shop/msg.bin", 378)
    patchTextBinFile("interface/status/msg.bin", 390)
    patchTextBinFile("interface/svtselect/msg.bin", 419)
    patchTextBinFile("interface/title/msg.bin", 425)
    patchTextBinFile("interface/charsel/svt_select.bin", 430)
    patchTextBinFile("interface/cmn/dialog.bin", 431)
    patchTextBinFile("interface/dayresult/d_result.bin", 433)
    patchTextBinFile("interface/dungeon/i_dun_sysmsg.bin", 434)
    patchTextBinFile("interface/dungeon/msg.bin", 435)
    patchTextBinFile("interface/equip/msg.bin", 645)
    patchTextBinFile("interface/gallery/msg.bin", 650)
    patchTextBinFile("interface/gameover/gov.bin", 683)
    patchTextBinFile("battle/interface/btl_msg.bin", 684)
    patchTextBinFile("interface/cmn/install.bin", 691, charset = Charsets.UTF_8)
    patchDungeonSelectBinFile("interface/dungeonselect/0000.bin", 696)
    patchSjisFile("paramData/chaDataTbl.sjis", 736)
    translation.checkForTooLongTranslations(textMeasure, warn, "Misc - next objective", CccWidthLimit.nextObjective, offsets = 435..644)
  }
}
