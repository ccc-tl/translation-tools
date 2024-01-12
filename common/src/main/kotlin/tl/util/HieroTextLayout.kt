package tl.util

import tl.file.HieroFile

object HieroTextLayout {
  fun layoutText(
    hiero: HieroFile,
    text: String,
    maxLineLength: Int,
    newLine: String = "\n",
    xAdvanceOverrides: Map<Char, Int> = emptyMap(),
  ): HieroLayoutResult {
    var lineLength = 0
    val lineLengths = mutableListOf<Int>()
    val out = StringBuilder()
    val words = text.split(" ")
    val spaceLength = measureWord(hiero, " ", xAdvanceOverrides)
    words.forEachIndexed { index, word ->
      val wordLength = measureWord(hiero, word, xAdvanceOverrides)
      if (lineLength + wordLength > maxLineLength) {
        if (out.endsWith(" ")) {
          out.setLength(out.length - 1)
          lineLength - spaceLength
        }
        out.append(newLine)
        lineLengths.add(lineLength)
        lineLength = 0
      }
      out.append(word)
      lineLength += wordLength
      if (index != words.lastIndex) {
        out.append(" ")
        lineLength + spaceLength
      }
    }
    lineLengths.add(lineLength)
    return HieroLayoutResult(out.toString(), lineLengths, lineLengths.maxOf { it })
  }

  fun measureWord(hiero: HieroFile, word: String, xAdvanceOverrides: Map<Char, Int>): Int {
    var wordLength = 0
    word.forEachIndexed { index, character ->
      wordLength += xAdvanceOverrides[character] ?: hiero.chars.firstOrNull { it.id == character.code }?.xadvance
        ?: error("Missing Hiero definition for character: '$character' in '$word'")
      if (index < word.lastIndex) {
        val nextCharacter = word[index + 1]
        val kerning = hiero.kernings.firstOrNull { it.first == character.code && it.second == nextCharacter.code }
        wordLength += kerning?.amount ?: 0
      }
    }
    return wordLength
  }
}

data class HieroLayoutResult(val text: String, val lineLengths: List<Int>, val maxLineLength: Int)
