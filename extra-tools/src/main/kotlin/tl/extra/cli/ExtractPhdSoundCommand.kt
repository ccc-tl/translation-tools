package tl.extra.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import tl.util.PhdSoundProcessor

class ExtractPhdSoundCommand : CliktCommand(
  name = "phd",
  help = "Batch extract and convert PHD/PBD sound files into WAV. Only tested with Fate/Extra, Fate/Extra CCC and few other games."
) {
  private val noIdMap by option("--no-id-map", help = "Do not generate ID map JSON.")
    .flag(default = false)

  private val vgmstreamTool by argument(help = "Path to the vgmstream/test executable.")
    .file(mustExist = true, mustBeReadable = true, canBeDir = false)
  private val source by argument(help = "Source directory containing PHD/PBD files.")
    .file(mustExist = true, mustBeReadable = true, canBeFile = false)
  private val dest by argument(help = "Output directory, must be empty.")
    .file(mustExist = true, mustBeWritable = true, canBeFile = false)
    .validate { requireEmptyOutputDirectory(it) }

  override fun run() {
    PhdSoundProcessor(vgmstreamTool)
      .process(
        source,
        dest,
        writeIdMap = !noIdMap
      )
  }
}
