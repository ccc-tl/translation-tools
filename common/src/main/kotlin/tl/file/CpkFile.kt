package tl.file

import kio.BitInputStream
import kio.KioInputStream
import kio.util.align
import kio.util.child
import kio.util.readBytes
import kio.util.readNullTerminatedString
import kio.util.readString
import kio.util.toUnsignedInt
import kio.util.toWHex
import kio.util.writeNullTerminatedString
import tl.util.Log
import tl.util.ReadableByteArrayOutputStream
import tl.util.SpaceAllocationTracker
import tl.util.SpaceAllocator
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

/**
 * .CPK unpacker and in-place patching
 * Unpacker and decompression ported from vgm_ripping
 * */
class CpkFile(val file: File, private val log: Log = Log()) {
  private val utfStringCache = mutableMapOf<Long, String>()

  /**
   * Patches CPK in-place.
   * @param patchedFiles map of patched files that should replace stock content
   * @param dataAlign data alignment of this CPK file
   * @param insertNewFilesAt where to put new file content, typically this is the length of original CPK
   * @param allowBlockReplace if true then in-place patcher will try to replace unused overridden files data with patched files
   * allowing to reduce output CPK size. WARNING: this is a potentially dangerous operation if performed on already patched
   * CPK when the new patch file list is different from previous patch list. Only set this to true when working on clean
   * CPK.
   * @param rewriteFileName lambda that should return new file name for file, new file name must be shorter or equal than original path
   */
  fun patchInPlace(
    patchedFiles: Map<String, File>,
    dataAlign: Int = 2048,
    insertNewFilesAt: Long = -1,
    allowBlockReplace: Boolean = false,
    rewriteFileName: (String) -> String? = { null },
  ): List<CpkPatchedFile> {
    val input = RandomAccessFile(file, "rw")

    val tocTable = createUtfTable(input, 0)
    val tocOffset = queryUtf(input, tocTable, 0, "TocOffset") as Long
    val files = queryUtf(input, tocTable, 0, "Files") as Int
    val fileTable = createUtfTable(input, tocOffset)

    if (allowBlockReplace && insertNewFilesAt != -1L && insertNewFilesAt != input.length()) {
      error("Attempted block replace on CPK that was most likely already patched. (insertNewFilesAt must be -1L or exactly CPK length)")
    }
    val spaceAlloc: SpaceAllocator?
    if (allowBlockReplace) {
      val allocTracker = SpaceAllocationTracker(dataAlign)
      log.info("Scanning for free blocks...")
      repeat(files) { index ->
        val dirName = queryUtf(input, fileTable, index, "DirName") as String
        val fileName = queryUtf(input, fileTable, index, "FileName") as String
        val fileAbsOffset = tocOffset + queryUtf(input, fileTable, index, "FileOffset") as Long
        val fileSize = queryUtf(input, fileTable, index, "FileSize") as Int
        val relPath = "$dirName/$fileName"
        allocTracker.addOffsetUse(fileAbsOffset, fileSize)
        if (relPath in patchedFiles) {
          allocTracker.removeOffsetUse(fileAbsOffset)
        }
      }
      spaceAlloc = allocTracker.compactFree()
    } else {
      spaceAlloc = null
    }

    var placeNextFileAt = insertNewFilesAt
    if (placeNextFileAt == -1L) {
      placeNextFileAt = input.length()
    }
    input.setLength(placeNextFileAt)

    fun getPatchedInsertAbsOffset(patchSize: Int): Long {
      val block = spaceAlloc?.allocateBestBlockFor(patchSize)
      if (block != null) {
        return block.offset
      }
      input.seek(input.length())
      input.align(dataAlign.toLong())
      return input.filePointer
    }

    log.info("Patching files...")
    val insertCache = mutableMapOf<String, CpkPatchedFile>()
    repeat(files) { index ->
      val dirName = queryUtf(input, fileTable, index, "DirName") as String
      val fileName = queryUtf(input, fileTable, index, "FileName") as String
      val fileOffset = queryUtf(input, fileTable, index, "FileOffset") as Long
      val newFileName = rewriteFileName(fileName)
      val relPath = "$dirName/$fileName"
      if (relPath in patchedFiles) {
        val patch = insertCache.getOrPut(relPath) {
          val fileSize = queryUtf(input, fileTable, index, "FileSize") as Int
          val extractSize = queryUtf(input, fileTable, index, "ExtractSize") as Int
          val patchedFile = patchedFiles.getValue(relPath)
          val patchInsertAbsOffset = getPatchedInsertAbsOffset(patchedFile.length().toInt())
          log.info("Inserting $relPath at ${patchInsertAbsOffset.toWHex()}")
          input.seek(patchInsertAbsOffset)
          input.write(patchedFile.readBytes())
          CpkPatchedFile(
            relPath,
            newFileName,
            fileOffset + tocOffset,
            fileSize,
            extractSize,
            patchInsertAbsOffset,
            patchedFile.length().toInt(),
          )
        }
        log.info("Patching $relPath at ${fileOffset.toWHex()} using content at ${patch.offset.toWHex()}")
        arrayOf("FileSize", "ExtractSize").forEach { columnName ->
          patchUtf(
            input,
            fileTable,
            index,
            columnName,
            writer = { offset ->
              patch.writeNewSizeAt.add(offset)
              input.seek(offset)
              input.writeInt(patch.size)
            },
          )
        }
        patchUtf(
          input,
          fileTable,
          index,
          "FileOffset",
          writer = { offset ->
            patch.writeNewOffsetAt.add(offset)
            input.seek(offset)
            input.writeLong(patch.offset - tocOffset)
          },
        )
        if (newFileName != null) {
          patchUtf(
            input,
            fileTable,
            index,
            "FileName",
            writer = { offset ->
              input.seek(offset)
              val stringOffset = fileTable.baseOffset + input.readInt()
              patch.writeNewFileNameAt.add(stringOffset)
              input.seek(stringOffset)
              input.writeNullTerminatedString(newFileName)
            },
          )
        }
      }
    }

    input.close()
    return insertCache.values.toList()
  }

