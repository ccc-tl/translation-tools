package tl.extra.file

import kio.KioInputStream
import tl.util.readDatString
import java.io.File

@Suppress("UNUSED_VARIABLE")
internal class ExtraInfoMatrixBinFile(dFile: File, tFile: File) {
  val entries: List<ExtraInfoMatrixEntry>

  init {
    val titles = mutableListOf<String>()
    val texts = mutableListOf<String>()
    val abbreviations = mutableListOf<String>()

    with(KioInputStream(dFile)) {
      val id = readInt()
      skip(0x50)
      repeat(3) {
        val text = readDatString(maintainStreamPos = true)
        titles.add(text)
        skip(0x20)
      }
      repeat(6) {
        val text = readDatString(maintainStreamPos = true)
        titles.add(text)
        skip(0x80)
      }
      close()
    }
    with(KioInputStream(tFile)) {
      val id = readInt()
      repeat(9) {
        val text = readDatString(maintainStreamPos = true, fixUpNewLine = true)
        texts.add(text)
        skip(0x1400)
      }
      repeat(9) {
        val text = readDatString(maintainStreamPos = true)
        abbreviations.add(text)
        skip(0x44)
      }
      close()
    }

    val entries = mutableListOf<ExtraInfoMatrixEntry>()
    if (titles.size != texts.size || texts.size != abbreviations.size) {
      error("Infomatrix content count mismatch")
    }
    repeat(titles.count()) { idx ->
      entries.add(ExtraInfoMatrixEntry(titles[idx], texts[idx], abbreviations[idx]))
    }
    this.entries = entries
  }
}

class ExtraInfoMatrixEntry(val title: String, val text: String, val abbreviation: String)
