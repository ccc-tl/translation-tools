package tl.extra.file

import kio.KioInputStream
import kio.util.WINDOWS_932
import tl.util.readDatString
import java.io.File
import java.nio.charset.Charset

class IndexedTextBinFile(file: File, charset: Charset = Charsets.WINDOWS_932) {
  val entries: List<Pair<Int, String>>

  init {
    val entries = mutableListOf<Pair<Int, String>>()
    with(KioInputStream(file)) {
      var lastIndex = 0
      while (true) {
        val index = readInt()
        if (index < lastIndex) {
          break
        }
        val length = readInt()
        if (length == 0) {
          break
        }
        val text = readDatString(fixUpNewLine = true, charset = charset)
        entries.add(index to text)
        lastIndex++
        if (eof()) {
          break
        }
      }
      close()
    }
    this.entries = entries
  }
}
