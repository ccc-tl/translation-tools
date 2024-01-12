package tl.extra.patcher

import kio.util.relativizePath
import kio.util.walkDir
import java.io.File

class FileCollector(
  private val projectDir: File,
  private val collectFor: String,
  private val warn: (String) -> Unit,
) {
  val files = mutableMapOf<String, File>()

  fun includeDir(dir: File) {
    walkDir(dir) {
      val path = it.relativizePath(dir)
      if (path.startsWith(".git/") || path == ".gitignore") {
        return@walkDir
      }
      if (path in files) {
        warn("Ignoring $collectFor replacement file: ${it.relativizePath(projectDir)}. Already included from other source.")
      } else {
        files[path] = it
      }
    }
  }
}
