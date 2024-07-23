package tl.extra.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.types.file
import tl.extra.util.PackageExtractor

class ExtractPackageCommand : CliktCommand(name = "package", help = "Batch extract PAK and CMP files into an unified hierarchy.") {
  private val source by argument(help = "Source directory of the extracted CPK.")
    .file(mustExist = true, mustBeReadable = true, canBeFile = false)
  private val dest by argument(help = "Output directory, must be empty.")
    .file(mustExist = true, mustBeWritable = true, canBeFile = false)
    .validate { requireEmptyOutputDirectory(it) }

  override fun run() {
    PackageExtractor(source, dest)
    println("Done")
  }
}
