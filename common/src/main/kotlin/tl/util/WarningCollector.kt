package tl.util

class WarningCollector(
  private val logWarningsImmediately: Boolean = true
) {
  private val warnings = mutableListOf<String>()

  fun printSummary() {
    if (warnings.size > 0) {
      println("${warnings.size} ${if (warnings.size == 1) "warning" else "warnings"}")
      warnings.forEach {
        println("WARN: $it")
      }
    } else {
      println("No warnings!")
    }
  }

  fun warn(msg: String) {
    if (logWarningsImmediately) {
      println("WARN: $msg")
    }
    warnings.add(msg)
  }
}
