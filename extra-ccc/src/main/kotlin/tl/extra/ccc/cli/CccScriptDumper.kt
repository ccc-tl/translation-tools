package tl.extra.ccc.cli

import kio.KioInputStream
import kio.util.WINDOWS_932
import kio.util.appendLine
import kio.util.child
import kio.util.readJson
import kio.util.writeJson
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.cccUnpack
import tl.extra.ccc.file.InfoMatrixDBinFile
import tl.extra.ccc.file.InfoMatrixTBinFile
import tl.extra.ccc.file.ItemParam01BinFile
import tl.extra.ccc.file.ItemParam04BinFile
import tl.extra.ccc.patcher.translation.ChrDatEntryDescriptor
import tl.extra.fateOutput
import tl.extra.file.FixedSizeTextBinFile
import tl.extra.file.IndexedTextBinFile
import tl.extra.file.SjisFile
import tl.extra.util.parseCccSoundTable
import tl.util.ScriptEditorEntry
import tl.util.ScriptEditorFilesWriter
import tl.util.readDatString
import java.io.File
import java.nio.charset.Charset

fun main() {
  CccScriptDumper(
    cccUnpack,
    cccToolkit.child("src/NPJH50505.BIN"),
    cccToolkit.child("src/translation/eboot/pointers.json"),
    fateOutput.child("ccc-script-v3"),
  )
  println("Done")
}