  fun getFileDescriptors(): List<CpkFileDescriptor> {
    val input = RandomAccessFile(file, "rw")
    val tocTable = createUtfTable(input, 0)
    val tocOffset = queryUtf(input, tocTable, 0, "TocOffset") as Long
    val files = queryUtf(input, tocTable, 0, "Files") as Int
    val fileTable = createUtfTable(input, tocOffset)
    val descriptors = mutableListOf<CpkFileDescriptor>()
    repeat(files) { index ->
      val dirName = queryUtf(input, fileTable, index, "DirName") as String
      val fileName = queryUtf(input, fileTable, index, "FileName") as String
      val fileAbsOffset = tocOffset + queryUtf(input, fileTable, index, "FileOffset") as Long
      val fileSize = queryUtf(input, fileTable, index, "FileSize") as Int
      val extractSize = queryUtf(input, fileTable, index, "ExtractSize") as Int
      val relPath = "$dirName/$fileName"
      descriptors.add(CpkFileDescriptor(relPath, fileAbsOffset, fileSize, extractSize))
    }
    input.close()
    return descriptors
  }

  fun extractTo(outDir: File, ignoreNotEmptyOut: Boolean = false) {
    extractSpecified(outDir, ignoreNotEmptyOut, null)
  }

  fun extractSpecified(outDir: File, ignoreNotEmptyOut: Boolean = false, paths: List<String>?) {
    if (!ignoreNotEmptyOut && outDir.exists() && outDir.list()!!.isNotEmpty()) {
      log.fatal("outDir is not empty")
    }
    outDir.mkdirs()

    val input = RandomAccessFile(file, "r")
    with(input) {
      if (readString(4) != "CPK ") {
        log.fatal("No CPK magic string inside input file")
      }
      val tocTable = createUtfTable(input, 0)
      val tocOffset = queryUtf(input, tocTable, 0, "TocOffset") as Long
      val contentOffset = queryUtf(input, tocTable, 0, "ContentOffset") as Long
      val files = queryUtf(input, tocTable, 0, "Files") as Int
      log.info("TOC at $tocOffset")
      log.info("Content at $contentOffset")
      log.info("Files: $files")

      seek(tocOffset)
      if (readString(4) != "TOC ") {
        log.fatal("No TOC signature at offset $tocOffset")
      }
      val fileTable = createUtfTable(input, tocOffset)
      log.startProgress()
      repeat(files) { index ->
        val dirName = queryUtf(input, fileTable, index, "DirName") as String
        val fileName = queryUtf(input, fileTable, index, "FileName") as String
        val fileSize = queryUtf(input, fileTable, index, "FileSize") as Int
        val extractSize = queryUtf(input, fileTable, index, "ExtractSize") as Int
        val fileOffset = queryUtf(input, fileTable, index, "FileOffset") as Long
        val path = "$dirName/$fileName"
        if (paths != null) {
          if (!paths.contains(path)) {
            return@repeat
          }
        }
        val outFile = outDir.child(path)
        if (outFile.exists()) {
          return@repeat
        }
        outFile.parentFile.mkdirs()
        outFile.createNewFile()

        val baseOffset = fileOffset + tocOffset

        seek(baseOffset)
        if (extractSize > fileSize) {
          log.progress(index, files, "Extract and decompress $path")
          val compressedBytes = readBytes(fileSize)
          val decompressedBytes = decompressLayla(compressedBytes, extractSize, log)
          outFile.writeBytes(decompressedBytes)
        } else {
          log.progress(index, files, "Extract $path")
          outFile.writeBytes(readBytes(fileSize))
        }
      }
      log.endProgress()

      close()
    }

    log.info("Done")
  }

