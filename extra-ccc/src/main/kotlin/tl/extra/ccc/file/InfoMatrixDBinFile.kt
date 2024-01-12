package tl.extra.ccc.file

import kio.KioInputStream
import kio.util.appendLine
import tl.util.readDatString
import java.io.File

class InfoMatrixDBinFile(dFile: File) {
  val entries = mutableListOf<String>()

  init {
    with(KioInputStream(dFile)) {
      skip(0x64)
      repeat(4) {
        entries.add(readDatString(maintainStreamPos = true))
        skip(0x20)
      }
      repeat(10) {
        entries.add(readDatString(maintainStreamPos = true))
        skip(0x80)
      }
      close()
    }
  }

  fun writeToStringBuilder(out: StringBuilder) {
    entries.forEach { text ->
      out.appendLine(text)
    }
  }
}
