package tl.extra.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import tl.extra.file.PakFileEntry
import tl.extra.patcher.CmpCompressor
import tl.extra.patcher.PakWriter

class RebuildPakCommand : CliktCommand(name = "pak", help = "Rebuild PAK from source files.") {
  private val compress by option("-c", "--compress", help = "Compress IECP data (.CMP files).")
    .flag(default = false)
  private val overwrite by option("-o", "--overwrite", help = "Overwrite destination file if it already exists.")
    .flag(default = false)

  private val source by argument(help = "Source PAK files directory.")
    .file(mustExist = true, mustBeReadable = true, canBeFile = false)
    .validate {
      require(it.listFiles()?.size != 0) { "Source directory can't be empty" }
    }
  private val dest by argument(help = "Output file.")
    .file(canBeDir = false)

  override fun run() {
    if (dest.exists() && !overwrite) {
      throw PrintMessage("Output file already exists, use -o to overwrite.")
    }

    dest.parentFile.mkdirs()
    val sourceFiles = source.listFiles()
    val hasPaths = sourceFiles?.firstOrNull()?.name?.contains(" - ")
      ?: error("Failed to determinate if PAK has paths")

    val entries = source.listFiles()
      ?.map {
        val parts = it.name.split(" - ", limit = 2)
        val invalidFormatMsg = "Invalid file name: ${it.name}. Extract PAK with the 'extract pak' command before rebuilding."
        if ((hasPaths && parts.size != 2) || (!hasPaths && parts.size != 1)) {
          throw PrintMessage(invalidFormatMsg)
        }
        val index = parts[0].toIntOrNull()
          ?: throw PrintMessage(invalidFormatMsg)
        val path = (parts.getOrNull(1) ?: "").replace("$", "/")
        PakFileEntry(index, path, it.readBytes())
      }
      ?.sortedBy { it.index }
      ?: emptyList()

    PakWriter(entries, writePaths = hasPaths).writeTo(dest)
    if (compress) {
      println("Compressing, this might take a while...")
      dest.writeBytes(CmpCompressor(dest, secondPass = true).getData())
    }
    println("Done")
  }
}
