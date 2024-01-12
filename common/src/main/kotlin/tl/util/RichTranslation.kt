package tl.util

import java.io.File

class RichTranslation(
  jpFile: File,
  enFile: File = jpFile.resolveSibling("script-translation.txt"),
  notesFile: File = jpFile.resolveSibling("script-notes.txt"),
  portraitFile: File = jpFile.resolveSibling("script-portrait.txt"),
  audioFile: File = jpFile.resolveSibling("script-audio.txt"),
  options: Set<PreProcessingOptions> = emptySet(),
) : Translation(jpFile, enFile, notesFile, options) {
  val portraits = processTranslationFile(portraitFile, options.contains(PreProcessingOptions.StripPortraitsNewLine))
  val audio = processTranslationFile(audioFile, options.contains(PreProcessingOptions.StripAudioNewLine))

  init {
    if (jpTexts.size != portraits.size || portraits.size != audio.size) {
      error("Entry count mismatch")
    }
  }

  fun getPortrait(index: Int, offset: Int = 0): String {
    return portraits[index + offset]
  }

  fun getAudio(index: Int, offset: Int = 0): String {
    return audio[index + offset]
  }
}
