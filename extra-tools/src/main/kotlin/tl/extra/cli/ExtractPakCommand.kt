package tl.extra.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kio.util.child
import tl.extra.file.CmpFile
import tl.extra.file.PakFile

class ExtractPakCommand : CliktCommand(name = "pak", help = "Extract PAK in a format suitable for rebuilding.") {
  private val decompress by option("-d", "--decompress", help = "Decompress IECP data (.CMP files).")
    .flag(default = false)

  private val source by argument(help = "Source PAK.")
    .file(mustExist = true, mustBeReadable = true, canBeDir = false)
  private val dest by argument(help = "Output directory, must be empty.")
    .file(mustExist = true, mustBeWritable = true, canBeFile = false)
    .validate { requireEmptyOutputDirectory(it) }

  override fun run() {
    val data = when {
      decompress -> CmpFile(source).getData()
      else -> source.readBytes()
    }
    val pak = PakFile(data)

    PakFile(data)
      .entries
      .forEach { entry ->
        val outName = when {
          pak.hasPaths -> {
            val flatPath = entry.path.replace("\\", "/").replace("/", "$")
            "${entry.index} - $flatPath"
          }
          else -> "${entry.index}"
        }
        val outFile = dest.child(outName)
        println("Write $outName")
        if (outFile.exists()) {
          error("Can't extract $outName because it already exists!")
        }
        outFile.writeBytes(entry.bytes)
      }
    println("Done")
  }
}
