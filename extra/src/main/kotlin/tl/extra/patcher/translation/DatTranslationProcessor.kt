package tl.extra.patcher.translation

import com.google.common.hash.Hashing
import kio.util.child
import kio.util.readJson
import kio.util.writeJson
import tl.extra.patcher.file.CombinedDatEntry
import tl.extra.patcher.file.ExtraDatFilePatcher
import java.io.File

internal class DatTranslationProcessor(
  private val pakExtract: File,
  private val unitDir: File,
  outDir: File,
  private val warn: (String) -> Unit,
) {
  private val patchedOut = outDir.child("patched")
  private val translationFile = unitDir.child("script-translation.txt")
  private val translationHashFile = outDir.child("hash.json")
  private val translation = ExtraTranslation(unitDir.child("script-japanese.txt"))
  private val translationOrig = ExtraTranslation(
    unitDir.child("script-japanese.txt"),
    unitDir.child("script-translation-orig.txt"),
  )

  init {
    if (isPatchingNeeded()) {
      patchFiles()
    } else {
      println("DAT patching not needed")
    }
  }

  private fun patchFiles() {
    checkDuplicates()
    patchedOut.mkdir()
    val entriesMain: List<CombinedDatEntry> = unitDir.child("entriesMain.json").readJson()
    val entriesMisc: List<CombinedDatEntry> = unitDir.child("entriesMisc.json").readJson()
    val entries = mutableListOf<CombinedDatEntry>()
    entries.addAll(entriesMain)
    entries.addAll(entriesMisc)
    entries
      .groupBy { it.relPath }
      .forEach { (relPath, fileEntries) ->
        patchFile(relPath, fileEntries)
      }
    saveHash()
  }

  private fun isPatchingNeeded(): Boolean {
    if (!translationHashFile.exists()) {
      return true
    }
    val prevHash = translationHashFile.readJson<ByteArray>()
    val currentHash = getHashOfCurrent()
    return !prevHash.contentEquals(currentHash)
  }

  private fun saveHash() {
    translationHashFile.writeJson(getHashOfCurrent())
  }

  private fun getHashOfCurrent(): ByteArray {
    return Hashing.sha256().hashBytes(translationFile.readBytes()).asBytes()
  }

  private fun checkDuplicates() {
    translationOrig.enTexts
      .mapIndexed { idx, text -> Pair(idx, text) }
      .groupBy { it.second }
      .filter { it.value.size > 1 }
      .forEach { origEntry ->
        val translatedSet = HashSet<Pair<String, String>>()
        origEntry.value.forEach { set ->
          val newText = translation.enTexts[set.first]
          translatedSet.add(Pair(set.second, newText))
        }
        if (translatedSet.size > 1) {
          warn("DAT English duplicate: ${translatedSet.first().first}")
        }
      }
  }

  private fun patchFile(path: String, entries: List<CombinedDatEntry>) {
    println("Patching $path...")
    val srcFile = pakExtract.child(path)
    val outFile = patchedOut.child(path)
    outFile.parentFile.mkdirs()
    ExtraDatFilePatcher(
      srcFile.readBytes(),
      outFile,
      translation,
      translationOrig,
      entries,
    )
  }
}
