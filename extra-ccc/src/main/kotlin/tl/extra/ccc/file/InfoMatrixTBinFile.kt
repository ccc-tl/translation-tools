package tl.extra.ccc.file

import kio.KioInputStream
import kio.util.appendLine
import tl.util.readDatString
import java.io.File

class InfoMatrixTBinFile(dFile: File) {
  val entries = mutableListOf<String>()

  init {
    with(KioInputStream(dFile)) {
      skip(0x4)
      while (true) {
        entries.add(readDatString())
        skipNullBytes()
        if (eof()) {
          break
        }
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
