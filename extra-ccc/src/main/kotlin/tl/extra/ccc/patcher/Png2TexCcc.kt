package tl.extra.ccc.patcher

import kio.util.child
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.cccUnpack
import tl.extra.patcher.tex.ConversionType
import tl.extra.patcher.tex.Png2Tex
import tl.extra.patcher.tex.TexConversion
import java.io.File

fun main() {
  Png2TexCcc(cccToolkit)
}

internal class Png2TexCcc(baseDir: File) : Png2Tex(
  cccUnpack,
  baseDir.child("src/png2tex"),
  baseDir.child("src/custom-pak-files"),
  extract = false,
  verify = true,
) {
  init {
    repackProgress()
    repackInfoMatrix()
    repackLoca()
    repackMainMenu()
    repackEvGraphic()
    repackMyRoom()
    repackICon()
    repackISelect()
    repackIBpt()
    repackBtlMenu()
    repackBtlEmi()
    repackShop()
    repackEquip()
    repackSgSelect()
    repackGallery()
    repackTitle()
    repackBtlVs()
    repackBtlVsAlt()
    repackSg()
    repackSgAlt()
    repackItem()
    repackSave()
    repackDungeonSelect()
    repackLogo()
    repackGameOver()
    repackOption()
    repackStatus()
    repackModeSelect()
    repackFont()
    repackRuby()
    repackAdvice()
    repackIconName()
    repackIconNameAlt()
    repackPoems()
    repackStaffRoll()
    repackDayResult()
    repackDayResultAlt()
  }

  private fun repackProgress() {
    repackTexture(
      "progress",
      "interface/progress/0000.txb",
      listOf(
        TexConversion(14, ConversionType.SWIZZLED_4BPP_CUSTOM_PALETTED_LINEAR_CCT),
        TexConversion(15, ConversionType.SWIZZLED_4BPP_CUSTOM_PALETTED_LINEAR_CCT),
        TexConversion(16, ConversionType.SWIZZLED_4BPP_CUSTOM_PALETTED_LINEAR_CCT),
        TexConversion(17, ConversionType.SWIZZLED_4BPP_CUSTOM_PALETTED_LINEAR_CCT),
        TexConversion(19, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackInfoMatrix() {
    repackTexture(
      "infomatrixex",
      "interface/infomatrixex/0000.txb",
      listOf(
        TexConversion(26, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(27, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(28, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(29, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(30, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(31, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(32, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(45, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(76, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(82, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(83, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(84, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(88, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(89, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(90, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(91, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(92, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(93, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(94, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(101, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(102, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(103, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(104, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(105, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(108, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(114, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(115, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(116, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(117, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(118, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(126, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(132, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(141, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(142, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(143, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(144, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(145, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(146, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(147, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(148, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(150, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackLoca() {
    listOf(
      "loca_01", "loca_02", "loca_03", "loca_04", "loca_05", "loca_06",
      "loca_07", "loca_08", "loca_09", "loca_10", "loca_11", "loca_12",
      "loca_13", "loca_14", "loca_15", "loca_16", "loca_17", "loca_18",
      "loca_19", "loca_20",
    ).forEach { loca ->
      repackTexture(
        "dungeon",
        "interface/dungeon/$loca.txb",
        listOf(
          TexConversion(0, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT_CORRECT_ALPHA),
          TexConversion(6, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT_CORRECT_ALPHA),
        ),
        innerDirSuffix = " $loca",
      )
    }

    listOf("loca_21", "loca_22", "loca_23").forEach { loca ->
      repackTexture(
        "dungeon",
        "interface/dungeon/$loca.txb",
        listOf(
          TexConversion(1, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT_CORRECT_ALPHA),
        ),
        innerDirSuffix = " $loca",
      )
    }

    listOf("loca_24", "loca_25").forEach { loca ->
      repackTexture(
        "dungeon",
        "interface/dungeon/$loca.txb",
        listOf(
          TexConversion(0, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT_CORRECT_ALPHA),
          TexConversion(1, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT_CORRECT_ALPHA),
          TexConversion(6, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT_CORRECT_ALPHA),
        ),
        innerDirSuffix = " $loca",
      )
    }
  }

  private fun repackMainMenu() {
    repackTexture(
      "mainmenu",
      "interface/mainmenu/0001.txb",
      listOf(
        TexConversion(89, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackEvGraphic() {
    png2TexDir.child("evgraphic").listFiles()!!
      .filter { it.extension == "txb" }
      .forEach { file ->
        val texToConvert = listOf(0)
          .map {
            TexConversion(it, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT)
          }

        repackTexture(
          "evgraphic",
          "interface/event/pict/${file.name}",
          texToConvert,
          innerDirSuffix = " ${file.nameWithoutExtension}",
        )
      }
  }

  private fun repackMyRoom() {
    repackTexture(
      "myroom",
      "interface/myroom/myroom.txb",
      listOf(
        TexConversion(16, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(52, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackICon() {
    repackTexture(
      "i_con",
      "interface/cmn/i_con.txb",
      listOf(
        TexConversion(12, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackISelect() {
    repackTexture(
      "i_select",
      "interface/select/i_select.txb",
      listOf(
        TexConversion(11, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(12, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackIBpt() {
    repackTexture(
      "i_bpt",
      "interface/cmn/i_bpt.txb",
      listOf(
        TexConversion(0, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(1, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackBtlMenu() {
    repackTexture(
      "btl_menu",
      "battle/interface/btl_menu.txb",
      listOf(
        TexConversion(36, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(109, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(110, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(111, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackBtlEmi() {
    repackTexture(
      "btl_emi",
      "battle/interface/emi/0001.txb",
      listOf(
        TexConversion(0, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(1, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(8, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(9, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(10, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(11, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(12, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(13, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(14, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(15, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(17, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(18, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackShop() {
    // Align manually fixed
    repackTexture(
      "shop",
      "interface/shop/shop_00.txb",
      listOf(
        TexConversion(5, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(39, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(40, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(42, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(13, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackEquip() {
    // Align manually fixed
    repackTexture(
      "equip",
      "interface/equip/equip_00.txb",
      listOf(
        TexConversion(6, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(14, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(15, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackSgSelect() {
    // Asm patch was required to fix cursor in profile section
    repackTexture(
      "sg",
      "interface/sg/sgselect_tex.txb",
      listOf(45, 46, 47, 49).map {
        TexConversion(it, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT)
      },
      innerDirSuffix = " sgselect_tex",
    )
  }

  private fun repackGallery() {
    repackTexture(
      "gallery",
      "interface/gallery/gallery_tex.txb",
      listOf(
        TexConversion(1, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackTitle() {
    // Removed cancel from install dialog buttons
    repackTexture(
      "title",
      "interface/title/0000.txb",
      listOf(
        TexConversion(18, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackBtlVs() {
    repackBtlVs("btl_vs", copyToCustomPakFiles = true)
  }

  private fun repackBtlVsAlt() {
    repackBtlVs("btl_vs_alt", copyToCustomPakFiles = false)
  }

  private fun repackBtlVs(baseDirName: String, copyToCustomPakFiles: Boolean) {
    repackTexture(
      baseDirName,
      "battle/interface/vs/btl_vs.txb",
      listOf(
        TexConversion(8, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(9, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(14, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(10, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(35, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(37, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(38, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(41, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
      copyToCustomPakFiles = copyToCustomPakFiles,
    )
  }

  private fun repackSg() {
    // Asm patch was required to fix cursor position, .dat patching was required to fix some tex positions
    png2TexDir.child("sg")
      .listFiles()!!
      .filter { it.isFile && it.extension == "txb" }
      .filterNot { it.nameWithoutExtension == "sgselect_tex" }
      .forEach { file ->
        val texToConvert = let {
          when (file.nameWithoutExtension) {
            "sg_kia" -> listOf(5, 6, 7, 9, 11, 13)
            in listOf("sg_eli", "sg_mll", "sg_gil") -> listOf(3, 5, 6, 7, 9, 11)
            else -> listOf(5, 6, 7, 9, 11)
          }
        }.map {
          if (it == 3 && file.nameWithoutExtension == "sg_mll") {
            TexConversion(it, ConversionType.SWIZZLED_8BPP_CUSTOM_PALETTED_LINEAR_CCT)
          } else {
            TexConversion(it, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT)
          }
        }

        repackTexture(
          "sg",
          "interface/sg/${file.name}",
          texToConvert,
          innerDirSuffix = " ${file.nameWithoutExtension}",
        )
      }
  }

  private fun repackSgAlt() {
    png2TexDir.child("sg_alt")
      .listFiles()!!
      .filter { it.isFile && it.extension == "txb" }
      .forEach { file ->
        val texToConvert = let {
          when (file.nameWithoutExtension) {
            "sg_mll" -> listOf(3, 5, 6, 7, 9, 11)
            else -> error("Not implemented SG alt texture to convert list for file '${file.name}'")
          }
        }.map {
          if (it == 3) {
            TexConversion(it, ConversionType.SWIZZLED_8BPP_CUSTOM_PALETTED_LINEAR_CCT)
          } else {
            TexConversion(it, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT)
          }
        }

        repackTexture(
          "sg_alt",
          "interface/sg/${file.name}",
          texToConvert,
          innerDirSuffix = " ${file.nameWithoutExtension}",
          copyToCustomPakFiles = false,
        )
      }
  }

  private fun repackItem() {
    // Align manually fixed
    repackTexture(
      "item",
      "interface/item/item_00.txb",
      listOf(
        TexConversion(11, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackSave() {
    repackTexture(
      "save",
      "interface/save/0000.txb",
      listOf(
        TexConversion(6, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(7, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(8, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(9, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(11, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
      innerDirSuffix = " 0000",
    )
    repackTexture(
      "save",
      "interface/save/0001.txb",
      listOf(
        TexConversion(7, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(8, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
      innerDirSuffix = " 0001",
    )
  }

  private fun repackDungeonSelect() {
    repackTexture(
      "dungeonselect",
      "interface/dungeonselect/dungeon_select.txb",
      (1..20).map {
        TexConversion(it, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT)
      },
    )
  }

  private fun repackLogo() {
    repackTexture(
      "logo",
      "interface/logo/0000.txb",
      listOf(
        TexConversion(4, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackGameOver() {
    repackTexture(
      "gameover",
      "interface/gameover/gov_00.txb",
      listOf(
        TexConversion(3, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(4, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackOption() {
    repackTexture(
      "option",
      "interface/option/option_00.txb",
      listOf(
        TexConversion(4, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(5, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(6, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(7, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(8, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(9, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(10, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(28, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackStatus() {
    repackTexture(
      "status",
      "interface/status/0000.txb",
      listOf(
        TexConversion(13, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(14, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(15, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(16, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(17, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(28, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(29, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(30, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(81, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(82, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackModeSelect() {
    repackTexture(
      "modeselect",
      "interface/modeselect/modeselect.txb",
      listOf(
        TexConversion(3, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(5, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(6, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(12, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackFont() {
    repackTexture(
      "font",
      "font.txb",
      listOf(
        TexConversion(0, ConversionType.UNSWIZZLED_4BPP_PALETTED_NO_CCT),
        TexConversion(10, ConversionType.UNSWIZZLED_4BPP_PALETTED_NO_CCT),
        TexConversion(11, ConversionType.UNSWIZZLED_4BPP_PALETTED_NO_CCT),
      ),
    )
  }

  private fun repackRuby() {
    repackTexture(
      "ruby",
      "ruby.txb",
      listOf(TexConversion(0, ConversionType.UNSWIZZLED_4BPP_PALETTED_NO_CCT)),
      dropEntries = listOf(1),
    )
  }

  private fun repackAdvice() {
    // Some images are wider but game auto centers them
    repackTexture(
      "advice",
      "interface/advice/0000.txb",
      listOf(
        TexConversion(1, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(2, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(3, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(4, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(5, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(7, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(8, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
    )
  }

  private fun repackIconName() {
    // Looks good even with some textures being slightly higher (16 x 20px)
    repackIconName("i_con_name", copyToCustomPakFiles = true)
  }

  private fun repackIconNameAlt() {
    repackIconName("i_con_name_alt", copyToCustomPakFiles = false)
  }

  private fun repackIconName(baseDirName: String, copyToCustomPakFiles: Boolean) {
    repackTexture(
      baseDirName,
      "interface/cmn/i_con_name.txb",
      (0..37).map {
        TexConversion(it, ConversionType.UNSWIZZLED_4BPP_PALETTED_NO_CCT)
      },
      copyToCustomPakFiles = copyToCustomPakFiles,
    )
  }

  private fun repackPoems() {
    val poemsTextures = listOf(
      "cpt_00" to listOf(
        Pair(
          ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
          listOf(13, 14, 15, 16, 17),
        ),
      ),
      "cpt_01" to listOf(
        Pair(
          ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
          listOf(9, 12, 13, 14, 15, 16, 17, 18, 19, 20),
        ),
      ),
      "cpt_02" to listOf(
        Pair(
          ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
          listOf(9, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23),
        ),
      ),
      "cpt_03" to listOf(
        Pair(
          ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
          listOf(9, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25),
        ),
      ),
      "cpt_04" to listOf(
        Pair(
          ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
          listOf(9, 13, 14, 15, 16, 17, 18, 19, 20),
        ),
      ),
      "cpt_05" to listOf(
        Pair(
          ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
          listOf(9, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23),
        ),
      ),
      "cpt_06" to listOf(
        Pair(
          ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
          listOf(9, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28),
        ),
      ),
      "cpt_07" to listOf(
        Pair(
          ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
          listOf(9, 13, 14, 15),
        ),
      ),
      "cpt_08" to listOf(
        Pair(
          ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT,
          listOf(9, 13, 14, 15),
        ),
      ),
      "poem_ex_tex" to listOf(
        Pair(
          ConversionType.SWIZZLED_8BPP_CUSTOM_PALETTED_LINEAR_CCT,
          listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
        ),
      ),
    )
    poemsTextures.forEach { (chapterName, config) ->
      val conversions = config.flatMap { (convertMode, texIds) ->
        texIds.map { TexConversion(it, convertMode) }
      }
      repackTexture(
        "poem",
        "interface/poem/$chapterName.txb",
        conversions,
        innerDirSuffix = " $chapterName",
      )
    }
  }

  private fun repackStaffRoll() {
    val staffRollTextures = listOf(
      "01" to listOf(
        TexConversion(2, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT),
      ),
      "02" to listOf(
        TexConversion(1, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT),
        TexConversion(2, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT),
        TexConversion(6, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT),
      ),
      "03" to listOf(
        TexConversion(2, ConversionType.SWIZZLED_8BPP_PALETTED_LINEAR_CCT),
        TexConversion(4, ConversionType.SWIZZLED_8BPP_CUSTOM_PALETTED_LINEAR_CCT),
        TexConversion(5, ConversionType.SWIZZLED_8BPP_CUSTOM_PALETTED_LINEAR_CCT),
      ),
    )
    staffRollTextures.forEach { (setName, conversions) ->
      repackTexture(
        "staffroll",
        "interface/staffroll/staffroll_$setName.txb",
        conversions,
        innerDirSuffix = " staffroll_$setName",
      )
    }
  }

  private fun repackDayResult() {
    repackDayResultBase("dayresult", copyToCustomPakFiles = true)
  }

  private fun repackDayResultAlt() {
    repackDayResultBase("dayresult_alt", copyToCustomPakFiles = false)
  }

  private fun repackDayResultBase(baseDirName: String, copyToCustomPakFiles: Boolean) {
    repackTexture(
      baseDirName,
      "interface/dayresult/day_result00.txb",
      listOf(
        TexConversion(19, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(20, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(23, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
        TexConversion(24, ConversionType.SWIZZLED_4BPP_PALETTED_LINEAR_CCT),
      ),
      copyToCustomPakFiles = copyToCustomPakFiles,
    )
  }
}
