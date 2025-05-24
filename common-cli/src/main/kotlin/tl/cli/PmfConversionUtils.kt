package tl.cli

import kio.util.child
import kio.util.execute
import kio.util.nullStreamHandler
import java.io.File

fun main(args: Array<String>) {
  if (args.size != 3) {
    println("Usage: [rawIn] [burnedOut] [segmentTime]")
    return
  }
  val rawIn = File(args[0])
  val burnedOut = File(args[1])
  val segmentTime = args[2] // for example: 00:01:20

  // PMF Conversion Process
  // 1. Use VGMToolbox to split .PMF into .OMA and .264 files
  // 2. Convert OMA and burn subs to AVI:
  burnSubsToAvi(rawIn, burnedOut, segmentTime)
  println("Done")
  // 3. Use Umd Stream Composer to create MPS files from AVI and WAV
  // Make sure to set input levels and output level as full
  // 4. Use PMF Creator to make new PMF
}

private fun burnSubsToAvi(rawIn: File, burnedOut: File, segmentTime: String, useSubs: Boolean = true, vflip: Boolean = true) {
  burnedOut.mkdir()
  println("Converting OMA to WAV")
  val inFiles = rawIn.listFiles() ?: emptyArray()

  inFiles
    .filter { it.extension.equals("oma", ignoreCase = true) }
    .forEach { audioIn ->
      val wavOut = burnedOut.child(audioIn.nameWithoutExtension + ".wav")
      if (wavOut.exists()) {
        println("${wavOut.name} already exists, skipping.")
        return@forEach
      }
      println("Converting ${wavOut.name}...")
      execute("ffmpeg", listOf("-i", audioIn, wavOut), workingDirectory = burnedOut, streamHandler = nullStreamHandler())
    }

  println("Burning subs and converting to uncompressed AVI")
  inFiles
    .filter { it.extension.equals("264", ignoreCase = true) }
    .forEach { videoIn ->
      val aviOut = burnedOut.child(videoIn.nameWithoutExtension + "-%03d.avi")
      val subsIn = inFiles.firstOrNull { it.nameWithoutExtension == videoIn.nameWithoutExtension.substringBeforeLast("_") }

      if (subsIn == null) {
        println("WARN: Subtitles for ${aviOut.name} are missing! Skipping.")
        return@forEach
      }
      val subsOut = burnedOut.child("${aviOut.nameWithoutExtension}.${subsIn.extension}")
      subsIn.copyTo(subsOut, overwrite = true)
      println("Burning ${aviOut.name}...")
      val videoFilters = listOfNotNull(
        if (useSubs) "ass=${subsOut.name}" else null,
        if (vflip) "vflip" else null,
      )
        .joinToString(separator = ",")
      execute(
        "ffmpeg",
        listOf(
          "-i", videoIn,
          "-y",
          "-vf", videoFilters,
          "-vcodec", "rawvideo",
          "-segment_time", segmentTime,
          "-reset_timestamps", "1",
          "-f", "segment",
          "-pix_fmt", "bgr24",
          aviOut,
        ),
        workingDirectory = burnedOut,
        streamHandler = nullStreamHandler(),
      )
      subsOut.delete()
    }
}
