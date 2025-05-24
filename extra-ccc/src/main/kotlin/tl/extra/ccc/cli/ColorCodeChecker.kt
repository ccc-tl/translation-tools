package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.patcher.translation.CccTranslation
import java.io.File

fun main() {
  ColorCodeChecker(
    cccToolkit.child("src/translation/dat"),
  ).process()
  println("Done")
}

class ColorCodeChecker(
  translationDir: File,
) {
  private val translation = CccTranslation(
    translationDir.child("script-japanese.txt"),
    stripNewLine = true,
    stripJpAndNotesNewLine = false,
  )

  fun process() {
    val scanRange = 0 until translation.jpTexts.size

    val colorRegex = """#C\d{9}""".toRegex()
    scanRange.mapNotNull { offset ->
      val jp = translation.getJapanese(offset)
      val en = translation.getTranslation(offset, allowBlank = true)
      val jpMatches = colorRegex.findAll(jp).map { it.value }.toList()
      val enMatches = colorRegex.findAll(en).map { it.value }.toList()
      if ((jpMatches.isNotEmpty() || enMatches.isNotEmpty()) && !equalsIgnoreOrder(jpMatches, enMatches)) {
        println("Mismatch at $offset: ${en.replace("\n", "\\n")}. Expected: $jpMatches got $enMatches")
      }
    }
  }

  private fun <T> equalsIgnoreOrder(list1: List<T>, list2: List<T>) = list1.size == list2.size && list1.toSet() == list2.toSet()
}
