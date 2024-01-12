package tl.extra.ccc.cli

import kio.util.child
import kio.util.readJson
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.extra.ccc.util.CccWidthLimit
import tl.extra.ccc.util.replaceControlCodesForTextMeasure
import tl.extra.fateOutput
import tl.extra.util.TextMeasure
import tl.util.WarningCollector
import java.io.File

fun main() {
  DatTextMeasure(
    fateOutput.child("dialog_additional_data4/entries.json"),
    cccToolkit.child("src/translation/dat"),
  ).process()
  println("Done")
}

class DatTextMeasure(
  private val entriesFile: File,
  translationDir: File,
) {
  private val warningCollector = WarningCollector(logWarningsImmediately = false)
  private val translation = CccTranslation(
    translationDir.child("script-japanese.txt"),
    stripNewLine = true,
    stripJpAndNotesNewLine = false
  )
  private val textMeasure = TextMeasure(7, warningCollector::warn)

  fun process() {
    val entries = entriesFile.readJson<List<Entry>>()
      .associateBy { it.scriptIndex }

    val scanRange = 0 until 28448

    val narrationOverrides = (176..178).associateWith { true } +
      (1459..1462).associateWith { true } +
      (5594..5596).associateWith { true } +
      (5600..5604).associateWith { true } +
      (5980..5981).associateWith { true } +
      (6107..6109).associateWith { true } +
      (6843..6845).associateWith { true } +
      (6848..6851).associateWith { true } +
      (7222..7223).associateWith { true } +
      (10017..10021).associateWith { true } +
      (10170..10171).associateWith { true } +
      (10177..10183).associateWith { true } +
      (10772..10774).associateWith { true } +
      (12055..12057).associateWith { true } +
      (12059..12061).associateWith { true } +
      (12063..12064).associateWith { true } +
      (12314..12316).associateWith { true } +
      (13048..13057).associateWith { true } +
      (14706..14707).associateWith { true } +
      (14768..14770).associateWith { true } +
      (16033..16037).associateWith { true } +
      (17667..17669).associateWith { true } +
      (17671..17672).associateWith { true } +
      (17675..17677).associateWith { true } +
      (17679..17680).associateWith { true } +
      (17686..17687).associateWith { true } +
      (17690..17693).associateWith { true } +
      (17699..17702).associateWith { true } +
      (17864..17866).associateWith { true } +
      (17895..17896).associateWith { true } +
      (17868..17869).associateWith { true } +
      (17898..17903).associateWith { true } +
      (17906..17908).associateWith { true } +
      (17912..17919).associateWith { true } +
      (17924..17931).associateWith { true } +
      (17938..17939).associateWith { true } +
      (17944..17945).associateWith { true } +
      (17947..17948).associateWith { true } +
      (17957..17958).associateWith { true } +
      (17984..17987).associateWith { true } +
      (18006..18012).associateWith { true } +
      (18017..18018).associateWith { true } +
      (18023..18025).associateWith { true } +
      (18037..18042).associateWith { true } +
      (18045..18050).associateWith { true } +
      (18031..18032).associateWith { true } +
      (18365..18374).associateWith { true } +
      (21949..21950).associateWith { true } +
      (23209..23213).associateWith { true } +
      (26420..26422).associateWith { true } +
      (26944..26949).associateWith { true } +
      listOf(
        3263, 6056, 6638, 6640,
        10191, 10292, 12643, 12727, 12730, 12975, 12979, 13009, 13029, 13038, 14071, 15760, 15885, 16034,
        17673, 17685, 17914, 17939, 17955, 17963, 17964, 17968, 17970, 17971, 17973, 17974, 17975, 17976, 17977, 17979, 17980, 17997, 17998,
        18028, 18374, 19252, 23853, 23862, 23865, 23876, 23879, 23885, 23898, 23962, 27403, 27407, 28231,
        11693, 13037, 15762, 20029, 20046, 22718, 22719,
        23864, 23866, 23867, 23878, 23883, 23886, 23889, 23889, 23893, 23897, 23900, 23902, 23904, 23906, 23908, 23910,
        23959, 23965, 23968, 23972, 23974, 23975, 23977, 23978,
        27401, 27402, 27438, 27610
      ).associateWith { true } +
      mapOf(15297 to false, 15389 to false)

    val ambiguousOffsets = setOf(
      76, 175, 188, 237, 2202, 2585, 3205, 3856, 4173, 4561, 4595, 5432, 6107, 6108, 6109, 6215, 6345, 6652, 6833, 6834, 9556,
      10017, 10018, 10019, 10020, 10021, 10170, 10171, 10177, 10178, 10179, 10180, 10181, 10182, 10183, 10772, 10773, 10774,
      12975, 12979, 13009, 13029, 13038, 13048, 13049, 13050, 13051, 13052, 13053, 13054, 13055, 13056, 13057, 13425,
      15885, 15920, 19147, 19185, 19253, 19345, 19346, 19496, 19679,
      23853, 23862, 23865, 23876, 23879, 23885, 23898, 23962, 26784, 26785, 26811, 27205, 27206, 27403, 27407, 28229, 28230, 28339, 28340,
      11693, 13037, 15297, 15762, 20029, 20046, 22718, 22719,
      23864, 23866, 23867, 23878, 23883, 23886, 23889, 23889, 23893, 23897, 23900, 23902, 23904, 23906, 23908, 23910,
      23959, 23965, 23968, 23972, 23974, 23975, 23977, 23978,
      27401, 27402,
    )

    val narrowCharacters = listOf(',', '.', '!')
    val veryNarrowCharacters = listOf('.')

    val tooLongLines = scanRange.flatMap { offset ->
      val en = translation.getTranslation(offset, allowBlank = true)
      val narration = narrationOverrides[offset]
        ?: entries[offset]?.protagonist
        ?: let {
          warningCollector.warn("Missing narration flag for $offset")
          false
        }
      val lines = en.split("\n")
        .map { replaceControlCodesForTextMeasure(it) }
        .dropLastWhile { it.isBlank() }

      if (!narration && lines.size > 3) {
        warningCollector.warn("Too many dialog lines: $offset")
      }

      if (offset !in ambiguousOffsets) {
        val noteLines = translation.notes[offset].split("\n")
        val endCheckedIndex = noteLines.indexOfFirst { it.endsWith("]") }.coerceAtLeast(0) + 2
        val checkedNotes = noteLines.take(endCheckedIndex).joinToString(" ")
        if (!narration && checkedNotes.contains("Protagonist")) {
          warningCollector.warn("Not narration but protagonist in notes: $offset")
        }
        if (narration && !checkedNotes.contains("Protagonist")) {
          warningCollector.warn("Narration but protagonist not in notes: $offset")
        }
      }

      lines
        .filter { it.isNotBlank() }
        .mapIndexedNotNull { index, line ->
          val lineWidth = textMeasure.measureInGameText(line)
          val lastLine = when {
            narration -> index == lines.lastIndex
            else -> index >= 2
          }
          val maxWidth = when {
            narration -> if (lastLine) CccWidthLimit.narrationLastLine else CccWidthLimit.narration
            else -> {
              when {
                lastLine && line.last() in veryNarrowCharacters -> CccWidthLimit.dialogLastLineFinalCharacterVeryNarrow
                lastLine && line.last() in narrowCharacters -> CccWidthLimit.dialogLastLineFinalCharacterNarrow
                lastLine -> CccWidthLimit.dialogLastLine
                line.last() in narrowCharacters -> CccWidthLimit.dialogFinalCharacterNarrow
                else -> CccWidthLimit.dialog
              }
            }
          }
          when {
            lineWidth > maxWidth -> TooLongLine(offset, lineWidth, maxWidth, narration, lastLine, line)
            else -> null
          }
        }
    }

    tooLongLines
      .sortedWith(compareBy({ it.narration }, { it.lastLine }, { it.lineWidth }, { it.offset }))
      .forEach { println(it) }
    println("${tooLongLines.size} too long lines")

    warningCollector.printSummary()
  }

  private data class Entry(
    val scriptIndex: Int,
    val protagonist: Boolean,
  )

  private data class TooLongLine(
    val offset: Int,
    val lineWidth: Int,
    val maxWidth: Int,
    val narration: Boolean,
    val lastLine: Boolean,
    val line: String
  ) {
    override fun toString(): String {
      return "Too long line: index $offset: width $lineWidth > $maxWidth (narration=$narration, lastLine=$lastLine): $line"
    }
  }
}
