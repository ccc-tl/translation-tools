package tl.extra.cli

import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = ExtraCommand()
  .subcommands(
    ExtractCommand()
      .subcommands(
        ExtractCpkCommand(),
        ExtractPackageCommand(),
        ExtractPakCommand(),
        ExtractPhdSoundCommand(),
      ),
    RebuildCommand()
      .subcommands(RebuildPakCommand()),
  )
  .main(args)
