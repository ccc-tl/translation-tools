package tl.extra.ccc.cli

import kio.util.relativizePath
import kio.util.walkDir
import tl.extra.ccc.cccUnpack
import tl.util.ensureNoSdbmHashCollisions

fun main() {
  val paths = mutableListOf<ByteArray>()
  walkDir(cccUnpack) {
    val relPath = it.relativizePath(cccUnpack)
    paths.add(relPath.toByteArray())
  }
  ensureNoSdbmHashCollisions(paths)
  println("Done")
}
