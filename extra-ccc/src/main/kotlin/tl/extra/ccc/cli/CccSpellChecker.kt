package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.extra.ccc.util.stripControlCodes
import tl.extra.fateOutput
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

fun main() {
  val out = fateOutput.child("spellchecking").apply { mkdir() }
  CccSpellChecker(cccToolkit, out)
  println("Done")
}

class CccSpellChecker(projectDir: File, outDir: File) {
  private val translationDir = projectDir.child("src/translation")
  private val htmlOut = outDir.child("ccc-spell-check.html")
  private val dictionary = listOf("src/dict/user.txt", "src/dict/en.big.txt")
    .flatMap { projectDir.child(it).readLines() }
    .map { it.lowercase() }
    .toSet()
  private val misspellings = mutableMapOf<String, AtomicInteger>()

  private val sRegex = """'s$""".toRegex()
  private val wordExtractingRegex = """[*―.(\["]*((?:\w|')+)[+～*.―,;!?~:")\]]*""".toRegex()
  private val numberRegex = """(\d\dth|B\d\d|W\d\d|H\d\d|B\d\d/W\d\d/H\d\d|\d\d\dcm|\d\dkg)""".toRegex()

  private val units = translationDir.listFiles()!!
    .filter { it.isDirectory }
    .filter { it.name !in listOf(".git") }
    .map { it.child("script-japanese.txt") }
    .filter { it.exists() }
    .associateWith { CccTranslation(it) }

  init {
    val unitsMisspelling = units.mapValues { (file, unit) ->
      println("Processing ${file.parentFile.name}...")
      unit.enTexts.mapIndexedNotNull { index, enText ->
        val enStripped = stripControlCodes(
          enText,
          replacement = " ",
        )
        when {
          enStripped.isNotBlank() -> mapTextToSpellChecked(index, enStripped)
          else -> null
        }
      }
    }

    htmlOut.writeText(
      """
      <!doctype html>
      <html lang="en">
      <head>
        <meta charset="utf-8">
        <title>CCC Spell Check</title>
        ${
        unitsMisspelling.entries.joinToString("\n") { unit ->
          """
            <h1>${unit.key.parentFile.name}</h1>
            ${unit.value.joinToString("\n") { "<p>$it</p>" }}
          """
        }
      }
      </head>
      <body>
      </body>
      </html>
      """.trimIndent(),
    )

    val summary = unitsMisspelling.map { "${it.key.parentFile.name}: ${it.value.size}" }
      .joinToString("\n")
    println("\nSummary:")
    println(summary)
    println("\nTop 20 items:")
    misspellings
      .map { Pair(it.key, it.value.get()) }
      .sortedByDescending { it.second }
      .take(20)
      .forEach { println(it) }
  }

  private fun mapTextToSpellChecked(index: Int, text: String): String? {
    var hasErrors = false

    val newText = text
      .replace("…", " ")
      .replace("...", " ")
      .replace("―", " ")
      .replace("鷙", " ")
      .replace("薗", " ")
      .replace("♪", " ")
      .replace("⑥", " ")
      .replace(numberRegex, " ")
      .split(" ")
      .joinToString(separator = " ") { token ->
        val strippedToken = token
          .let {
            when {
              token.length >= 3 && (token[0] == token[2] || token[0] == token[2].uppercaseChar()) && token[1] == '-' -> token.drop(2)
              else -> token
            }
          }
          .replace(sRegex, "")
          .replace(wordExtractingRegex) { it.groups.last()!!.value }.lowercase()
        when {
          strippedToken.isBlank() -> token
          strippedToken.all { it.isDigit() } -> token
          strippedToken.none { it.isLetter() } -> token
          !dictionary.contains(strippedToken) -> {
            hasErrors = true
            misspellings.getOrPut(strippedToken) { AtomicInteger(0) }.incrementAndGet()
            """<span style="color:red">$token</span>"""
          }
          else -> token
        }
      }

    return when {
      hasErrors -> "$index: $newText"
      else -> null
    }
  }
}
