package tl

import kio.util.child
import java.io.File

private const val PROJECT_BASE_ENV = "FE_PROJECT_BASE"
val projectBase by lazy {
  getBaseDirectory(PROJECT_BASE_ENV)
}

private const val PSPSDK_HOME_ENV = "PSPSDK_HOME"
val pspSdkHome by lazy {
  getBaseDirectory(PSPSDK_HOME_ENV).child("bin")
}

private const val PPSSPP_HOME_ENV = "PPSSPP_HOME"
val ppssppHome by lazy {
  getBaseDirectory(PPSSPP_HOME_ENV)
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
