package tl.extra.file.tex

import kio.util.child
import tl.extra.file.PakFile
import java.io.File

class TxbFile(bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  val textures: Map<Int, TexFile>
  var emptyEntryCount = 0
    private set

  init {
    val pak = PakFile(bytes)

    val textures = mutableMapOf<Int, TexFile>()
    pak.entries.forEachIndexed pakEntry@{ i, entry ->
      if (entry.bytes.isEmpty()) {
        println("WARN: Skipped empty file, entry index $i, name '${entry.path}'")
        emptyEntryCount++
        return@pakEntry
      }
      if (textures.containsKey(i)) {
        error("Texture index duplicate: $i")
      }
      textures[i] = TexFile(entry.bytes)
    }
    this.textures = textures
  }

  fun writeToFolder(folder: File, cropOutImage: Boolean = true) {
    textures.forEach { (i, tex) ->
      tex.writeToPng(folder.child("$i.png"), cropOutImage)
    }
  }
}
