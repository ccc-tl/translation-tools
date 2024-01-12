@file:Suppress("UNUSED_VARIABLE")

package tl.extra.ccc.cli

import kio.KioInputStream
import kio.util.child
import kio.util.relativizePath
import kio.util.walkDir
import kio.util.writeJson
import tl.extra.ccc.cccUnpack
import tl.extra.ccc.patcher.translation.CccDatEntry
import tl.extra.fateOutput
import tl.util.readDatString
import java.io.EOFException
import java.io.File

fun main() {
  val scriptOutDir = File(fateOutput, "ccc_script_dat_v2").apply { mkdir() }
  val baseDir = cccUnpack

  walkDir(baseDir.child("field_new")) { scriptFile ->
    if (scriptFile.name == "0000.dat" || scriptFile.name == "1000.dat") {
      val relPath = scriptFile.relativizePath(baseDir)
      println("Processing $relPath...")
      val flatRelPath = relPath.replace("/", "$")
      val outFile = scriptOutDir.child("$flatRelPath.json")
      ScriptDatFileV2(scriptFile, outFile)
    }
  }
  println("Done")
}

private class ScriptDatFileV2(
  private val scriptFile: File,
  outFile: File,
) {
  private val entries = mutableListOf<CccDatEntry>()

  init {
    processFile()
    outFile.writeJson(entries)
  }

  private fun processFile() {
    with(KioInputStream(scriptFile.readBytes())) {
      while (!eof()) {
        handleOpcode(readInt())
      }
    }
  }

  private fun KioInputStream.handleOpcode(op: Int) {
    when (op) {
      0x00003926 -> { // main dialog text
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("text", ptrLoc, textLoc)
      }
      0x00000126 -> { // set text motion
        readInt()
        readInt()
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("motion", ptrLoc, textLoc)
      }
      0x00001530 -> { // backlog
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("backlog", ptrLoc, textLoc)
      }
      0x00000A26 -> { // locations and characters names, in (almost?) every .dat
        readInt()
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("locationOrName", ptrLoc, textLoc)
      }
      0x00000030 -> { // locations and characters names
        val unk1 = readInt()
        val unk2 = readInt()
        val ptrLoc = pos()
        val textLoc = readInt()
        if (unk1 != 0 || unk2 !in listOf(0, 1, 2)) {
          return
        }
        if (scriptFile.parentFile.name == "068" && textLoc == 512) {
          return
        }
        createEntryAt("locationOrName2", ptrLoc, textLoc)
      }
      0x00000102 -> { // special memory update case
        readInt()
        readInt()
        val mode = readInt()
        val ptrLoc = pos()
        val textLoc = readInt()
        if (mode != 0) {
          return
        }
        createEntryAt("memoryWrite", ptrLoc, textLoc, acceptOnly = listOf("？？？", "藤村 大河", "ランサー", "アーチャー"))
      }
      0x00000A30 -> { // player choices, can yield some false positives
        readInt()
        readInt()
        readInt()
        readInt()
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("choice", ptrLoc, textLoc)
      }
      0x00005030 -> { // in dungeon dialogue
        readInt()
        val id = readInt()
        readInt()
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("inDungeon", ptrLoc, textLoc)
      }
      0x00005E16 -> { // yes / no popup, doesn't seem to be used in game, exists only in code
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("popupYesNo", ptrLoc, textLoc)
      }
      0x00005F16 -> { // yes / no popup with two lines of text, used mainly for SG texts
        val ptrLoc = pos()
        val textLoc = readInt()
        val ptrLoc2 = pos()
        val textLoc2 = readInt()
        createEntryAt("popupYesNo_line1", ptrLoc, textLoc)
        createEntryAt("popupYesNo_line2", ptrLoc2, textLoc2)
      }
      0x00006016 -> { // popup without buttons, used in game files
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("popupStd", ptrLoc, textLoc)
      }
      0x00006116 -> { // popup without buttons with two lines of text, used in game files
        val ptrLoc = pos()
        val textLoc = readInt()
        val ptrLoc2 = pos()
        val textLoc2 = readInt()
        createEntryAt("popupStd_line1", ptrLoc, textLoc)
        createEntryAt("popupStd_line2", ptrLoc2, textLoc2)
      }
      0x00006416 -> { // only used for save before quit texts
        val ptrLoc = pos()
        val textLoc = readInt()
        val ptrLoc2 = pos()
        val textLoc2 = readInt()
        createEntryAt("popupSaveBeforeQuit_line1", ptrLoc, textLoc)
        createEntryAt("popupSaveBeforeQuit_line2", ptrLoc2, textLoc2)
      }
      0x00005316 -> { // mainly used for save data texts, also for some other misc prompts
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("unk_op1653", ptrLoc, textLoc)
      }
      0x00005016 -> { // mainly used for save clear data details, also for some other misc prompts
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("unk_op1650", ptrLoc, textLoc)
      }
      0x00005916 -> { // occurs in game files, purpose unknown, maybe even some debug prompts
        readInt()
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("unk_op1659", ptrLoc, textLoc)
      }
      0x00006316 -> { // unused probably
        readInt()
        val ptrLoc = pos()
        val textLoc = readInt()
        createEntryAt("unk_op1663", ptrLoc, textLoc)
      }
    }
  }

  private fun KioInputStream.createEntryAt(
    type: String,
    ptrLoc: Int,
    textLoc: Int,
    acceptOnly: List<String> = emptyList(),
  ) {
    if (textLoc == 0 || textLoc % 4 != 0) {
      return
    }
    val lastPos = pos()
    try {
      val text = readDatString(at = textLoc)
      if (text.isBlank() || text == "TEST") {
        return
      }
      if (acceptOnly.isNotEmpty() && text !in acceptOnly) {
        return
      }
      entries.add(CccDatEntry(type, ptrLoc, textLoc, text))
    } catch (e: EOFException) {
      println("EOF exception while reading string, ignored")
    } finally {
      setPos(lastPos)
    }
  }
}