  private fun createUtfTable(input: RandomAccessFile, initialOffset: Long): UtfTable = with(input) {
    val offset = initialOffset + 0x10
    seek(offset)
    if (readString(4) != "@UTF") {
      log.fatal("No UTF table at $offset")
    }
    val table = UtfTable()
    table.tableOffset = offset
    table.tableSize = readInt()
    table.schemaOffset = 0x20
    table.rowsOffset = readInt()
    table.stringTableOffset = readInt()
    table.dataOffset = readInt()
    table.nameString = readInt()
    table.columns = readShort()
    table.rowWidth = readShort()
    table.rows = readInt()

    table.schemas = arrayOfNulls(table.columns.toInt())
    table.baseOffset = table.stringTableOffset + 0x8 + offset

    repeat(table.columns.toInt()) { index ->
      val schema = UtfColumnSchema()
      table.schemas[index] = schema
      schema.type = readByte().toUnsignedInt()
      schema.columnName = readInt()

      if (schema.type and COLUMN_STORAGE_MASK == COLUMN_STORAGE_CONSTANT) {
        schema.constantOffset = filePointer
        when (val typeFlag = schema.type and COLUMN_TYPE_MASK) {
          COLUMN_TYPE_STRING -> readString(4)
          COLUMN_TYPE_DATA -> readString(8)
          COLUMN_TYPE_FLOAT -> readString(4)
          COLUMN_TYPE_8BYTE2 -> readString(8)
          COLUMN_TYPE_8BYTE -> readString(8)
          COLUMN_TYPE_4BYTE2 -> readString(4)
          COLUMN_TYPE_4BYTE -> readString(4)
          COLUMN_TYPE_2BYTE2 -> readString(2)
          COLUMN_TYPE_2BYTE -> readString(2)
          COLUMN_TYPE_1BYTE2 -> readString(1)
          COLUMN_TYPE_1BYTE -> readString(1)
          else -> error("Unknown constant type: $typeFlag")
        }
      }
    }
    return table
  }

