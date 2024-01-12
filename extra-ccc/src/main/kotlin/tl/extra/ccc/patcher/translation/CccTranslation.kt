package tl.extra.ccc.patcher.translation

import tl.extra.ccc.util.replaceControlCodesForTextMeasure
import tl.extra.util.TextMeasure
import tl.util.PreProcessingOptions
import tl.util.Translation
import java.io.File

class CccTranslation(
  jpFile: File,
  enFile: File = jpFile.resolveSibling("script-translation.txt"),
  notesFile: File = jpFile.resolveSibling("script-notes.txt"),
  private val stripNewLine: Boolean = false,
  stripJpAndNotesNewLine: Boolean = stripNewLine,
  private val replaceLiteralNewLine: Boolean = true,
  overrides: Map<Int, String> = mapOf(),
  private val checkForAmbiguous: Boolean = false,
  private val failOnLiteralNewLine: Boolean = false,
  safetyChecks: Boolean = true
) : Translation(
  jpFile, enFile, notesFile,
  if (stripJpAndNotesNewLine) setOf(
    PreProcessingOptions.StripJpNewLine,
    PreProcessingOptions.StripNotesNewLine
  ) else emptySet(),
  overrides = overrides
) {
  init {
    if (safetyChecks) {
      checkRubyStartedButInvalid()
      checkRubyEndedButNotStarted()
      checkInvalidRubyOffsets()
      checkForInvalidCharacters()
      checkForLiteralNewLine()
    }
  }

  private fun checkRubyStartedButInvalid() {
    val rubsRegex = "#RUBS".toRegex()
    repeat(enTexts.size) { idx ->
      val tl = getTranslation(idx)
      val rubsInTl = rubsRegex.find(tl)
        ?.groups
        ?.filterNotNull()
        ?.map { it.range } ?: return@repeat

      rubsInTl.forEachIndexed { index, range ->
        val rubsIndex = range.last
        val nextRubsIndex = rubsInTl.getOrNull(index + 1)?.last ?: tl.length
        val rubeIndex = tl.indexOf("#RUBE", rubsIndex)
        val rendIndex = tl.indexOf("#REND", rubeIndex)
        if (rubeIndex == -1 ||
          rendIndex == -1 ||
          rubeIndex > rendIndex ||
          rubeIndex > nextRubsIndex ||
          rendIndex > nextRubsIndex
        ) {
          error("TL ERROR: Invalid ruby will cause issues in game: $idx, $tl")
        }
      }
    }
  }

  private fun checkRubyEndedButNotStarted() {
    val rubsRegex = "(^|[^#])RUBS".toRegex()
    repeat(enTexts.size) { idx ->
      val tl = getTranslation(idx)
      rubsRegex.find(tl)?.groups?.filterNotNull()?.forEach { group ->
        val wrongRubsIndex = group.range.last
        val rubeIndex = tl.indexOf("#RUBE", wrongRubsIndex)
        val rendIndex = tl.indexOf("#REND", wrongRubsIndex)
        if (wrongRubsIndex != -1 && (rubeIndex == -1) || (rendIndex == -1)) {
          error("TL ERROR: Wrong ruby formatting will cause crash in game: $idx, $tl")
        }
      }
    }
  }

  private fun checkInvalidRubyOffsets() {
    val startedOffsetsRegex = """#ROFS""".toRegex()
    val validOffsetsRegex = """#ROFS[-\d]\d{3}""".toRegex()
    repeat(enTexts.size) { idx ->
      val tl = getTranslation(idx)
      val startedOffsets = startedOffsetsRegex.findAll(tl).count()
      val validOffsets = validOffsetsRegex.findAll(tl).count()
      if (startedOffsets != validOffsets) {
        error("TL ERROR: Wrong ruby ROFS control code will cause crash in game: $idx, $tl")
      }
    }
  }

  private fun checkForInvalidCharacters() {
    repeat(enTexts.size) { idx ->
      val tl = getTranslation(idx)
      val rawTl = getRawTranslation(idx, allowBlank = true)
      if (tl.contains("—")) {
        error("TL ERROR: Wrong dash used `—`: $idx, $tl")
      }
      if (rawTl.contains("+") || rawTl.contains("＋")) {
        error("TL ERROR: Plus renders as minus so it's likely an mistake and wide plus should not be used, replace with @: $idx, $rawTl")
      }
      if (rawTl.contains("⑥")) {
        error("TL ERROR: ⑥ should not be used due to different style, replace with !?: $idx, $rawTl")
      }
    }
  }

  private fun checkTryingToUseNewLineAsControlCode() {
    repeat(enTexts.size) { idx ->
      val tl = getTranslation(idx, allowBlank = true)
      if (tl.contains("#\n")) {
        println("TL WARN: Trying to use new line as a control code: $idx, $tl")
      }
    }
  }

  private fun checkForLiteralNewLine() {
    if (!failOnLiteralNewLine) {
      return
    }
    repeat(enTexts.size) { idx ->
      val tl = getRawTranslation(idx, allowBlank = true)
      if (tl.contains("\\n")) {
        error("TL ERROR: Literal new line found but they are not permitted: $idx, $tl")
      }
    }
  }

  fun getTranslation(jpText: String, allowBlank: Boolean = false): String {
    val firstJpTextIndex = jpTexts.indexOfFirst { it == jpText }
    val lastJpTextIndex = jpTexts.indexOfLast { it == jpText }
    if (firstJpTextIndex != lastJpTextIndex && checkForAmbiguous) {
      println(
        "TL WARN: Ambiguous text found: '${jpText.replace("\n", "\\n")}', " +
          "it exists at least at offsets ${firstJpTextIndex + 1} and ${lastJpTextIndex + 1}"
      )
    }
    if (firstJpTextIndex == -1) {
      error("Missing translation entry for: $jpText")
    }
    return getTranslation(firstJpTextIndex, 0, allowBlank)
  }

  override fun getTranslation(index: Int, offset: Int, allowBlank: Boolean): String {
    return getRawTranslation(index, offset, allowBlank)
      .let { if (replaceLiteralNewLine) it.replace("\\n \n", "\\n\n") else it }
      .let { if (stripNewLine) it.replace("\n", "") else it }
      .let { if (replaceLiteralNewLine) it.replace("\\n", "\n") else it }
      .replace("’", "'")
      .replace("“", "\"")
      .replace("”", "\"")
      .replace("~", "～")
      // Č é á ā ä ī and so on - extra mappings for those characters uses non printable ASCII
      .replace("Č", "\u001F")
      .replace("é", "\u001E")
      .replace("á", "\u001D")
      .replace("ā", "\u001C")
      .replace("ī", "\u0019")
      .replace("ä", "\u0017")
      .replace("è", "\u0016")
      .replace("ó", "\u0015")
      .replace("ü", "\u0014")
      .replace("í", "\u0013")
      .replace("Ç", "\u0012")
      .replace("ö", "\u0011")
      .replace("É", "\u0010")
      .replace("ū", "\u0009")
      .replace("ṇ", "\u0007")
      .replace("<!--IGNORED-->", "")
  }
}

fun CccTranslation.checkForTooLongTranslations(
  textMeasure: TextMeasure,
  warn: (String) -> Unit,
  logPrefix: String,
  maxWidth: Int,
  maxLines: Int = Integer.MAX_VALUE,
  offsets: IntRange? = null,
) {
  (offsets ?: enTexts.indices).forEach { offset ->
    val translation = this.getTranslation(offset, allowBlank = true)
    checkForTooLongTranslation(textMeasure, warn, logPrefix, maxWidth, maxLines, translation, offset)
  }
}

fun CccTranslation.checkForTooLongTranslation(
  textMeasure: TextMeasure,
  warn: (String) -> Unit,
  logPrefix: String,
  maxWidth: Int,
  maxLines: Int = Integer.MAX_VALUE,
  translation: String,
  offset: Int?,
) {
  translation.split("\n")
    .also { if (it.size > maxLines) warn("$logPrefix: Too many text lines: ${translation.replace("\n", "\\n")}") }
    .forEach { line ->
      val width = textMeasure.measureInGameText(replaceControlCodesForTextMeasure(line))
      if (width > maxWidth) {
        warn("$logPrefix: Too long text line ($width > $maxWidth) in ${offset ?: "(offset not specified)"}: $line")
      }
    }
}
