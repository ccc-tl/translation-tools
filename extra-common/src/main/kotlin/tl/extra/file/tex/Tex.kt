package tl.extra.file.tex

class Block(val id: Int, val bytes: ByteArray)

class Chunk(val id: Int, val bytes: ByteArray)

class Cct(val table: List<Int>, val chunkSize: Int) {
  fun isLinear(): Boolean {
    var lastChunk = 0
    table.forEach {
      if (lastChunk != it)
        return false
      lastChunk++
    }
    return true
  }
}
