package tl.extra.file

import kio.KioInputStream
import kio.util.padArray
import java.io.File

/** .PAK file parser */
class PakFile(bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  val headerSize: Int
  val offsets: Array<Int>
  val entries: Array<PakFileEntry>
  val hasPaths: Boolean

  init {
    val fileOffsets = mutableListOf<Int>()
    val fileEntries = mutableListOf<PakFileEntry>()

    with(KioInputStream(bytes)) {
      val filesCount = readShort().toInt()
      hasPaths = readShort() == 0x8000.toShort()

      val fileSizes = mutableListOf<Int>()
      repeat(filesCount) {
        fileSizes.add(readInt())
      }

      align(16)
      headerSize = pos()

      repeat(filesCount) { index ->
        val path = if (hasPaths) readString(64).replace("\u0000", "") else ""
        fileOffsets.add(pos())
        fileEntries.add(PakFileEntry(index, path, readBytes(fileSizes[index])))
      }

      close()
    }

    offsets = fileOffsets.toTypedArray()
    entries = fileEntries.toTypedArray()
  }

  fun getEntry(path: String): PakFileEntry {
    return entries.getEntry(path)
  }
}

fun Array<PakFileEntry>.sizeInGame(): Int = this.map { it.bytes.size + 0x40 }.sum()

fun Array<PakFileEntry>.getDataLocationInFile(baseAddress: Long, headerSize: Int, path: String): Long {
  var address = baseAddress + headerSize
  forEach { entry ->
    if (entry.path == path) {
      return address + 0x40
    }
    address += entry.bytes.size + 0x40
  }
  error("No such entry: $path")
}

fun Array<PakFileEntry>.getEntry(path: String): PakFileEntry {
  forEachIndexed { idx, entry ->
    if (entry.path == path) {
      return this[idx]
    }
  }
  error("No such entry: $path")
}

fun Array<PakFileEntry>.getEntryBytes(path: String): ByteArray = getEntry(path).bytes

fun Array<PakFileEntry>.replaceEntry(path: String, newBytes: ByteArray) {
  forEachIndexed { idx, entry ->
    if (entry.path == path) {
      this[idx] = PakFileEntry(this[idx].index, path, padArray(newBytes))
      return
    }
  }
  error("No such entry: $path")
}

fun Array<PakFileEntry>.replaceEntry(id: Int, newPath: String, newBytes: ByteArray) {
  forEachIndexed { idx, _ ->
    if (idx == id) {
      this[idx] = PakFileEntry(this[idx].index, newPath, padArray(newBytes))
      return
    }
  }
  error("No such entry with id: $id")
}

class PakFileEntry(val index: Int, val path: String, val bytes: ByteArray) {
  fun writeToFile(outFile: File) {
    outFile.parentFile.mkdirs()
    outFile.writeBytes(bytes)
  }
}
