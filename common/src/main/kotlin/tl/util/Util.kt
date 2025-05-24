package tl.util

import kio.KioInputStream
import kio.KioOutputStream
import kio.LERandomAccessFile
import kio.util.WINDOWS_932
import kio.util.appendLine
import kio.util.execute
import org.apache.commons.exec.PumpStreamHandler
import java.io.File
import java.nio.charset.Charset
import java.util.Locale

fun StringBuilder.appendWindowsLine(text: String = "") {
  appendLine(text, "\r\n")
}

fun KioInputStream.readDatString(
  at: Int = pos(),
  maintainStreamPos: Boolean = false,
  fixUpNewLine: Boolean = false,
  charset: Charset = Charsets.WINDOWS_932,
): String {
  val prevPos = longPos()
  setPos(at)
  var charCount = 0
  while (true) {
    charCount++
    val character = readByte().toInt().toChar()
    if (character.code == 0x00 && pos().rem(4) == 0) {
      break
    }
  }
  setPos(at)
  val bytes = ByteArray(charCount)
  var idx = 0
  while (true) {
    val character = readByte()
    if (character != 0x00.toByte()) {
      bytes[idx++] = character
    } else if (pos().rem(4) == 0) {
      break
    }
  }
  if (maintainStreamPos) {
    setPos(prevPos)
  }
  val result = String(bytes, charset).replace("\u0000", "")
  if (fixUpNewLine) {
    return result.replace("\n\r", "\r\n")
  }
  return result
}

fun KioOutputStream.writeDatString(string: String, charset: Charset = Charsets.WINDOWS_932): Int {
  val bytes = string.toByteArray(charset)
  writeBytes(bytes)
  val padding = 4 - (bytes.size % 4)
  writeBytes(ByteArray(padding))
  return bytes.size + padding
}

fun LERandomAccessFile.writeDatString(string: String, charset: Charset = Charsets.WINDOWS_932): Int {
  val bytes = string.toByteArray(charset)
  write(bytes)
  val padding = 4 - (bytes.size % 4)
  write(ByteArray(padding))
  return bytes.size + padding
}

fun createXdeltaPatch(
  xdeltaTool: File,
  baseDir: File,
  oldFile: File,
  newFile: File,
  patchOut: File,
  bufSize: Long = 1812725760,
  streamHandler: PumpStreamHandler? = null,
) {
  execute(
    xdeltaTool,
    workingDirectory = baseDir,
    streamHandler = streamHandler,
    args = listOf("-9", "-S", "djw", "-B", bufSize, "-e", "-A", "-vfs", oldFile, newFile, patchOut),
  )
}

fun createFinePatch(
  fineTool: File,
  oldFile: File,
  newFile: File,
  patchOut: File,
  streamHandler: PumpStreamHandler? = null,
) {
  execute(
    fineTool,
    streamHandler = streamHandler,
    args = listOf("-encode", oldFile, newFile, patchOut),
  )
}

fun alignValue(value: Int, pad: Int = 16): Int {
  if (value % pad == 0) {
    return value
  }
  return (value / pad + 1) * pad
}

fun copyFromJar(owner: Any, sourcePath: String, targetFile: File, overwrite: Boolean = false) {
  val url = owner.javaClass.getResource(sourcePath) ?: error("Failed to get resource URL")
  File(url.path).copyTo(targetFile, overwrite = overwrite)
}

fun copyFromJarRecursively(owner: Any, sourcePath: String, targetDir: File, overwrite: Boolean = false) {
  targetDir.mkdir()
  val url = owner.javaClass.getResource(sourcePath) ?: error("Failed to get resource URL")
  File(url.path).copyRecursively(targetDir, overwrite = overwrite)
}

fun isPlatformValidFileName(name: String): Boolean {
  try {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    if (isWindows && (name.contains(">") || name.contains("<"))) {
      return false
    }
    return File(name.lowercase()).canonicalFile.name == name.lowercase()
  } catch (e: Exception) {
    return false
  }
}

fun findClosetsDivisibleBy(value: Int, divideBy: Int): Int {
  var newValue = value
  while (true) {
    if (newValue % divideBy == 0) return newValue
    newValue++
  }
}

fun mapRange(input: Double, inputStart: Double, inputEnd: Double, outputStart: Double, outputEnd: Double): Double {
  val slope = 1.0 * (outputEnd - outputStart) / (inputEnd - inputStart)
  return outputStart + slope * (input - inputStart)
}

fun percentString(count: Int, total: Int): String {
  return "%.02f".format(Locale.US, percentValue(count, total)) + "%"
}

fun percentValue(count: Int, total: Int): Double {
  return count * 100.0 / total
}
