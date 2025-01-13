package tl.extra.patcher.tex

import kio.SequentialArrayReader
import kio.SequentialArrayWriter
import tl.extra.file.tex.Chunk
import java.io.ByteArrayOutputStream

fun swizzle4Bpp(encodedBytes: ByteArray, width: Int, height: Int): SwizzleResult {
  val chunksPerLine = width / 32
  val chunkLines = height / 32
  val chunks = mutableListOf<Chunk>()
  var currentChunkIdx = 0
  val reader = SequentialArrayReader(encodedBytes)

  // for each chunk line
  repeat(chunkLines) {
    // create data store for chunks on current chunk line
    val newChunks = Array(chunksPerLine) {
      val chunk = Chunk(currentChunkIdx++, ByteArray(16 * 32)) // chunk is 16 bytes wide, 32 bytes tall
      chunks.add(chunk)
      SequentialArrayWriter(chunk.bytes)
    }

    // for each vertical pixel line of chunk
    repeat(32) {
      // for each chunk in chunk line
      repeat(chunksPerLine) { lineChunkIdx ->
        // for each horizontal pixel of chunk
        repeat(16) {
          newChunks[lineChunkIdx].write(reader.read())
        }
      }
    }
  }

  val output = ByteArrayOutputStream()
  chunks.forEach { output.write(it.bytes) }
  return SwizzleResult(chunks, chunksPerLine, chunkLines, output.toByteArray())
}

fun swizzle8Bpp(encodedBytes: ByteArray, width: Int, height: Int): SwizzleResult {
  val chunksPerLine = width / 16
  val chunkLines = height / 16
  val chunks = mutableListOf<Chunk>()
  var currentChunkIdx = 0
  val reader = SequentialArrayReader(encodedBytes)

  // for each chunk line
  repeat(chunkLines) {
    // create data store for chunks on current chunk line
    val newChunks = Array(chunksPerLine) {
      val chunk = Chunk(currentChunkIdx++, ByteArray(16 * 16)) // chunk is 32 bytes wide, 16 bytes tall
      chunks.add(chunk)
      SequentialArrayWriter(chunk.bytes)
    }

    // for each vertical pixel line of chunk
    repeat(16) {
      // for each chunk in chunk line
      repeat(chunksPerLine) { lineChunkIdx ->
        // for each horizontal pixel of chunk
        repeat(16) {
          newChunks[lineChunkIdx].write(reader.read())
        }
      }
    }
  }

  val output = ByteArrayOutputStream()
  chunks.forEach { output.write(it.bytes) }
  return SwizzleResult(chunks, chunksPerLine, chunkLines, output.toByteArray())
}
