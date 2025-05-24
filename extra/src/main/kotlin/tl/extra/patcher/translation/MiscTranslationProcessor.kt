package tl.extra.patcher.translation

import com.google.common.hash.Hashing
import kio.util.WINDOWS_932
import kio.util.child
import kio.util.readJson
import kio.util.writeJson
import tl.extra.patcher.file.ExtraChrDatFilePatcher
import tl.extra.patcher.file.ExtraFixedSizeTextBinFilePatcher
import tl.extra.patcher.file.ExtraIndexedTextBinFilePatcher
import tl.extra.patcher.file.ExtraInfoMatrixBinDFilePatcher
import tl.extra.patcher.file.ExtraInfoMatrixBinTFilePatcher
import tl.extra.patcher.file.ExtraItemParam01BinFilePatcher
import tl.extra.patcher.file.ExtraItemParam04BinFilePatcher
import tl.extra.patcher.file.ExtraSjisFilePatcher
import java.io.File
import java.nio.charset.Charset

internal class MiscTranslationProcessor(
  private val pakExtract: File,
  private val srcDir: File,
  unitDir: File,
  outDir: File,
) {
  private val patchedOut = outDir.child("patched")
  private val translationFile = unitDir.child("script-translation.txt")
  private val translationHashFile = outDir.child("hash.json")
  private val translation = ExtraTranslation(unitDir.child("script-japanese.txt"))
  private val translationOrig = ExtraTranslation(
    unitDir.child("script-japanese.txt"),
    unitDir.child("script-translation-orig.txt"),
  )

  init {
    if (isPatchingNeeded()) {
      patchFiles()
    } else {
      println("Misc patching not needed")
    }
  }

  private fun patchFiles() {
    patchedOut.mkdir()

    patchSjisFile("paramData/chaDataTbl.sjis", 0)
    patchItemParam01File("cmn/item_param_01.bin", 920)
    patchItemParam04File("cmn/item_param_04.bin", 1212)

    patchExtendedTextBinFile("interface/nameentry/msg.bin", 1609)
    patchExtendedTextBinFile("interface/select/i_move.bin", 1675)

    patchTextBinFile("interface/charsel/svt_select.bin", 1368)
    patchTextBinFile("interface/cmn/dialog.bin", 1369)
    patchTextBinFile("interface/dayresult/d_result.bin", 1371)
    patchTextBinFile("interface/dungeon/i_dun_sysmsg.bin", 1372)
    patchTextBinFile("interface/equip/msg.bin", 1373)
    patchTextBinFile("interface/gameover/gov.bin", 1376)
    patchTextBinFile("interface/gradationair/i_ga.bin", 1377)
    patchTextBinFile("interface/infomatpop/msg.bin", 1391)
    patchTextBinFile("interface/infomatrixex/msg.bin", 1392)
    patchTextBinFile("interface/item/msg.bin", 1450)
    patchTextBinFile("interface/mainmenu/help.bin", 1451)
    patchTextBinFile("interface/modeselect/modeselect.bin", 1456)
    patchTextBinFile("interface/option/msg.bin", 1460)
    patchTextBinFile("interface/save/msg.bin", 1467)
    patchTextBinFile("interface/shop/msg.bin", 1476)
    patchTextBinFile("interface/status/msg.bin", 1488)
    patchTextBinFile("battle/interface/btl_msg.bin", 1489)
    patchTextBinFile("cmn/cmn_name.bin", 1495)

    patchInfomatrixFiles("interface/infomatrixex/infomatrix_alc_d_04.bin", 1682)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_ali_d_03.bin", 1709)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_eld_d_01.bin", 1736)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_emi_d_00.bin", 1763)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_fun_d_05.bin", 1790)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_gaw_d_07.bin", 1817)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_koo_d_06.bin", 1844)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_ner_d_00.bin", 1871)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_rob_d_02.bin", 1898)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_ryo_d_06.bin", 1925)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_tam_d_00.bin", 1952)
    patchInfomatrixFiles("interface/infomatrixex/infomatrix_war_d_04.bin", 1979)

    patchInDungeonFile("chr/emi/0000.dat", 2006, 102)
    patchInDungeonFile("chr/gat/0000.dat", 2108, 11)
    patchInDungeonFile("chr/kun/0000.dat", 2119, 11)
    patchInDungeonFile("chr/mal/0000.dat", 2130, 7)
    patchInDungeonFile("chr/ner/0000.dat", 2137, 106)
    patchInDungeonFile("chr/sin/0000.dat", 2243, 10)
    patchInDungeonFile("chr/tam/0000.dat", 2253, 96)

    saveHash()
  }

  private fun isPatchingNeeded(): Boolean {
    if (!translationHashFile.exists()) {
      return true
    }
    val prevHash = translationHashFile.readJson<ByteArray>()
    val currentHash = getHashOfCurrent()
    return !prevHash.contentEquals(currentHash)
  }

  private fun saveHash() {
    translationHashFile.writeJson(getHashOfCurrent())
  }

  private fun getHashOfCurrent(): ByteArray {
    return Hashing.sha256().hashBytes(translationFile.readBytes()).asBytes()
  }

  private fun patchSjisFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(
      path,
      translationOffset,
      charset,
      handler = { bytes, file, translation, offset, hCharset ->
        ExtraSjisFilePatcher(
          bytes,
          srcDir.child("chaDataTbl-jp.sjis").readBytes(),
          file,
          translation,
          offset,
          hCharset,
        )
      },
    )
  }

  private fun patchItemParam01File(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::ExtraItemParam01BinFilePatcher)
  }

  private fun patchItemParam04File(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::ExtraItemParam04BinFilePatcher)
  }

  private fun patchTextBinFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::ExtraIndexedTextBinFilePatcher)
  }

  private fun patchExtendedTextBinFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::ExtraFixedSizeTextBinFilePatcher)
  }

  private fun patchInfomatrixFiles(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::ExtraInfoMatrixBinDFilePatcher)
    patchFile(path.replace("_d_", "_t_"), translationOffset, charset, ::ExtraInfoMatrixBinTFilePatcher)
  }

  private fun patchInDungeonFile(
    path: String,
    translationOffset: Int,
    count: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(
      path,
      translationOffset,
      charset,
      handler = { bytes, file, translation, offset, hCharset ->
        ExtraChrDatFilePatcher(bytes, file, translation, translationOrig, offset, count, hCharset)
      },
    )
  }

  private fun patchFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
    handler: (ByteArray, File, ExtraTranslation, Int, Charset) -> Any?,
  ) {
    println("Patching $path...")
    val srcFile = pakExtract.child(path)
    val outFile = patchedOut.child(path)
    outFile.parentFile.mkdirs()
    handler(srcFile.readBytes(), outFile, translation, translationOffset, charset)
  }
}