  private fun queryUtf(input: RandomAccessFile, table: UtfTable, index: Int, name: String): Any = with(input) {
    for (i in index until table.rows) {
      val widthOffset = i * table.rowWidth
      var rowOffset = table.tableOffset + 8 + table.rowsOffset + widthOffset

      repeat(table.columns.toInt()) { index ->
        val schema = table.schemas[index]!!

        val dataOffset = if (schema.constantOffset >= 0) {
          schema.constantOffset
        } else {
          rowOffset
        }

        val oldReadAddr: Long
        val readBytes: Long

        val value: Any
        if (schema.type and COLUMN_STORAGE_MASK == COLUMN_STORAGE_ZERO) {
          value = 0
        } else {
          seek(dataOffset)
          val typeFlag = schema.type and COLUMN_TYPE_MASK
          oldReadAddr = filePointer
          when (typeFlag) {
            COLUMN_TYPE_STRING -> {
              val stringOffset = readInt()
              val stringAbsOffset = table.baseOffset + stringOffset
              value = utfStringCache.getOrPut(stringAbsOffset) {
                seek(stringAbsOffset)
                readNullTerminatedString(Charsets.US_ASCII)
              }
              readBytes = 4L
            }
            COLUMN_TYPE_DATA -> {
              val varDataOffset = readInt()
              val varDataSize = readInt()
              seek(table.baseOffset + varDataOffset)
              value = readBytes(varDataSize)
              readBytes = 8L
            }
            COLUMN_TYPE_FLOAT -> {
              value = readFloat()
              readBytes = 4L
            }
            COLUMN_TYPE_8BYTE, COLUMN_TYPE_8BYTE2 -> {
              value = readLong()
              readBytes = 8L
            }
            COLUMN_TYPE_4BYTE, COLUMN_TYPE_4BYTE2 -> {
              value = readInt()
              readBytes = 4L
            }
            COLUMN_TYPE_2BYTE, COLUMN_TYPE_2BYTE2 -> {
              value = readShort()
              readBytes = 2L
            }
            COLUMN_TYPE_1BYTE, COLUMN_TYPE_1BYTE2 -> {
              value = readByte()
              readBytes = 1L
            }
            else -> log.fatal("Unknown constant type: $typeFlag")
          }

          if (schema.constantOffset < 0) {
            rowOffset = oldReadAddr + readBytes
          }
        }
        val columnNameAbsOffset = table.baseOffset + schema.columnName
        val columnName = utfStringCache.getOrPut(columnNameAbsOffset) {
          seek(columnNameAbsOffset)
          readNullTerminatedString(Charsets.US_ASCII)
        }
        if (columnName == name) {
          return value
        }
      }
    }
    log.fatal("No UTF match")
  }

  private fun patchUtf(
    input: RandomAccessFile,
    table: UtfTable,
    index: Int,
    name: String,
    writer: (offset: Long) -> Unit,
  ) = with(input) {
    for (i in index until table.rows) {
      val widthOffset = i * table.rowWidth
      var rowOffset = table.tableOffset + 8 + table.rowsOffset + widthOffset

      repeat(table.columns.toInt()) { index ->
        val schema = table.schemas[index]!!

        val dataOffset = if (schema.constantOffset >= 0) {
          schema.constantOffset
        } else {
          rowOffset
        }

        val oldReadAddr: Long
        val readBytes: Long

        val offset: Long
        if (schema.type and COLUMN_STORAGE_MASK == COLUMN_STORAGE_ZERO) {
          error("Can't patch COLUMN_STORAGE_ZERO")
        } else {
          seek(dataOffset)
          val typeFlag = schema.type and COLUMN_TYPE_MASK
          oldReadAddr = filePointer
          when (typeFlag) {
            COLUMN_TYPE_STRING -> {
              offset = filePointer
              readBytes = 4L
            }
            COLUMN_TYPE_DATA -> {
              offset = filePointer
              readBytes = 8L
            }
            COLUMN_TYPE_FLOAT -> {
              offset = filePointer
              readBytes = 4L
            }
            COLUMN_TYPE_8BYTE, COLUMN_TYPE_8BYTE2 -> {
              offset = filePointer
              readBytes = 8L
            }
            COLUMN_TYPE_4BYTE, COLUMN_TYPE_4BYTE2 -> {
              offset = filePointer
              readBytes = 4L
            }
            COLUMN_TYPE_2BYTE, COLUMN_TYPE_2BYTE2 -> {
              offset = filePointer
              readBytes = 2L
            }
            COLUMN_TYPE_1BYTE, COLUMN_TYPE_1BYTE2 -> {
              offset = filePointer
              readBytes = 1L
            }
            else -> log.fatal("Unknown constant type: $typeFlag")
          }

          if (schema.constantOffset < 0) {
            rowOffset = oldReadAddr + readBytes
          }
        }

        val columnNameAbsOffset = table.baseOffset + schema.columnName
        val columnName = utfStringCache.getOrPut(columnNameAbsOffset) {
          seek(columnNameAbsOffset)
          readNullTerminatedString(Charsets.US_ASCII)
        }
        if (columnName == name) {
          writer(offset)
          return@with
        }
      }
    }
    log.fatal("No UTF match")
  }

