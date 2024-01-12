package tl.extra.ccc.cli

import kio.util.child
import tl.extra.ccc.cccToolkit
import java.io.File
import java.text.SimpleDateFormat
import java.util.TimeZone
import kotlin.math.roundToInt

fun main() {
  EndingLyricsEncoder(
    cccToolkit.child("src/ending-lyrics.srt")
  ).encode()
  println("Done")
}

class EndingLyricsEncoder(private val lyricsFile: File) {
  companion object {
    const val FRAME_DURATION_MILLIS = 1000f / 30
    var timeFormat = SimpleDateFormat("HH:mm:ss,SSS")
      .also { it.timeZone = TimeZone.getTimeZone("UTC") }
  }

  fun encode() {
    val subtitles = lyricsFile.readLines()
      .chunked(4) {
        require(it.size == 3 || it.last().isBlank()) { "Last line not blank, probably multi line subtitle which is not supported for ED: $it" }
        val timeParts = it[1].split(" --> ")
        LyricsLine(
          (timeFormat.parse(timeParts[0]).time / FRAME_DURATION_MILLIS).roundToInt(),
          (timeFormat.parse(timeParts[1]).time / FRAME_DURATION_MILLIS).roundToInt(),
          it[2]
        )
      }
      .sortedBy { it.startFrames }

    val timings = mutableListOf<Int>()
    val lines = mutableListOf<String>()

    var currentFrames = 0
    subtitles.forEach {
      if (currentFrames < it.startFrames) {
        lines.add("")
        timings.add(it.startFrames - currentFrames)
        currentFrames = it.startFrames
      } else if (currentFrames > it.startFrames) {
        error("Overlapping not supported")
      }
      lines.add(it.textLine)
      timings.add(it.endFrames - it.startFrames)
      currentFrames = it.endFrames
    }

    timings.forEach {
      println(it)
    }
    println("---")
    lines.forEach {
      println(it)
    }
  }

  private data class LyricsLine(
    val startFrames: Int,
    val endFrames: Int,
    val textLine: String
  )
}
