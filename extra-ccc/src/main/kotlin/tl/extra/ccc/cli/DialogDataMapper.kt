package tl.extra.ccc.cli

import kio.KioInputStream
import kio.util.appendLine
import kio.util.child
import kio.util.relativizePath
import kio.util.toHex
import kio.util.toWHex
import kio.util.walkDir
import kio.util.writeJson
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.cccUnpack
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.extra.fateOutput
import tl.extra.util.parseCccSoundTable
import tl.util.appendWindowsLine
import tl.util.readDatString
import java.io.EOFException
import java.io.File
import java.io.IOException

fun main() {
  val baseOut = fateOutput.child("dialog_additional_data4")
  DialogueDataMapper(
    cccUnpack,
    cccUnpack.child("field_new"),
    parseCccSoundTable(cccToolkit.child("src/NPJH50505.BIN")),
    cccToolkit.child("src/translation/dat/script-japanese.txt"),
  ).processTo(baseOut, createUpdatedNotes = false, createAudioAndPortraitText = true)
  println("Done")
}

class DialogueDataMapper(
  private val baseDir: File,
  private val fieldSrcDir: File,
  private val soundTable: List<String>,
  translationJpFile: File,
) {
  private val translation = CccTranslation(translationJpFile, stripNewLine = true)
  private val translationNotes = CccTranslation(translationJpFile, stripNewLine = false)
  private val entries = LinkedHashSet<Entry>()

  private fun processDatFile(file: File) {
    if (file.extension != "dat") {
      return
    }
    val relPath = file.relativizePath(baseDir)
    println("Process $relPath")

    var protagonist = false
    var soundPath: String? = null
    var portraitId: Int? = null
    var portraitSetAt: Int? = null

    fun resetInternalCtx() {
      protagonist = false
      soundPath = null
      portraitId = null
      portraitSetAt = null
    }

    val funcCtx = FunctionIdCtx()

    with(KioInputStream(file)) {
      while (!eof()) {
        val op = readInt()

        // display standard text
        if (op == 0x00003926) {
          val ptr = readInt()
          try {
            val text = readDatString(ptr, maintainStreamPos = true, fixUpNewLine = true)
              .replace("\n", "\\n")
              .trim(' ', '　')
            val scriptIndex = translation.jpTexts.indexOfFirst { it == text }
            val entry = Entry(scriptIndex, text, protagonist, soundPath, portraitId, portraitSetAt, ptr)
            if (!entries.contains(entry)) {
              entries.add(entry)
            }
            soundPath = null
          } catch (e: EOFException) {
            setPos(pos() + 0x4)
          }
        }

        // play voice
        if (op == 0x00002311) {
          val ptr = readInt()
          try {
            temporaryJump(ptr) {
              val soundId = readInt()
              soundPath = if (soundId == -1 || soundId > soundTable.size) {
                null
              } else {
                soundTable[soundId]
              }
            }
          } catch (e: EOFException) {
            println("Invalid BGM opcode read at ${pos().toWHex()}")
          }
        }

        // write word to variable block
        if (op == 0x00000004) {
          val ptr = pos()
          val newValue = readInt()
          var wordNum = readInt()
          if (wordNum > 0x7FFFFF00) {
            wordNum -= 0x7FFFFF00
          }
          if (wordNum == 4) {
            // ignore this value, it is dynamic at runtime
            if (newValue > 0x7FFFFF00) {
              portraitId = null
              portraitSetAt = null
            } else {
              portraitId = newValue
              portraitSetAt = ptr
            }
          }
        }

        // prepareAndLoadEventPAK -> assume context will start fresh
        if (op == 0x0237) {
          resetInternalCtx()
        }

        // function call
        if (op == 0x1401) {
          val func = readInt()
          if (!funcCtx.isKnown(func)) {
            identifyFunction(funcCtx, func)
          }
          when (func) {
            in funcCtx.protagStartFunc -> {
              protagonist = true
            }
            in funcCtx.protagEndFunc -> {
              protagonist = false
            }
          }
        }
      }
    }
  }

  private fun KioInputStream.identifyFunction(funcCtx: FunctionIdCtx, func: Int) {
    temporaryJump(func) {
      try {
        while (!eof()) {
          val op = readInt()
          if (op == 0x0326 && func < 0x10000) {
            val structId = readInt()
            val motionMode = readInt()
            val motionCtl = readInt()
            val unk28 = readInt()
            val op2 = readInt()
            val structId2 = readInt()
            if (structId == 0x4A && motionMode == 2 && motionCtl == 0x1E0 && unk28 == 0x110 &&
              op2 == 0x21 && structId2 == 0x4A
            ) {
              println("Found protagonist starting function (from bg motion) at ${func.toHex()}")
              funcCtx.protagStartFunc.add(func)
              break
            }
          }
          if (op == 0x1220 && func < 0x10000) {
            println("Found protagonist starting function (from opcode) at ${func.toHex()}")
            funcCtx.protagStartFunc.add(func)
            break
          }
          if (op == 0x0124) {
            val structId = readInt()
            val clearColor = readInt()
            val clearTime = readInt()
            if (structId == 0x4A && clearColor == 0xFFFFFFFF.toInt() && clearTime == 0x9) {
              println("Found protagonist ending function (from bg clear) at ${func.toHex()}")
              funcCtx.protagEndFunc.add(func)
              break
            }
          }
          if (op == 0x0004) {
            val val1 = readInt()
            val val2 = readInt()
            val val3 = readInt()
            val val4 = readInt()
            val val5 = readInt()
            if (val1 == 0x7FFFFF26 && val2 == 0x7FFFFF27 && val3 == 0x3 &&
              val4 == 0xFFFFFFFF.toInt() && val5 == 0x7FFFFF20
            ) {
              println("Found protagonist ending function (from place check) at ${func.toHex()}")
              funcCtx.protagEndFunc.add(func)
              break
            }
          }
          if (op == 0x0B20) {
            val structId = readInt()
            if (structId == 0x4B) {
              println("Found protagonist ending function (from clear) at ${func.toHex()}")
              funcCtx.protagEndFunc.add(func)
              break
            }
          }
          if (op == 0x1E01) {
            funcCtx.ignoreFunc.add(func)
            break
          }
        }
      } catch (e: IOException) {
        funcCtx.ignoreFunc.add(func)
      }
    }
  }

  private fun mapEntryToSpeakerData(entry: Entry, transformTo: (Entry, String) -> String): Pair<String, String> {
    if (entry.protagonist) {
      return "-" to "Protagonist"
    }
    if (entry.portraitId == null) {
      return "[unknown]" to "[unknown]"
    }
    return when (entry.portraitId ushr 8) {
      0x0 -> transformTo(entry, "rin_1") to "Rin"
      0x1 -> transformTo(entry, "ran_1") to "Rani"
      0x2 -> transformTo(entry, "sin_1") to "Shinji"
      0x3 -> error("Should be unused")
      0x4 -> transformTo(entry, "mal") to "Alice"
      0x5 -> transformTo(entry, "gat") to "Gatou"
      0x6 -> error("Should be unused")
      0x7 -> transformTo(entry, "ju") to "Julius"
      0x8 -> transformTo(entry, "leo_1") to "Leo"
      0x9 -> transformTo(entry, "twi") to "Twice"
      0xA -> transformTo(entry, "ner_1") to "Saber"
      0xB -> transformTo(entry, "emi_1") to "Archer"
      0xC -> transformTo(entry, "tam_1") to "Caster"
      0xD -> transformTo(entry, "cuu") to "Lancer"
      0xE -> error("Should be unused")
      0xF -> error("Should be unused")
      0x10 -> transformTo(entry, "rob") to "Robin Hood"
      0x11 -> transformTo(entry, "sal") to "Nursery Rhyme"
      0x13 -> error("Should be unused")
      0x14 -> error("Should be unused")
      0x15 -> transformTo(entry, "gaw") to "Gawain"
      0x16 -> error("Should be unused")
      0x17 -> error("Should be unused")
      0x18 -> "-" to "???"
      0x19 -> transformTo(entry, "sak_1") to "Sakura"
      0x1A -> transformTo(entry, "ngil") to "Gilgamesh"
      0x1B -> transformTo(entry, "tai") to "Taiga"
      0x1C -> error("Should be unused") // Text says Aozaki Touko but tex uses Gilgamesh
      0x1D -> transformTo(entry, "iss") to "Issei Ryuudou"
      0x1E -> transformTo(entry, "kot") to "Kotomine"
      0x1F -> error("Should be unused")
      0x20 -> transformTo(entry, "adl") to "Andersen"
      0x21 -> transformTo(entry, "mks") to "BB"
      0x22 -> transformTo(entry, "psl") to "Passionlip"
      0x23 -> transformTo(entry, "mll") to "Meltlilith"
      0x24 -> transformTo(entry, "eli") to "Elizabeth"
      0x25 -> transformTo(entry, "ksk") to "BB"
      0x26 -> transformTo(entry, "kia") to "Kiara"
      0x27 -> transformTo(entry, "nkia") to "Kiara"
      0x28 -> transformTo(entry, "eli") to "Elizabeth"
      0x29 -> transformTo(entry, "gil_1") to "Gilgamesh"
      0x2A -> transformTo(entry, "zin") to "Jinako"
      0x2B -> transformTo(entry, "kar") to "Karna"
      0x2C -> transformTo(entry, "mdc") to "Green Tea"
      0x2D -> transformTo(entry, "ner2") to "??? (Saber)"
      0x2E -> transformTo(entry, "emi2") to "??? (Archer)"
      0x2F -> transformTo(entry, "tam2") to "??? (Caster)"
      0x30 -> transformTo(entry, "tam3") to "Caster"
      0x31 -> transformTo(entry, "tam2") to "??? (Gilgamesh)"
      0x32 -> error("Should be unused")
      0x33 -> "-" to "Male Student"
      0x34 -> "-" to "Female Student"
      0x35 -> transformTo(entry, "ner_1_2") to "Saber"
      0x36 -> transformTo(entry, "emi_1_2") to "Archer"
      0x37 -> transformTo(entry, "tam_1_2") to "Caster"
      0x38 -> transformTo(entry, "gil_1_2") to "Gilgamesh"
      0x39 -> transformTo(entry, "rin_1_2") to "Rin"
      0x3A -> transformTo(entry, "ran_1_2") to "Rani"
      0x3B -> transformTo(entry, "kar_2") to "Karna"
      0x3C -> transformTo(entry, "sak_1_2") to "Sakura"
      0x3D -> transformTo(entry, "sin_2") to "Shinji"
      0x3E -> transformTo(entry, "zin_2") to "Jinako"
      0x3F -> transformTo(entry, "ksk_2") to "BB"
      0x40 -> transformTo(entry, "mks_2") to "BB"
      0x41 -> transformTo(entry, "eli_2") to "Elizabeth"
      0x42 -> "-" to "Young Man"
      0x43 -> "-" to "Newscaster"
      0x44 -> transformTo(entry, "emi3") to "EMIYA (stay night)"
      0x45 -> error("Should be unused")
      0x46 -> transformTo(entry, "rin2") to "Rin"
      0x47 -> error("Should be unused")
      0x48 -> transformTo(entry, "gaw2") to "Gawain"
      0x49 -> transformTo(entry, "rob2") to "Robin"
      0x4A -> transformTo(entry, "ran2") to "Rani"
      else -> {
        println("WARN: Does not match any ${entry.portraitId.toHex()} at script index ${entry.scriptIndex + 1}")
        "-" to "[id match failed: ${entry.portraitId.toHex()}]"
      }
    }
  }

  private fun transformToImgTag(entry: Entry, name: String) =
    """<img src="img/$name/${name}_${entry.portraitId!! and 0xFF}.png" alt="[missing img for '$name', id: ${entry.portraitId.toHex()}]">"""

  private fun transformToFilePath(entry: Entry, name: String) =
    """img/$name/${name}_${entry.portraitId!! and 0xFF}.png"""

  fun processTo(outDir: File, createUpdatedNotes: Boolean, createAudioAndPortraitText: Boolean) {
    outDir.mkdir()
    val mismatches = StringBuilder()
    walkDir(fieldSrcDir) { processDatFile(it) }
    entries.groupBy { it.scriptIndex / 1000 }.forEach { (chunkIdx, entriesChunk) ->
      val rangeStart = chunkIdx * 1000 + 1
      val rangeEnd = (chunkIdx + 1) * 1000
      val sortedEntriesChunk = entriesChunk.sortedBy { it.scriptIndex }
      generateHtml(outDir.child("entries $rangeStart - $rangeEnd.html"), sortedEntriesChunk, mismatches)
    }
    outDir.child("entries.json").writeJson(entries)
    if (createUpdatedNotes) {
      createUpdatedNotes(outDir)
    }
    if (createAudioAndPortraitText) {
      createAudioAndPortraitText(outDir)
    }
    outDir.child("mismatches.txt").writeText(mismatches.toString())
  }

  private fun createAudioAndPortraitText(outDir: File) {
    val processedData = entries
      .groupBy { it.scriptIndex }
      .mapValues { group ->
        val audioEntry = group.value
          .mapNotNull { it.soundPath }
          .toSet()
          .joinToString(separator = "\n")
        val portraitEntry = group.value
          .asSequence()
          .map { mapEntryToSpeakerData(it, this::transformToFilePath) }
          .filterNot { it.second == "[unknown]" }
          .filterNot { it.second.startsWith("[id match failed") }
          .toSet()
          .joinToString(separator = "\n") { "${it.first}\t${it.second}" }
        Pair(audioEntry, portraitEntry)
      }
    val audioOut = StringBuilder()
    val portraitOut = StringBuilder()
    val blank = Pair("", "")
    repeat(translation.jpTexts.size) { id ->
      audioOut.appendLine("${processedData.getOrDefault(id, blank).first}{end}\n")
      portraitOut.appendLine("${processedData.getOrDefault(id, blank).second}{end}\n")
    }
    outDir.child("script-audio.txt").writeText(audioOut.toString())
    outDir.child("script-portrait.txt").writeText(portraitOut.toString())
  }

  private fun createUpdatedNotes(outDir: File) {
    val groupedEntries = entries.groupBy { it.scriptIndex }
    val notesOut = StringBuilder()
    translationNotes.notes.forEachIndexed { noteIdx, note ->
      val entries = groupedEntries[noteIdx]
      val extraText = if (!entries.isNullOrEmpty()) {
        val processedNames = entries
          .map {
            val speakerName = mapEntryToSpeakerData(it, this::transformToImgTag).second
            if (speakerName == "[unknown]" || speakerName.startsWith("[id match failed")) {
              return@map ""
            }
            if (it.soundPath != null && !it.protagonist) {
              return@map "$speakerName (${it.soundPath})"
            }
            return@map speakerName
          }
          .toSortedSet()
        val joinedNames = processedNames.joinToString(separator = "\n", prefix = "[", postfix = "]\n")
        if (processedNames.isEmpty()) "" else joinedNames
      } else {
        ""
      }
      notesOut.appendLine("$extraText$note{end}\n")
    }
    outDir.child("script-notes.txt").writeText(notesOut.toString())
  }

  private fun generateHtml(htmlFile: File, outEntries: List<Entry>, mismatches: StringBuilder) {
    val htmlOut = StringBuilder()
    htmlOut.append(
      """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
table {
  font-family: arial, sans-serif;
  border-collapse: collapse;
}

td, th {
  border: 1px solid #dddddd;
  text-align: left;
  padding: 8px;
}
</style>
</head>
<body>

<table>
  <tr>
    <th>Offset</th>
    <th>Image</th>
    <th>Speaker</th>
    <th>Japanese</th>
    <th>Translated</th>
    <th>Notes</th>
    <th>Audio</th>
  </tr>""",
    )

    outEntries.sortedBy { it.scriptIndex }.forEach { entry ->
      val en = try {
        translation.getTranslation(entry.text, true)
      } catch (e: IllegalStateException) {
        println("WARN: Retry translation get with trailing new line")
        translation.getTranslation(entry.text + "\n", true)
      }
      val note = translationNotes.getNote(entry.scriptIndex)
      val noteSpeaker = note.split("\n")[0]
      val (portrait, speaker) = mapEntryToSpeakerData(entry, this::transformToImgTag)
      if (note.isNotBlank() && !noteSpeaker.contains(speaker, ignoreCase = true) && !speaker.startsWith("[unknown")) {
        if (!(speaker == "Kiara" && noteSpeaker.contains("Obscene Nun"))) {
          mismatches.appendWindowsLine("At ${entry.scriptIndex + 1}, file: '$speaker', note: '${note.split("\n")[0]}'")
        }
      }
      htmlOut.append(
        """<tr>
    <td>${entry.scriptIndex + 1}</td>
    <td>$portrait</td>
    <td>$speaker</td>
    <td>${entry.text.replace("\\n", "<br>")}</td>
    <td>${en.replace("\n", "<br>")}</td>
    <td>${note.replace("\n", "<br>")}</td>
    <td>${entry.soundPath ?: "-"}</td>
  </tr>""",
      )
    }
    htmlOut.append("""</table></body></html>""")
    htmlFile.writeText(htmlOut.toString())
  }

  private data class Entry(
    val scriptIndex: Int,
    val text: String,
    val protagonist: Boolean,
    val soundPath: String?,
    val portraitId: Int?,
    val portraitSetAt: Int?,
    val textFoundAt: Int,
  )

  private class FunctionIdCtx {
    val protagStartFunc = mutableSetOf<Int>()
    val protagEndFunc = mutableSetOf<Int>()
    val ignoreFunc = mutableSetOf<Int>()

    fun isKnown(func: Int): Boolean {
      return func in protagStartFunc || func in protagEndFunc || func in ignoreFunc
    }
  }
}