  private class UtfTable {
    var tableOffset = 0L
    var tableSize = 0
    var schemaOffset = 0
    var rowsOffset = 0
    var stringTableOffset = 0
    var dataOffset = 0
    var nameString = 0
    var columns: Short = 0
    var rowWidth: Short = 0
    var rows = 0

    var schemas: Array<UtfColumnSchema?> = arrayOf()
    var baseOffset = 0L
  }

  private class UtfColumnSchema {
    var type = 0
    var columnName = 0
    var constantOffset = -1L
  }

  private companion object {
    const val COLUMN_STORAGE_MASK = 0xf0
    const val COLUMN_TYPE_MASK = 0x0f

    const val COLUMN_STORAGE_CONSTANT = 0x30
    const val COLUMN_STORAGE_ZERO = 0x10

    const val COLUMN_TYPE_DATA = 0x0b
    const val COLUMN_TYPE_STRING = 0x0a
    const val COLUMN_TYPE_FLOAT = 0x08
    const val COLUMN_TYPE_8BYTE2 = 0x07
    const val COLUMN_TYPE_8BYTE = 0x06
    const val COLUMN_TYPE_4BYTE2 = 0x05
    const val COLUMN_TYPE_4BYTE = 0x04
    const val COLUMN_TYPE_2BYTE2 = 0x03
    const val COLUMN_TYPE_2BYTE = 0x02
    const val COLUMN_TYPE_1BYTE2 = 0x01
    const val COLUMN_TYPE_1BYTE = 0x00
  }
}

fun decompressLayla(fileBytes: ByteArray, extractSize: Int, log: Log = Log()): ByteArray {
  val input = KioInputStream(fileBytes)
  if (input.readString(8) != "CRILAYLA") {
    log.fatal("Not compressed using CRILAYLA")
  }
  val sizeOrig = input.readInt()
  val sizeComp = input.readInt()
  val dataComp = input.readBytes(sizeComp)
  val prefix = input.readBytes(0x100)
  input.close()

  dataComp.reverse()

  val out = ReadableByteArrayOutputStream(sizeOrig)

  with(BitInputStream(dataComp)) {
    while (out.size() < sizeOrig) {
      val sizes = sequence {
        yield(2)
        yield(3)
        yield(5)
        while (true) yield(8)
      }

      if (readBit()) {
        var repetitions = 3
        val lookBehind = readInt(13) + 3

        for (size in sizes) {
          val marker = readInt(size)
          repetitions += marker
          if (marker != (1 shl size) - 1) {
            break
          }
        }

        repeat(repetitions) {
          val byte = out.at(out.size() - lookBehind)
          out.write(byte)
        }
      } else {
        val byte = readByte()
        out.write(byte)
      }

      if (sizeComp - 1 == pos && posInCurrentByte == 7) {
        break
      }
    }
  }

  val combined = ByteArrayOutputStream(extractSize)
  combined.write(prefix)
  combined.write(out.toByteArray().reversedArray())
  return combined.toByteArray()
}

class CpkPatchedFile(
  val relPath: String,
  val newFileName: String?,
  val oldOffset: Long,
  val oldSize: Int,
  val oldExtractSize: Int,
  val offset: Long,
  val size: Int,
) {
  val writeNewOffsetAt = mutableListOf<Long>()
  val writeNewSizeAt = mutableListOf<Long>()
  val writeNewFileNameAt = mutableListOf<Long>()
}

data class CpkFileDescriptor(
  val relPath: String,
  val offset: Long,
  val size: Int,
  val extractSize: Int,
)
