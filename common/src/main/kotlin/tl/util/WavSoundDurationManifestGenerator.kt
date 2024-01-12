package tl.util

import kio.util.walkDir
import kio.util.writeJson
import java.io.File
import javax.sound.sampled.AudioSystem

fun generateWavSoundDurationManifest(srcDir: File, outFile: File) {
  val durations = mutableMapOf<String, Float>()
  walkDir(srcDir) {
    val ais = AudioSystem.getAudioInputStream(it)
    val format = ais.format
    val duration = it.length() / (format.frameSize * format.frameRate)
    durations[it.name] = duration
  }
  outFile.writeJson(durations)
}
