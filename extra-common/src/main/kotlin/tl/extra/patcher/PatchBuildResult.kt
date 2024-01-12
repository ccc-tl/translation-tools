package tl.extra.patcher

import java.io.File

data class PatchBuildResult(
  val isoBuildDir: File,
  val patchFsFiles: List<File>,
)
