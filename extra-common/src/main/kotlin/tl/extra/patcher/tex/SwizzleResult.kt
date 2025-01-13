package tl.extra.patcher.tex

import tl.extra.file.tex.Chunk

class SwizzleResult(
  val chunks: List<Chunk>,
  val chunksPerLineCount: Int,
  val chunkLinesCount: Int,
  val bytes: ByteArray,
)
