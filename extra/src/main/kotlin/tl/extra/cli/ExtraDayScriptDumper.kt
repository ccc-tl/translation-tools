package tl.extra.cli

import com.google.gson.Gson
import kio.KioInputStream
import kio.util.child
import kio.util.relativizePath
import kio.util.walkDir
import tl.extra.extraCpkUnpack
import tl.extra.extraJpCpkUnpack
import tl.extra.fateOutput
import tl.extra.patcher.file.CombinedDatEntry
import tl.extra.patcher.file.DatEntry
import tl.util.ScriptEditorEntry
import tl.util.ScriptEditorFilesWriter
import tl.util.readDatString
import java.io.EOFException
import java.io.File

fun main() {
  val outDir = File(fateOutput, "extra_script_v4").child("day")
  val baseEnDir = extraCpkUnpack
  val baseJpDir = extraJpCpkUnpack

  val mainEntries = mutableListOf<CombinedDatEntry>()
  val mainTextSet = mutableListOf<TextEntry>()
  val mainTextList = mutableListOf<TextEntry>()

  // see ExtraMainScriptDumper.kt for docs
  val combineOutputJpWithNotesAndOutputJpAsEn = true

  outDir.mkdir()
  walkDir(baseEnDir.child("day"), { _, e -> e.printStackTrace() }) {
    if (it.extension == "dat") {
      val dat = ExtraScriptDatDayFile(it.relativizePath(baseEnDir), baseEnDir, baseJpDir)
      mainEntries.addAll(dat.mainText)
    }
  }
  println("Parsing done, deduping...")

  mainEntries.forEach {
    val entry = TextEntry(it.enEntry.text, it.jpEntry.text)
    if (!mainTextSet.contains(entry)) {
      mainTextSet.add(entry)
      mainTextList.add(entry)
    }
  }
  val entries = mutableListOf<ScriptEditorEntry>()
  mainTextList.forEach {
    if (combineOutputJpWithNotesAndOutputJpAsEn) {
      entries.add(ScriptEditorEntry(it.enText, "", it.jpText))
    } else {
      entries.add(ScriptEditorEntry(it.enText, it.jpText))
    }
  }
  ScriptEditorFilesWriter(entries).writeTo(outDir)
  outDir.child("entries.json").writeText(Gson().toJson(mainEntries))
  println("Done")
}

private class ExtraScriptDatDayFile(val relPath: String, baseEnDir: File, baseJpDir: File) {
  val enFile = File(baseEnDir, relPath)
  val jpFile = File(baseJpDir, relPath)

  val enMainText = mutableListOf<DatEntry>()
  val jpMainText = mutableListOf<DatEntry>()
  val mainText = mutableListOf<CombinedDatEntry>()

  init {
    println("Process $relPath")
    if (enFile.length() != 0L) {
      processFileText()
      enMainText.forEachIndexed { index, enEntry ->
        mainText.add(CombinedDatEntry(relPath, enEntry, jpMainText.getOrElse(index) { DatEntry("", 0, 0) }))
      }
    }
  }

  private fun processFileText() {
    arrayOf(enFile to enMainText, jpFile to jpMainText).forEach {
      with(KioInputStream(it.first)) {
        while (!eof()) {
          val op = readInt()
          if (op == 0x00000126) { // poem?
            readInt()
            readInt()
            val ptrLoc = pos()
            val ptr = readInt()
            readStringAt(this, ptr, ptrLoc, it.second)
          }
          if (op == 0x00003926) { // main dialogue text
            val ptrLoc = pos()
            val ptr = readInt()
            readStringAt(this, ptr, ptrLoc, it.second)
          }
          if (op == 0x00000A26) { // locations and characters names, in (almost?) every .dat
            readInt()
            val count = pos()
            val ptr = readInt()
            if (ptr % 4 != 0) continue
            if (ptr == 0) continue
            readStringAt(this, ptr, count, it.second)
          }
          if (op == 0x00000A30) { // player choices, can yield some false positives
            readInt()
            readInt()
            readInt()
            readInt()
            val count = pos()
            val ptr = readInt()
            if (ptr % 4 != 0) continue
            if (ptr == 0) continue
            readStringAt(this, ptr, count, it.second)
          }
          if (op == 0x00005030) { // in dungeon dialogue
            readInt()
            readInt()
            readInt()
            val count = pos()
            val ptr = readInt()
            readStringAt(this, ptr, count, it.second)
          }
          if (op == 0x00005E16) {
            val count = pos()
            val ptr = readInt()
            readStringAt(this, ptr, count, it.second)
          }
          if (op == 0x00005F16) { // yes / no popup with two lines of text
            val count = pos()
            val ptr = readInt()
            val count2 = pos()
            val ptr2 = readInt()
            readStringAt(this, ptr, count, it.second)
            readStringAt(this, ptr2, count2, it.second)
          }
          if (op == 0x00006016) { // popup without buttons
            val count = pos()
            val ptr = readInt()
            readStringAt(this, ptr, count, it.second)
          }
          if (op == 0x00006116) { // popup without buttons with two lines of text
            val count = pos()
            val ptr = readInt()
            val count2 = pos()
            val ptr2 = readInt()
            readStringAt(this, ptr, count, it.second)
            readStringAt(this, ptr2, count2, it.second)
          }
          if (op == 0x00006416) { // only used for save before quit texts
            val count = pos()
            val ptr = readInt()
            val count2 = pos()
            val ptr2 = readInt()
            readStringAt(this, ptr, count, it.second)
            readStringAt(this, ptr2, count2, it.second)
          }
          if (op == 0x00005316) { // mainly used for save data texts, also for some other misc prompts
            val count = pos()
            val ptr = readInt()
            readStringAt(this, ptr, count, it.second)
          }
          if (op == 0x00005016) { // mainly used for save clear data details, also for some other misc prompts
            val count = pos()
            val ptr = readInt()
            readStringAt(this, ptr, count, it.second)
          }
          if (op == 0x00005916) { // occurs in game files, purpose unknown, maybe even some debug prompts
            readInt()
            val count = pos()
            val ptr = readInt()
            readStringAt(this, ptr, count, it.second)
          }
          if (op == 0x00006316) { // unused probably
            readInt()
            val count = pos()
            val ptr = readInt()
            readStringAt(this, ptr, count, it.second)
          }
        }
      }
    }
  }

  private fun readStringAt(input: KioInputStream, ptr: Int, ptrLoc: Int, storeTo: MutableList<DatEntry>) = with(input) {
    val lastPos = pos()
    try {
      val text = readDatString(at = ptr)
      storeTo.add(DatEntry(text, ptr, ptrLoc))
    } catch (e: EOFException) {
      println("EOF exception while reading string, ignored")
    } finally {
      setPos(lastPos)
    }
  }
}