@Suppress("SameParameterValue")
class CccScriptDumper(
  private val srcDir: File,
  private val ebootFile: File,
  private val ebootPointersFile: File,
  private val outDir: File,
) {
  private val soundTable = parseCccSoundTable(ebootFile)
  private val entries = mutableListOf<Entry>()
  private val tocEntries = mutableListOf<String>()

  init {
    outDir.mkdir()
    translationUnit("eboot") {
      processEbootFile()
    }
    translationUnit("items") {
      processItemParam01BinFile("cmn/item_param_01.bin")
      processItemParam04BinFile("cmn/item_param_04.bin")
    }
    translationUnit("sg", keepNotes = true) {
      processSgBinFile("interface/sg/0000.bin")
      processSgNamedBinFile("interface/sg/sg_eli.bin")
      processSgTextBinFile("interface/sg/sg_eli_text1.bin")
      processSgTextBinFile("interface/sg/sg_eli_text2.bin")
      processSgTextBinFile("interface/sg/sg_eli_text3.bin")
      processSgTextBinFile("interface/sg/sg_eli_text4.bin")

      processSgNamedBinFile("interface/sg/sg_emi.bin")
      processSgTextBinFile("interface/sg/sg_emi_text1.bin")
      processSgTextBinFile("interface/sg/sg_emi_text2.bin")
      processSgTextBinFile("interface/sg/sg_emi_text3.bin")
      processSgTextBinFile("interface/sg/sg_emi_text4.bin")

      processSgNamedBinFile("interface/sg/sg_gil.bin")
      processSgTextBinFile("interface/sg/sg_gil_text1.bin")
      processSgTextBinFile("interface/sg/sg_gil_text2.bin")
      processSgTextBinFile("interface/sg/sg_gil_text3.bin")
      processSgTextBinFile("interface/sg/sg_gil_text4.bin")

      processSgNamedBinFile("interface/sg/sg_kia.bin")
      processSgTextBinFile("interface/sg/sg_kia_text1.bin")
      processSgTextBinFile("interface/sg/sg_kia_text2.bin")
      processSgTextBinFile("interface/sg/sg_kia_text3.bin")
      processSgTextBinFile("interface/sg/sg_kia_text4.bin")
      processSgTextBinFile("interface/sg/sg_kia_textEx.bin")

      processSgNamedBinFile("interface/sg/sg_ksk.bin")
      processSgTextBinFile("interface/sg/sg_ksk_text1.bin")
      processSgTextBinFile("interface/sg/sg_ksk_text2.bin")
      processSgTextBinFile("interface/sg/sg_ksk_text3.bin")
      processSgTextBinFile("interface/sg/sg_ksk_text4.bin")

      processSgNamedBinFile("interface/sg/sg_mll.bin")
      processSgTextBinFile("interface/sg/sg_mll_text1.bin")
      processSgTextBinFile("interface/sg/sg_mll_text2.bin")
      processSgTextBinFile("interface/sg/sg_mll_text3.bin")
      processSgTextBinFile("interface/sg/sg_mll_text4.bin")

      processSgNamedBinFile("interface/sg/sg_ner.bin")
      processSgTextBinFile("interface/sg/sg_ner_text1.bin")
      processSgTextBinFile("interface/sg/sg_ner_text2.bin")
      processSgTextBinFile("interface/sg/sg_ner_text3.bin")
      processSgTextBinFile("interface/sg/sg_ner_text4.bin")

      processSgNamedBinFile("interface/sg/sg_psl.bin")
      processSgTextBinFile("interface/sg/sg_psl_text1.bin")
      processSgTextBinFile("interface/sg/sg_psl_text2.bin")
      processSgTextBinFile("interface/sg/sg_psl_text3.bin")
      processSgTextBinFile("interface/sg/sg_psl_text4.bin")

      processSgNamedBinFile("interface/sg/sg_ran.bin")
      processSgTextBinFile("interface/sg/sg_ran_text1.bin")
      processSgTextBinFile("interface/sg/sg_ran_text2.bin")
      processSgTextBinFile("interface/sg/sg_ran_text3.bin")
      processSgTextBinFile("interface/sg/sg_ran_text4.bin")

      processSgNamedBinFile("interface/sg/sg_rin.bin")
      processSgTextBinFile("interface/sg/sg_rin_text1.bin")
      processSgTextBinFile("interface/sg/sg_rin_text2.bin")
      processSgTextBinFile("interface/sg/sg_rin_text3.bin")
      processSgTextBinFile("interface/sg/sg_rin_text4.bin")

      processSgNamedBinFile("interface/sg/sg_tam.bin")
      processSgTextBinFile("interface/sg/sg_tam_text1.bin")
      processSgTextBinFile("interface/sg/sg_tam_text2.bin")
      processSgTextBinFile("interface/sg/sg_tam_text3.bin")
      processSgTextBinFile("interface/sg/sg_tam_text4.bin")

      processSgNamedBinFile("interface/sg/sg_zin.bin")
      processSgTextBinFile("interface/sg/sg_zin_text1.bin")
      processSgTextBinFile("interface/sg/sg_zin_text2.bin")
      processSgTextBinFile("interface/sg/sg_zin_text3.bin")
      processSgTextBinFile("interface/sg/sg_zin_text4.bin")
    }
    translationUnit("infomatrix", keepNotes = true) {
      processIndexedTextBinFile("interface/infomatrixex/msg.bin")

      processInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_01.bin")
      processInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_02.bin")
      processInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_03.bin")
      processInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_04.bin")
      processInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_05.bin")
      processInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_06.bin")
      processInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_07.bin")
      processInfoMatrixDBinFile("interface/infomatrixex/infomatrix_d_08.bin")

      processInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_01.bin")
      processInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_02.bin")
      processInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_03.bin")
      processInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_04.bin")
      processInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_05.bin")
      processInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_06.bin")
      processInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_07.bin")
      processInfoMatrixTBinFile("interface/infomatrixex/infomatrix_t_08.bin")
    }
    translationUnit("misc", keepNotes = true) {
      processIndexedTextBinFile("interface/item/msg.bin")
      processIndexedTextBinFile("interface/mainmenu/help.bin")
      processIndexedTextBinFile("interface/modeselect/modeselect.bin")
      processIndexedTextBinFile("interface/myroom/msg.bin")
      processFixedSizeTextBinFile("interface/nameentry/msg.bin")
      processIndexedTextBinFile("interface/option/msg.bin")
      processIndexedTextBinFile("interface/osioki/msg.bin")
      processIndexedTextBinFile("interface/save/msg.bin")
      processFixedSizeTextBinFile("interface/select/i_move.bin")
      processIndexedTextBinFile("interface/shop/msg.bin")
      processIndexedTextBinFile("interface/status/msg.bin")
      processIndexedTextBinFile("interface/svtselect/msg.bin")
      processIndexedTextBinFile("interface/title/msg.bin")
      processIndexedTextBinFile("interface/charsel/svt_select.bin")
      processIndexedTextBinFile("interface/cmn/dialog.bin")
      processIndexedTextBinFile("interface/dayresult/d_result.bin")
      processIndexedTextBinFile("interface/dungeon/i_dun_sysmsg.bin")
      processIndexedTextBinFile("interface/dungeon/msg.bin")
      processIndexedTextBinFile("interface/equip/msg.bin")
      processIndexedTextBinFile("interface/gallery/msg.bin")
      processIndexedTextBinFile("interface/gameover/gov.bin")
      processIndexedTextBinFile("battle/interface/btl_msg.bin")
      processIndexedTextBinFile("interface/cmn/install.bin", Charsets.UTF_8)
      processDungeonSelectBinFile("interface/dungeonselect/0000.bin")
      processChaDataTblFile("paramData/chaDataTbl.sjis")
    }
    translationUnit("indungeon", keepNotes = true) { outDir ->
      val jsonOut = outDir.child("json")
      jsonOut.mkdir()
      processChrDatFile(jsonOut, "chr/el2/0000.dat", 0xB78, 10)
      processChrDatFile(jsonOut, "chr/eli/0000.dat", 0xB78, 10)
      processChrDatFile(jsonOut, "chr/elp/0000.dat", 0xCE8, 14)
      processChrDatFile(jsonOut, "chr/emi/0000.dat", 0x2A04, 89)
      processChrDatFile(jsonOut, "chr/emi/0000_ex.dat", 0x2A04, 89)
      processChrDatFile(jsonOut, "chr/emi3/0000.dat", 0x2A04, 89)
      processChrDatFile(jsonOut, "chr/gat/0000.dat", 0x1234, 11)
      processChrDatFile(jsonOut, "chr/gil/0000.dat", 0x2710, 83)
      processChrDatFile(jsonOut, "chr/gil/0000_ex.dat", 0x2710, 83)
      processChrDatFile(jsonOut, "chr/mal/0000.dat", 0xD80, 7)
      processChrDatFile(jsonOut, "chr/mll/0000.dat", 0x13AC, 3)
      processChrDatFile(jsonOut, "chr/ner/0000.dat", 0x27F4, 96)
      processChrDatFile(jsonOut, "chr/ner/0000_ex.dat", 0x27F4, 96)
      processChrDatFile(jsonOut, "chr/sin/0000.dat", 0x1184, 10)
      processChrDatFile(jsonOut, "chr/tam/0000.dat", 0x1CF8, 86)
      processChrDatFile(jsonOut, "chr/tam/0000_ex.dat", 0x1CF8, 86)
      processChrDatFile(jsonOut, "chr/tam3/0000.dat", 0x1CB8, 96)
    }
  }

  private fun processEbootFile() {
    println("Processing EBOOT")
    addTocEntry("EBOOT")
    with(KioInputStream(ebootFile)) {
      ebootPointersFile.readJson<List<EbootPointerEntry>>().forEach { entry ->
        setPos(entry.p)
        entries.add(Entry(String(readBytes(entry.s), Charsets.WINDOWS_932), "EBOOT"))
      }
      close()
    }
  }

  private fun processChrDatFile(jsonOut: File, path: String, startAddr: Int, textCount: Int) {
    println("Processing $path")
    val debug = false
    addTocEntry(path)
    val file = File(srcDir, path)
    val descriptors = mutableMapOf<Int, MutableList<ChrDatEntryDescriptor>>()
    with(KioInputStream(file)) {
      setPos(startAddr)
      repeat(textCount) {
        val textPtr = pos()
        val text = readDatString()
        var matched = 0
        temporaryJump(0) {
          while (!eof()) {
            val descriptorPtr = pos()
            if (readInt() == textPtr) {
              val unkValue = readInt()
              val soundPtr = readInt()
              val speakerId = readInt()
              var soundId = -1
              var soundPath = ""
              var likelyFalsePositive = false

              if (soundPtr != -1) {
                if (soundPtr > 0 && soundPtr < file.length() - 0x4) {
                  temporaryJump(soundPtr) {
                    soundId = readInt()
                    when (soundId) {
                      in soundTable.indices -> {
                        soundPath = soundTable[soundId]
                        likelyFalsePositive = false
                      }
                      -1 -> likelyFalsePositive = false
                      else -> likelyFalsePositive = true
                    }
                  }
                } else {
                  soundId = -1
                  likelyFalsePositive = true
                }
              } else {
                soundId = -1
                likelyFalsePositive = false
              }
              likelyFalsePositive = likelyFalsePositive || speakerId < 0 || speakerId > 152
              val descriptorList = descriptors.getOrPut(textPtr) { mutableListOf() }
              descriptorList.add(
                ChrDatEntryDescriptor(
                  descriptorPtr = descriptorPtr,
                  text = text,
                  textPtr = textPtr,
                  unkValue = unkValue,
                  speakerId = speakerId,
                  soundPtr = soundPtr,
                  soundId = soundId,
                  soundPath = soundPath,
                  likelyFalsePositive = likelyFalsePositive,
                ),
              )
              matched++
            }
          }
        }
        val note = if (matched != 0) {
          path
        } else {
          if (debug) {
            println("Not found: '${text.replace("\n", "\\n")}'")
          }
          "$path\n(very likely to be unused)"
        }
        val audioPaths = descriptors[textPtr]
          ?.asSequence()
          ?.filter { !it.likelyFalsePositive }
          ?.filter { it.soundPath.isNotBlank() }
          ?.map { it.soundPath }
          ?.distinct()
          ?.joinToString(separator = "\n") ?: ""
        entries.add(Entry(text, note, audio = audioPaths))
      }
    }
    jsonOut.child("${path.replace("/", "$")}.json").writeJson(descriptors)

    if (debug) {
      descriptors.forEach { (textPtr, entries) ->
        println("Text ptr $textPtr mapped to:")
        entries.forEach { println(it) }
        println("---")
      }
      descriptors.filterValues { it.size > 1 }
        .values
        .map { it.filter { desc -> !desc.likelyFalsePositive } }
        .forEach { textDesc ->
          val base = textDesc[0]
          textDesc.drop(1)
            .filter {
              it.textPtr != base.textPtr ||
                it.soundPtr != base.soundPtr ||
                it.speakerId != base.speakerId ||
                it.unkValue != base.unkValue
            }
            .forEach {
              println("--- Descriptor mismatch found ---")
              println(base)
              println(it)
              println("---")
            }
        }
    }
  }

  private fun processIndexedTextBinFile(path: String, charset: Charset = Charsets.WINDOWS_932) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(srcDir, path)
    IndexedTextBinFile(file, charset).entries.forEach {
      entries.add(Entry(it.second, path))
    }
  }

  private fun processSgBinFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(srcDir, path)
    with(KioInputStream(file)) {
      while (true) {
        entries.add(Entry(readDatString(maintainStreamPos = true), path))
        skip(0x60)
        if (eof()) {
          break
        }
      }
    }
  }

  private fun processSgNamedBinFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(this.srcDir, path)
    with(KioInputStream(file)) {
      while (true) {
        readInt()
        entries.add(Entry((readDatString()), path))
        skipNullBytes()
        if (eof()) {
          break
        }
      }
    }
  }

  private fun processSgTextBinFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(this.srcDir, path)
    with(KioInputStream(file)) {
      while (true) {
        entries.add(Entry(readDatString(), path))
        skipNullBytes()
        if (eof()) {
          break
        }
      }
    }
  }

  private fun processDungeonSelectBinFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(this.srcDir, path)
    with(KioInputStream(file)) {
      while (true) {
        entries.add(Entry(readDatString(maintainStreamPos = true), path))
        skip(0x30)
        entries.add(Entry(readDatString(maintainStreamPos = true), path))
        skip(0x60)
        if (eof()) {
          break
        }
      }
    }
  }

  private fun processFixedSizeTextBinFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(this.srcDir, path)
    FixedSizeTextBinFile(file).entries.forEach {
      entries.add(Entry(it.second, path))
    }
  }

  private fun processItemParam01BinFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(this.srcDir, path)
    ItemParam01BinFile(file).entries.forEach {
      entries.add(Entry(it.name, path))
      entries.add(Entry(it.text1, path))
      entries.add(Entry(it.text2, path))
      entries.add(Entry(it.text3, path))
    }
  }

  private fun processItemParam04BinFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(this.srcDir, path)
    ItemParam04BinFile(file).entries.forEach {
      entries.add(Entry(it.name, path))
      entries.add(Entry(it.text1, path))
      entries.add(Entry(it.text2, path))
    }
  }

  private fun processChaDataTblFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(this.srcDir, path)
    SjisFile(file).entries.forEach {
      entries.add(Entry(it.l1, path))
      entries.add(Entry(it.l2, path))
      entries.add(Entry(it.l3, path))
      entries.add(Entry(it.l4, path))
      entries.add(Entry(it.l5, path))
    }
  }

  private fun processInfoMatrixDBinFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(this.srcDir, path)
    InfoMatrixDBinFile(file).entries.forEach {
      entries.add(Entry(it, path))
    }
  }

  private fun processInfoMatrixTBinFile(path: String) {
    println("Processing $path")
    addTocEntry(path)
    val file = File(this.srcDir, path)
    InfoMatrixTBinFile(file).entries.forEach {
      entries.add(Entry(it, path))
    }
  }

  private fun translationUnit(name: String, keepNotes: Boolean = false, createEntries: (File) -> Unit) {
    entries.clear()
    tocEntries.clear()
    val unitOutDir = outDir.child(name)
    unitOutDir.mkdir()
    createEntries(unitOutDir)
    val editorEntries = mutableListOf<ScriptEditorEntry>()
    entries.forEach {
      editorEntries.add(
        ScriptEditorEntry(
          jp = it.jp,
          en = "",
          note = if (keepNotes) it.note else "",
          portrait = it.portrait,
          audio = it.audio,
        ),
      )
    }
    ScriptEditorFilesWriter(editorEntries).writeTo(unitOutDir)
    val tocOut = StringBuilder()
    tocEntries.forEach {
      tocOut.appendLine(it, newLine = "\r\n")
    }
    unitOutDir.child("toc.txt").writeText(tocOut.toString())
  }

  private fun addTocEntry(path: String) {
    tocEntries.add("${entries.size + 1} - $path")
  }

  private class Entry(val jp: String, val note: String, val portrait: String = "", val audio: String = "")

  private data class EbootPointerEntry(val p: Int, val s: Int)
}
