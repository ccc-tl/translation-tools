package tl.extra.patcher

import tl.util.ScriptWriteMode
import java.io.File

fun downloadRemoteTranslations(
  downloadTranslations: Boolean,
  config: Map<String, Map<ScriptWriteMode, File>>,
  warn: (String) -> Unit,
) {
  println("--- Download translations ---")
  config.forEach { (workUnit, destinations) ->
    if (destinations.keys.contains(ScriptWriteMode.COMPAT_SUGGESTED)) {
      warn("Using suggestions for translation work unit $workUnit")
    }
  }
  if (!downloadTranslations) {
    warn("Using cached translations")
    return
  }

  // Here you can implement translation download from the remote server
  // The CCC Translation Team's server code is not public so the client is useless and not included here
  warn("Translation download is not implemented")
}
