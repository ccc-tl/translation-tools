package tl.util

open class Log {
  private var inProgressSection = false

  open fun startProgress() {
    if (inProgressSection) {
      fatal("Already in progress section")
    }
    inProgressSection = true
  }

  open fun endProgress() {
    if (!inProgressSection) {
      fatal("Not in a progress section")
    }
    inProgressSection = false
  }

  open fun progress(step: Int, maxStep: Int, msg: String) {
    if (!inProgressSection) {
      fatal("Not in a progress section")
    }
    println("${step + 1}/$maxStep $msg")
  }

  open fun info(msg: String) {
    println(msg)
  }

  open fun warn(msg: String) {
    println("WARN: $msg")
  }

  open fun fatal(msg: String): Nothing {
    error(msg)
  }
}
