package tl

import kio.util.child
import java.io.File

private const val projectBaseVarName = "FE_PROJECT_BASE"
val projectBase by lazy {
  getBaseDirectory(projectBaseVarName)
}

private const val pspSdkHomeVarName = "PSPSDK_HOME"
val pspSdkHome by lazy {
  getBaseDirectory(pspSdkHomeVarName).child("bin")
}

private const val ppssppHomeVarName = "PPSSPP_HOME"
val ppssppHome by lazy {
  getBaseDirectory(ppssppHomeVarName)
}

private fun getBaseDirectory(varName: String): File {
  val path = System.getenv(varName)
    ?: error("$varName environment variable not defined")
  val file = File(path)
  if (!file.exists()) {
    error("$varName points to non existing directory")
  }
  return file
}
