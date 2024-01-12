package tl.extra.patcher

import kio.KioOutputStream
import tl.extra.file.PakFileEntry
import java.io.File

/** Writes new .PAK file from specified entries. */
class PakWriter(private val entries: List<PakFileEntry>, private val writePaths: Boolean) {
  fun writeTo(outFile: File) {
    with(KioOutputStream(outFile)) {
      writeShort(entries.size.toShort())
      if (writePaths) {
        writeShort(0x8000.toShort())
      } else {
        writeShort(0.toShort())
      }
      entries.forEach {
        writeInt(it.bytes.size)
      }
      align(16)
      entries.forEach { entry ->
        if (writePaths) {
          writeString(entry.path, 64)
        }
        writeBytes(entry.bytes)
        align(16)
      }
      close()
    }
  }
}
