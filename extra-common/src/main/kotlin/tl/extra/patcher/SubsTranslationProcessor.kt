package tl.extra.patcher

import kio.util.WINDOWS_932
import kio.util.child
import kio.util.toHex
import kio.util.toWHex
import tl.extra.util.TextMeasure
import tl.util.Translation
import tl.util.ensureNoSdbmHashCollisions
import tl.util.sdbmHash
import java.io.File
import java.time.Instant
import kotlin.math.round

class SubsTranslationProcessor(
  private val nativeImDir: File,
  subsTranslation: Translation,
  subsSeTranslation: Translation,
  seList: List<String>,
  private val seIdRemapping: Map<String, Map<Int, Int>>?,
  private val textMeasure: TextMeasure,
  private val publicBuild: Boolean,
  private val remappedTextData: ByteArray,
  private val warn: (String) -> Unit,
) {
  private val warnCharacterLineLimit = 48 * textMeasure.asciiSpacing
  private val warnCharacterSgLineLimit = 67 * textMeasure.asciiSpacing

  init {
    nativeImDir.mkdir()
    val subs = parseSubs(subsTranslation)
    val subsSe = parseSubs(subsSeTranslation)
      .mapNotNull {
        val (seBank, seId) = it.audioPath
          .replace(".wav", "")
          .split(".")
        val seBankId = seList.indexOf(seBank)
        if (seBankId == -1) {
          error("Sound effect bank '${it.audioPath}' not found in the SE bank list")
        }
        val remappedSeId = remapSeId(seBank, seId.toInt())
        if (remappedSeId != -1) {
          Sub(
            audioPath = "$seBankId.$remappedSeId",
            skipMode = it.skipMode,
            displayMode = it.displayMode,
            dispatchMode = it.dispatchMode,
            parts = it.parts
          )
        } else {
          warn("${it.audioPath} has subs but the game ID is not set")
          null
        }
      }
    val buildSubs = subs
      .plus(subsSe)
      .let { if (publicBuild) it.shuffled() else it }
    generateCode(buildSubs)
  }

  private fun remapSeId(seBank: String, seId: Int): Int {
    if (seIdRemapping == null) {
      return seId
    }
    return seIdRemapping.getValue(seBank).getValue(seId)
  }

  private fun parseSubs(translation: Translation?): List<Sub> {
    if (translation == null) {
      return emptyList()
    }
    return translation.jpTexts
      .map { it.lowercase() }
      .mapIndexedNotNull { idx, audioPath ->
        val sub = translation.getTranslation(idx, allowBlank = true)
        val note = translation.getNote(idx)
        val config = parseConfig(audioPath, note)
        parseSub(audioPath, sub, config)
      }
  }

  private fun parseSub(audioPath: String, sub: String, config: SubConfig?): Sub? {
    if (sub.isBlank()) {
      return null
    }
    if (config == null) {
      warn("Subtitles: '$audioPath' is missing valid config.")
      return null
    }
    val parts = sub.split("\n")
    if (parts.size != config.parts.size) {
      warn("Subtitles: '$audioPath' has invalid part config. Expected ${parts.size} but config has ${config.parts.size}.")
      return null
    }
    val subParts = parts.mapIndexed { partIdx, partText ->
      val (blockWidth, normalizedPartText, _) = centerMultilineText(partText)
      when {
        audioPath in listOf("cv/fld/kot_00924.at3", "cv/fld/kot_0093.at3", "cv/fld/kot_00931.at3", "sound/bgm/blossom_ok.at3") -> {
          // ignore, those are too long but appear as you leave the shop, ED was manually verified
        }
        config.displayMode == 2 && blockWidth > warnCharacterSgLineLimit -> {
          warn("Subtitles: '$audioPath' has too long text lines (SG): $normalizedPartText")
        }
        config.displayMode != 2 && blockWidth > warnCharacterLineLimit -> {
          warn("Subtitles: '$audioPath' has too long text lines: $normalizedPartText")
        }
      }
      val partConfig = config.parts[partIdx]
      val x = partConfig.xOverride?.plus(partConfig.xOffset) ?: (240 + partConfig.xOffset)
      val lineCount = normalizedPartText.count { it == '\n' } + 1
      SubPart(normalizedPartText, x, partConfig.frames, lineCount)
    }
    return Sub(audioPath, config.skipMode, config.displayMode, config.dispatchMode, subParts)
  }

  private fun centerMultilineText(text: String): TextCenteringResult {
    val lines = text.split("\\n")
    val maxWidth = lines.maxOf { textMeasure.measureInGameText(toInGameVisibleText(it)) }
    if (lines.size == 1) {
      return TextCenteringResult(maxWidth, text, toInGameVisibleText(text))
    }
    val sb = StringBuilder()
    val inGameSb = StringBuilder()
    lines.mapIndexed { idx, lineText ->
      val inGameLineText = toInGameVisibleText(lineText)
      val inGameLineTextWidth = textMeasure.measureInGameText(inGameLineText)
      if (inGameLineTextWidth == maxWidth) {
        sb.append(lineText)
        inGameSb.append(inGameLineText)
      } else {
        val sidePadding = round((maxWidth - inGameLineTextWidth) / textMeasure.asciiSpacing / 2.0).toInt()
        repeat(sidePadding) {
          sb.append(' ')
          inGameSb.append(' ')
        }
        sb.append(lineText)
        inGameSb.append(inGameLineText)
        repeat(sidePadding) {
          sb.append(' ')
          inGameSb.append(' ')
        }
      }
      if (idx != lines.lastIndex) {
        sb.append('\n')
        inGameSb.append('\n')
      }
    }
    return TextCenteringResult(maxWidth, sb.toString(), inGameSb.toString())
  }

  private fun toInGameVisibleText(line: String): String {
    return line
      .replace("#C[0-9]{9}".toRegex(), "")
      .replace("#CDEF", "")
      .replace("#SP13", "～")
      .replace("#SP14", "～")
      .replace("#SP15", "～")
      .replace("#SP16", "～")
  }

  private fun parseConfig(audioPath: String, configText: String): SubConfig? {
    if (configText.isBlank()) {
      return null
    }
    return try {
      val configSections = configText
        .split("-")[0]
        .split("\n", limit = 2)
      val header = configSections[0].split(",")
      val parts = configSections[1]
        .split("\n")
        .filter { it.isNotBlank() }
        .map {
          val partConfig = it.split(",", limit = 3)
          SubPartConfig(
            frames = partConfig[0].toInt(),
            xOverride = partConfig.elementAtOrNull(1)?.toInt(),
            xOffset = partConfig.elementAtOrNull(2)?.toInt() ?: 0
          )
        }
      SubConfig(
        skipMode = header[0].toInt(),
        displayMode = header[1].toInt(),
        dispatchMode = header.elementAtOrNull(2)?.toInt() ?: 1,
        parts = parts
      )
    } catch (e: Exception) {
      warn("Subtitles: '$audioPath' config can't be parsed. Exception occurred: ${e.javaClass.simpleName}: ${e.message}")
      null
    }
  }

  private fun generateCode(subs: List<Sub>) {
    ensureNoSdbmHashCollisions(subs.map { it.audioPath.toByteArray() })
    val subsGen = subs.mapIndexed { index, sub ->
      "{${sdbmHash(sub.audioPath.toByteArray())}, ${sub.skipMode}, ${sub.displayMode}, ${sub.dispatchMode}, ${sub.parts.size}, " +
        "sub${index}_parts} /* ${sub.audioPath} */"
    }

    nativeImDir.child("generated.cpp").writeText(
      """
// DO NOT MODIFY. THIS FILE IS AUTO-GENERATED.

#include "generated.h"

const u32 subsTimestamp = ${Instant.now().epochSecond.toInt().toHex()};

${generatePartsData(subs).joinToString(separator = "\n")}

${generateParts(subs).joinToString(separator = "\n")}

const u32 subsCount = ${subsGen.size};
const subtitle subs[] = {
    ${subsGen.joinToString(separator = ",\n\t")}
};

const u8 remappedTextData[] = ${remappedTextData.joinToString(separator = ", ", prefix = "{", postfix = "}") { "0x${it.toWHex()}" }};
""".trimStart()
    )
  }

  private fun generatePartsData(subs: List<Sub>) = subs
    .mapIndexed { subIdx, sub ->
      sub.parts.mapIndexed { partIdx, part ->
        generateTextData(subIdx, partIdx, part)
      }
    }
    .flatten()

  private fun generateTextData(subIdx: Int, partIdx: Int, part: SubPart, suffix: String = ""): String {
    val bytes = part.text
      .toByteArray(Charsets.WINDOWS_932)
      .toMutableList()
      .apply {
        add(0, part.lineCount.toByte())
        add(0)
      }
      .joinToString(separator = ", ", prefix = "{", postfix = "}") {
        "0x${it.toWHex()}"
      }
    return """// ${part.text.replace("\n", "<br>")}
const u8 partdata_${subIdx}_$partIdx$suffix[] = $bytes;
    """.trimIndent()
  }

  private fun generateParts(subs: List<Sub>) = subs.mapIndexed { subIndex, sub ->
    """
const subtitle_part sub${subIndex}_parts[] = {
    ${generatePartsList(subIndex, sub)}
};""".trimStart()
  }

  private fun generatePartsList(subIdx: Int, sub: Sub) = sub.parts
    .mapIndexed { partIdx, part -> partIdx to part }
    .joinToString(separator = ",\n\t") { (partIdx, part) ->
      """{(const char*)partdata_${subIdx}_$partIdx,
                | ${part.x}, ${part.frames}}""".trimMargin().replace("\n", "")
    }
}

