package tl.file

import kio.LERandomAccessFile
import kio.util.align
import kio.util.writeNullTerminatedString
import tl.util.writeDatString
import java.io.File

class PatchFsWriter(private val srcFiles: Map<String, File>, private val nestedPatchFsNames: List<String> = emptyList()) {
  fun writeTo(outputFile: File) {
    with(LERandomAccessFile(outputFile)) {
      setLength(0)
      val headerSize = 0x10L
      // header
      writeNullTerminatedString("PATCHFS")
      writeInt(srcFiles.size)
      writeInt(nestedPatchFsNames.size)
      if (length() != headerSize) {
        error("Invalid header size")
      }
      val nestedDataStartsAt = filePointer
      setLength(filePointer + nestedPatchFsNames.size * 0x8)
      seek(length())
      val fileDataStartsAt = filePointer
      setLength(filePointer + srcFiles.size * 0x20)
      seek(length())

      // write nested PatchFs names section
      val nestedPatchFsNamesPtrs = nestedPatchFsNames.map {
        val ptr = length()
        writeDatString(it, Charsets.US_ASCII)
        ptr
      }
      align(0x10)

      // write file path section
      val filePathsPtrs = srcFiles.map { (fsPath, _) ->
        val ptr = length()
        writeDatString(fsPath, Charsets.US_ASCII)
        ptr
      }
      align(0x10)

      // write content section
      val contentPtrs = srcFiles.map { (_, file) ->
        val ptr = length()
        write(file.readBytes())
        align(0x10)
        ptr
      }

      val lengths = srcFiles.map { (_, file) -> file.length() }

      // update tables of content
      seek(nestedDataStartsAt)
      nestedPatchFsNamesPtrs.forEach {
        writeLong(it)
      }
      seek(fileDataStartsAt)
      repeat(srcFiles.size) { fileIdx ->
        writeLong(filePathsPtrs[fileIdx])
        writeLong(contentPtrs[fileIdx])
        writeLong(lengths[fileIdx])
        writeLong(0)
      }
      close()
    }
  }
}
