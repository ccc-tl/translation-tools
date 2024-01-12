package tl.extra.file

import kio.KioInputStream
import kio.util.WINDOWS_932
import java.io.EOFException
import java.io.File

class SjisFile(bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  val entries: List<SjisEntry>

  init {
    val entries = mutableListOf<SjisEntry>()
    with(KioInputStream(bytes)) {
      while (!eof()) {
        try {
          val l1 = readNullTerminatedString(Charsets.WINDOWS_932)
          val l2 = readNullTerminatedString(Charsets.WINDOWS_932)
          val l3 = readNullTerminatedString(Charsets.WINDOWS_932)
          val l4 = readNullTerminatedString(Charsets.WINDOWS_932)
          val l5 = readNullTerminatedString(Charsets.WINDOWS_932)
          if (arrayOf(l1, l2, l3, l4, l5).all { it.isNotBlank() }) {
            entries.add(SjisEntry(l1, l2, l3, l4, l5))
          }
        } catch (ignored: EOFException) {
          // assuming file can have some padding
        }
      }
      close()
    }
    this.entries = entries
  }
}

class SjisEntry(val l1: String, val l2: String, val l3: String, val l4: String, val l5: String) {
  fun isUnused(): Boolean =
    l1 in listOf("（予備）", "0") && l2 in listOf("（予備）", "0") && l3 == "0" && l4 == "0" && l5 == "0"
}