private data class TextCenteringResult(
  val blockWidth: Int,
  val normalizedText: String,
  val dropShadowText: String,
)

// Subs config format:
// 0,0,0 - skip_mode,display_mode,dispatch_mode
// ...parts config follows, for each line number of frames how long it's visible, followed by an optional xOverride and optional xOffset
// -
// any other notes
//
// Skip mode: tells whether that audio playback can be skipped or interrupted. [NOT IMPLEMENTED]
// - 0: means no skipping is possible,
// - 1: can be skipped.
// Display mode:
// - 0: display at the bottom
// - 1: display at the top
// - 2: display mode for SG
// - 3: display mode for shop
// Dispatch mode:
// - 0: same as 2 but for CCC top subtitles won't be dispatched for 30 frames after dialog audio playback opcode is used
// - 1: dispatch only when in battle
// - 2: don't care, always dispatch

private class Sub(
  val audioPath: String,
  val skipMode: Int,
  val displayMode: Int,
  val dispatchMode: Int,
  val parts: List<SubPart>,
)

private class SubPart(
  val text: String,
  val x: Int,
  val frames: Int,
  val lineCount: Int,
)

private class SubConfig(
  val skipMode: Int,
  val displayMode: Int,
  val dispatchMode: Int,
  val parts: List<SubPartConfig>,
)

private class SubPartConfig(
  val frames: Int,
  val xOverride: Int?,
  val xOffset: Int,
)
