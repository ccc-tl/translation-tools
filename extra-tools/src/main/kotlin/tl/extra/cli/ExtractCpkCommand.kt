package tl.extra.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.types.file
import tl.file.CpkFile

class ExtractCpkCommand : CliktCommand(name = "cpk", help = "Extract CPK. Only tested with Fate/Extra and Fate/Extra CCC CPKs.") {
  private val source by argument(help = "Source CPK.")
    .file(mustExist = true, mustBeReadable = true, canBeDir = false)
  private val dest by argument(help = "Output directory, must be empty.")
    .file(mustExist = true, mustBeWritable = true, canBeFile = false)
    .validate { requireEmptyOutputDirectory(it) }

  override fun run() {
    CpkFile(source).extractTo(dest)
  }
}
