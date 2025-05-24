package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.patcher.translation.CccTranslation
import java.io.File

fun main() {
  InDungeonVerify(
    cccToolkit.child("src/translation/indungeon"),
  ).process()
  println("Done")
}

class InDungeonVerify(
  translationDir: File,
) {
  private val translation = CccTranslation(
    translationDir.child("script-japanese.txt"),
    stripNewLine = false,
    replaceLiteralNewLine = false,
    failOnLiteralNewLine = true,
  )

  fun process() {
    val texts = mutableMapOf<String, MutableSet<String>>()

    repeat(translation.jpTexts.size) { index ->
      val jp = translation.getJapanese(index)
      val en = translation.getTranslation(index, allowBlank = true)
      texts.getOrPut(jp) { mutableSetOf() }
        .add(en)
    }
    texts
      .filter { it.value.size > 1 }
      .forEach { println(it) }
  }
}
