package tl.extra.file

import kio.KioInputStream
import kio.util.WINDOWS_932
import tl.util.readDatString
import java.io.File
import java.nio.charset.Charset

class FixedSizeTextBinFile(file: File, charset: Charset = Charsets.WINDOWS_932) {
  val entries: List<Pair<Int, String>>

  init {
    val entries = mutableListOf<Pair<Int, String>>()
    with(KioInputStream(file)) {
      val entryCount = readInt()
      readInt()
      repeat(entryCount) {
        val index = readInt()
        val text = readDatString(maintainStreamPos = true, charset = charset)
        skip(0x40)
        entries.add(index to text)
      }
      close()
    }
    this.entries = entries
  }
}
