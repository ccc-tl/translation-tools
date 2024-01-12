package tl.extra.ccc.file

import kio.KioInputStream
import java.io.File

class EfpFile(bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  val entries: Array<EfpFileEntry>

  init {
    val fileEntries = mutableListOf<EfpFileEntry>()

    with(KioInputStream(bytes)) {
      if (bytes.isEmpty()) return@with
      val magic = readStringAndTrim(0x4)
      if (magic !in listOf("EFP", "EFC")) {
        error("Not EFP / EFC file")
      }
      readInt()
      readInt()
      readInt()

      while (true) {
        val fileSize = readInt()
        if (fileSize == 0) {
          break
        }
        readInt()
        val fileName = readString(0x18)
        val fileContent = readBytes(fileSize - 0x20)
        fileEntries.add(EfpFileEntry(fileName, fileContent))
      }

      close()
    }

    entries = fileEntries.toTypedArray()
  }
}

class EfpFileEntry(val path: String, val bytes: ByteArray) {
  fun writeToFile(outFile: File) {
    outFile.parentFile.mkdirs()
    outFile.writeBytes(bytes)
  }
}
