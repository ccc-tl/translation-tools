package tl.extra.util

import kio.KioInputStream
import java.io.File

// parameters probably for Extra US
fun parseExtraSoundTable(extraEbootFile: File) = parseSoundTable(extraEbootFile, 0xA0, 0x00160314, 3422)

// parameters for Extra CCC JP
fun parseCccSoundTable(cccEbootFile: File) = parseSoundTable(cccEbootFile, 0xA0, 0x001FE7DC, 13046)

fun parseSoundTable(
  ebootFile: File,
  ebootBaseOffset: Int,
  soundTableAddr: Int,
  soundTableEntryCount: Int,
): List<String> {
  val table = mutableListOf<String>()
  with(KioInputStream(ebootFile)) {
    setPos(ebootBaseOffset + soundTableAddr)
    repeat(soundTableEntryCount) {
      val ptr = readInt()
      readInt()
      temporaryJump(ptr + ebootBaseOffset) {
        table.add(readNullTerminatedString(Charsets.US_ASCII))
      }
    }
  }
  return table
}
