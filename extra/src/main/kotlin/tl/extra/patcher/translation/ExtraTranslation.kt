package tl.extra.patcher.translation

import tl.util.PreProcessingOptions
import tl.util.Translation
import java.io.File

internal class ExtraTranslation(
  jpFile: File,
  enFile: File = jpFile.resolveSibling("script-translation.txt"),
  notesFile: File = jpFile.resolveSibling("script-notes.txt"),
  stripNewLine: Boolean = false,
  overrides: Map<Int, String> = mapOf(),
  checkForAsciiOnly: Boolean = false,
) : Translation(
  jpFile, enFile, notesFile,
  if (stripNewLine) setOf(
    PreProcessingOptions.StripJpNewLine,
    PreProcessingOptions.StripEnNewLine,
    PreProcessingOptions.StripNotesNewLine
  ) else emptySet(),
  overrides = overrides
) {
  init {
    if (checkForAsciiOnly) {
      repeat(enTexts.size) { idx ->
        val tl = getTranslation(idx)
        val allowedNonAscii = listOf('…', 'お', '～', '―', '☆')
          .map { it.code }
        val hasNonAscii = tl.toCharArray()
          .map { it.code }
          .any { it > 126 && it !in allowedNonAscii }
        if (hasNonAscii) {
          error("TL ERROR: Non-ASCII characters in: $tl")
        }
      }
    }
  }

  override fun getTranslation(index: Int, offset: Int, allowBlank: Boolean): String {
    return getRawTranslation(index, offset, allowBlank)
      .replace("鷙", "お") // お is remapped to heart symbol, 鷙 is used in CCC, so we replace it here for consistency
      .replace("\r\n", "\n")
      .replace("’", "'")
      .replace("“", "\"")
      .replace("”", "\"")
  }
}
