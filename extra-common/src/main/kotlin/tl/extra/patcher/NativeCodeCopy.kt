package tl.extra.patcher

import tl.util.copyFromJarRecursively
import java.io.File

fun copyNativeCode(owner: Any, envName: String, sourcePath: String, nativeDir: File, warn: (String) -> Unit) {
  val customNativeDir = System.getenv(envName).let {
    if (it != null) File(it) else null
  }
  if (customNativeDir?.exists() == true) {
    warn("Using custom native dir")
    customNativeDir.copyRecursively(nativeDir, overwrite = true)
  } else {
    copyFromJarRecursively(owner, sourcePath, nativeDir, overwrite = true)
  }
}
