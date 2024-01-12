package tl.util

import java.io.File

open class Translation(
  jpFile: File,
  enFile: File = jpFile.resolveSibling("script-translation.txt"),
  notesFile: File = jpFile.resolveSibling("script-notes.txt"),
  options: Set<PreProcessingOptions> = emptySet(),
  private val overrides: Map<Int, String> = mapOf(),
) {
  val jpTexts = processTranslationFile(jpFile, options.contains(PreProcessingOptions.StripJpNewLine))
  val enTexts = processTranslationFile(enFile, options.contains(PreProcessingOptions.StripEnNewLine))
  val notes = processTranslationFile(notesFile, options.contains(PreProcessingOptions.StripNotesNewLine))

  init {
    if (jpTexts.size != enTexts.size || enTexts.size != notes.size) {
      error("Entry count mismatch")
    }
  }

  fun isTranslated(index: Int, offset: Int = 0): Boolean {
    val enText = enTexts[index + offset]
    return enText.isNotBlank()
  }

  open fun getTranslation(index: Int, offset: Int = 0, allowBlank: Boolean = false): String {
    return getRawTranslation(index, offset, allowBlank)
  }

  fun getRawTranslation(index: Int, offset: Int = 0, allowBlank: Boolean = false): String {
    val overrideText = overrides[index + offset]
    if (overrideText != null) {
      return overrideText
    }
    val enText = enTexts[index + offset]
    if (enText.isBlank() && !allowBlank) {
      return jpTexts[index + offset]
    }
    return enText
  }

  fun getJapanese(index: Int, offset: Int = 0): String {
    return jpTexts[index + offset]
  }

  fun getNote(index: Int, offset: Int = 0): String {
    return notes[index + offset]
  }
}

internal fun processTranslationFile(file: File, stripNewLine: Boolean): List<String> {
  return file.readText()
    .split("{end}\n\n").dropLast(1)
    .map { if (it.startsWith("\uFEFF")) it.substring(1) else it }
    .map { if (stripNewLine) it.replace("\n", "") else it }
}

sealed class PreProcessingOptions {
  data object StripJpNewLine : PreProcessingOptions()
  data object StripEnNewLine : PreProcessingOptions()
  data object StripNotesNewLine : PreProcessingOptions()
  data object StripPortraitsNewLine : PreProcessingOptions()
  data object StripAudioNewLine : PreProcessingOptions()
}
