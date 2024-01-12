package tl.extra.cli

import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import java.io.File

fun ArgumentTransformContext.requireEmptyOutputDirectory(file: File) {
  require(file.listFiles()?.size == 0) { "Output directory must be empty" }
}
