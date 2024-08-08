package tl.extra.cli

import com.google.gson.Gson
import kio.KioInputStream
import kio.util.appendLine
import kio.util.child
import tl.extra.extraJpPakUnpack
import tl.extra.extraUnpack
import tl.extra.fateOutput
import tl.extra.file.ExtraInfoMatrixBinFile
import tl.extra.file.ExtraItemParam01BinFile
import tl.extra.file.ExtraItemParam04BinFile
import tl.extra.file.FixedSizeTextBinFile
import tl.extra.file.IndexedTextBinFile
import tl.extra.file.SjisFile
import tl.util.ScriptEditorEntry
import tl.util.ScriptEditorFilesWriter
import tl.util.readDatString
import java.io.File

fun main() {
  ExtraMiscScriptDumper(
    File(fateOutput, "extra_script_v4").child("misc"),
    extraUnpack,
    extraJpPakUnpack,
  )
  println("Done")
}

class ExtraMiscScriptDumper(
  scriptDir: File,
  private val baseEn: File,
  private val baseJp: File,
) {
  private val entries = mutableListOf<TranslationEntry>()
  private val tocEntries = mutableListOf<String>()

  // see ExtraMainScriptDumper.kt for docs
  private val combineOutputJpWithNotesAndOutputJpAsEn = false

  init {
    scriptDir.mkdir()

    processFile("paramData/chaDataTbl.sjis") { relPath, jpFile, enFile ->
      addTocEntry(relPath, "skills info")
      val jp = SjisFile(jpFile).entries
      val en = SjisFile(enFile).entries
      if (jp.size != en.size) {
        error("$relPath entry count mismatch: ${jp.size} != ${en.size}")
      }
      jp.forEachIndexed { index, jpEntry ->
        val enEntry = en[index]
        if (jpEntry.isUnused() && enEntry.isUnused()) return@forEachIndexed
        entries.add(
          TranslationEntry(
            jpEntry.l1,
            enEntry.l1,
            "$relPath\nskill index $index, skill line 1",
            relPath,
            index
          )
        )
        entries.add(
          TranslationEntry(
            jpEntry.l2,
            enEntry.l2,
            "$relPath\nskill index $index, skill line 2",
            relPath,
            index
          )
        )
        entries.add(
          TranslationEntry(
            jpEntry.l3,
            enEntry.l3,
            "$relPath\nskill index $index, skill line 3",
            relPath,
            index
          )
        )
        entries.add(
          TranslationEntry(
            jpEntry.l4,
            enEntry.l4,
            "$relPath\nskill index $index, skill line 4",
            relPath,
            index
          )
        )
        entries.add(
          TranslationEntry(
            jpEntry.l5,
            enEntry.l5,
            "$relPath\nskill index $index, skill line 5",
            relPath,
            index
          )
        )
      }
    }

    processFile("cmn/item_param_01.bin") { relPath, jpFile, enFile ->
      addTocEntry(relPath, "items")
      val jp = ExtraItemParam01BinFile(jpFile, true).entries
      val en = ExtraItemParam01BinFile(enFile, false).entries
      if (jp.size != en.size) {
        error("$relPath entry count mismatch: ${jp.size} != ${en.size}")
      }
      jp.forEachIndexed { index, jpEntry ->
        val enEntry = en[index]
        if (jpEntry.isUnused() && enEntry.isUnused()) {
          return@forEachIndexed
        }
        entries.add(
          TranslationEntry(
            jpEntry.name,
            enEntry.name,
            "$relPath\nitem index $index, item name",
            relPath,
            index
          )
        )
        entries.add(
          TranslationEntry(
            jpEntry.description,
            enEntry.description,
            "$relPath\nitem index $index, item description",
            relPath,
            index
          )
        )
        entries.add(
          TranslationEntry(
            jpEntry.trivia1,
            enEntry.trivia1,
            "$relPath\nitem index $index, item trivia 1",
            relPath,
            index
          )
        )
        entries.add(
          TranslationEntry(
            jpEntry.trivia2,
            enEntry.trivia2,
            "$relPath\nitem index $index, item trivia 2",
            relPath,
            index
          )
        )
      }
    }

    processFile("cmn/item_param_04.bin") { relPath, jpFile, enFile ->
      addTocEntry(relPath, "items")
      val jp = ExtraItemParam04BinFile(jpFile, true).entries
      val en = ExtraItemParam04BinFile(enFile, false).entries
      if (jp.size != en.size) {
        error("$relPath entry count mismatch: ${jp.size} != ${en.size}")
      }
      jp.forEachIndexed { index, jpEntry ->
        val enEntry = en[index]
        if (jpEntry.isUnused() && enEntry.isUnused()) {
          return@forEachIndexed
        }
        entries.add(
          TranslationEntry(
            jpEntry.name,
            enEntry.name,
            "$relPath\nitem index $index, item name",
            relPath,
            index
          )
        )
        entries.add(
          TranslationEntry(
            jpEntry.description,
            enEntry.description,
            "$relPath\nitem index $index, item description",
            relPath,
            index
          )
        )
        entries.add(
          TranslationEntry(
            jpEntry.trivia,
            enEntry.trivia,
            "$relPath\nitem index $index, item trivia",
            relPath,
            index
          )
        )
      }
    }

    arrayOf(
      "interface/charsel/svt_select.bin",
      "interface/cmn/dialog.bin",
      "interface/dayresult/d_result.bin",
      "interface/dungeon/i_dun_sysmsg.bin",
      "interface/equip/msg.bin",
      "interface/gameover/gov.bin",
      "interface/gradationair/i_ga.bin",
      "interface/infomatpop/msg.bin",
      "interface/infomatrixex/msg.bin",
      "interface/item/msg.bin",
      "interface/mainmenu/help.bin",
      "interface/modeselect/modeselect.bin",
      "interface/option/msg.bin",
      "interface/save/msg.bin",
      "interface/shop/msg.bin",
      "interface/status/msg.bin",
      "battle/interface/btl_msg.bin",
      "cmn/cmn_name.bin"
    ).forEach { processFile(it, this::indexedTextBinHandler) }

    arrayOf(
      "interface/nameentry/msg.bin",
      "interface/select/i_move.bin"
    ).forEach { processFile(it, this::fixedSizeTextBinHandler) }

    arrayOf(
      "interface/infomatrixex/infomatrix_alc_d_04.bin",
      "interface/infomatrixex/infomatrix_ali_d_03.bin",
      "interface/infomatrixex/infomatrix_eld_d_01.bin",
      "interface/infomatrixex/infomatrix_emi_d_00.bin",
      "interface/infomatrixex/infomatrix_fun_d_05.bin",
      "interface/infomatrixex/infomatrix_gaw_d_07.bin",
      "interface/infomatrixex/infomatrix_koo_d_06.bin",
      "interface/infomatrixex/infomatrix_ner_d_00.bin",
      "interface/infomatrixex/infomatrix_rob_d_02.bin",
      "interface/infomatrixex/infomatrix_ryo_d_06.bin",
      "interface/infomatrixex/infomatrix_tam_d_00.bin",
      "interface/infomatrixex/infomatrix_war_d_04.bin"
    ).forEach { processFile(it, this::infoMatrixBinHandler) }

    arrayOf(
      ChrDatDesc("chr/emi/0000.dat", 0x2858, 0x283C, 102),
      ChrDatDesc("chr/gat/0000.dat", 0x1258, 0x1258, 11),
      ChrDatDesc("chr/kun/0000.dat", 0x124C, 0x124C, 11),
      ChrDatDesc("chr/mal/0000.dat", 0xD90, 0xD90, 7),
      ChrDatDesc("chr/ner/0000.dat", 0x2A08, 0x29EC, 106),
      ChrDatDesc("chr/sin/0000.dat", 0x130C, 0x130C, 10),
      ChrDatDesc("chr/tam/0000.dat", 0x1EA4, 0x1E88, 96)
    ).forEach { chrDesc ->
      processFile(chrDesc.relPath) { relPath, jpFile, enFile ->
        addTocEntry(relPath, "in-dungeon")

        val enTexts = mutableListOf<String>()
        with(KioInputStream(enFile)) {
          setPos(chrDesc.startEnAddr)
          repeat(chrDesc.textCount) {
            enTexts.add(readDatString())
          }
        }
        val jpTexts = mutableListOf<String>()
        with(KioInputStream(jpFile)) {
          setPos(chrDesc.startJpAddr)
          repeat(chrDesc.textCount) {
            jpTexts.add(readDatString())
          }
        }

        enTexts.forEachIndexed { index, enText ->
          entries.add(TranslationEntry(jpTexts[index], enText, relPath, relPath, index))
        }
      }
    }

    val csvOut = StringBuilder()
    val editorEntries = mutableListOf<ScriptEditorEntry>()
    entries.forEach {
      csvOut.appendLine("${it.en};${it.jp}\n")
      if (combineOutputJpWithNotesAndOutputJpAsEn) {
        editorEntries.add(ScriptEditorEntry(it.en, "", "${it.note}\n${it.jp}"))
      } else {
        editorEntries.add(ScriptEditorEntry(it.jp, it.en, it.note))
      }
    }
    val tocOut = StringBuilder()
    tocEntries.forEach {
      tocOut.appendLine(it, newLine = "\r\n")
    }
    scriptDir.child("toc.txt").writeText(tocOut.toString())
    scriptDir.child("text.csv").writeText(csvOut.toString())
    scriptDir.child("entries.json").writeText(Gson().toJson(entries))
    ScriptEditorFilesWriter(editorEntries).writeTo(scriptDir)
  }

  private fun addTocEntry(path: String) {
    tocEntries.add("${entries.size + 1} - $path")
  }

  private fun addTocEntry(path: String, note: String) {
    tocEntries.add("${entries.size + 1} - $path - $note")
  }

  private fun indexedTextBinHandler(relPath: String, jpFile: File, enFile: File) {
    addTocEntry(relPath)
    val jp = IndexedTextBinFile(jpFile).entries.map { it.second }
    val en = IndexedTextBinFile(enFile).entries.map { it.second }
    if (jp.size != en.size) {
      error("$relPath entry count mismatch: ${jp.size} != ${en.size}")
    }
    jp.forEachIndexed { index, jpText ->
      val enText = en[index]
      entries.add(TranslationEntry(jpText, enText, relPath, relPath, index))
    }
  }

  private fun fixedSizeTextBinHandler(relPath: String, jpFile: File, enFile: File) {
    addTocEntry(relPath)
    val jp = FixedSizeTextBinFile(jpFile).entries.map { it.second }
    val en = FixedSizeTextBinFile(enFile).entries.map { it.second }
    if (jp.size != en.size) {
      error("$relPath entry count mismatch: ${jp.size} != ${en.size}")
    }
    jp.forEachIndexed { index, jpText ->
      val enText = en[index]
      entries.add(TranslationEntry(jpText, enText, relPath, relPath, index))
    }
  }

  private fun infoMatrixBinHandler(relPath: String, jpDFile: File, enDFile: File) {
    addTocEntry(relPath)
    val jpTFile = File(baseJp, relPath.replace("_d_", "_t_"))
    val enTFile = File(baseEn, relPath.replace("_d_", "_t_"))

    val jp = ExtraInfoMatrixBinFile(jpDFile, jpTFile).entries
    val en = ExtraInfoMatrixBinFile(enDFile, enTFile).entries
    if (jp.size != en.size) {
      error("$relPath entry count mismatch: ${jp.size} != ${en.size}")
    }
    jp.forEachIndexed { index, jpEntry ->
      val enEntry = en[index]
      entries.add(
        TranslationEntry(
          jpEntry.title,
          enEntry.title,
          "$relPath\nentry index $index, title",
          relPath,
          index
        )
      )
      entries.add(
        TranslationEntry(
          jpEntry.abbreviation,
          enEntry.abbreviation,
          "$relPath\nentry index $index, abbreviation",
          relPath,
          index
        )
      )
      entries.add(
        TranslationEntry(
          jpEntry.text,
          enEntry.text,
          "$relPath\nentry index $index, text",
          relPath,
          index
        )
      )
    }
  }

  private fun processFile(path: String, handler: (relPath: String, jpFile: File, enFile: File) -> Unit) {
    println("Process $path")
    val jpFile = File(baseJp, path)
    val enFile = File(baseEn, path)
    handler(path, jpFile, enFile)
  }
}

private data class ChrDatDesc(
  val relPath: String,
  val startEnAddr: Int,
  val startJpAddr: Int,
  val textCount: Int
)

private data class TranslationEntry(
  val jp: String,
  val en: String,
  val note: String = "",
  val file: String,
  val fileOffset: Int,
)
