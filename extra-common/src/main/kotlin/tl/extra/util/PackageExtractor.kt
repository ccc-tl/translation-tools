package tl.extra.util

import kio.util.child
import kio.util.readableFileSize
import kio.util.relativizePath
import kio.util.writeJson
import tl.extra.file.CmpFile
import tl.extra.file.PakFile
import tl.util.Log
import java.io.File

/**
 * Batch file extractor for Extra and CCC .pak/.cmp files. Use this after extracting main CPK.
 */
class PackageExtractor(srcDir: File, outDir: File, log: Log = Log()) {
  init {
    var pakFiles = 0
    var pakFileEntries = 0
    var duplicates = 0
    var duplicatesSize = 0L
    val duplicatesWithContentCollision = mutableListOf<String>()
    val refs = MutablePakReferenceMap()

    println("Scanning for files...")
    val files = srcDir.walk().toList()
      .filter { it.extension == "pak" || it.extension == "cmp" }
    files.forEachIndexed { idx, file ->
      val data = when (file.extension) {
        "cmp" -> CmpFile(file).getData()
        "pak" -> file.readBytes()
        else -> {
          return@forEachIndexed
        }
      }
      log.info("${idx + 1}/${files.size} Process ${file.name}")
      val pak = PakFile(data)
      val pakRelPath = file.relativizePath(srcDir)
      pakFiles++
      pak.entries.forEachIndexed { pakIdx, entry ->
        refs.files.getOrPut(entry.path) { mutableListOf() }
          .apply { add(pakRelPath) }

        var outName = entry.path
        if (entry.path == "") {
          log.warn("Anonymous PAK file entry in PAK ${file.name}, ID $pakIdx")
          outName = "__anonymous from ${file.name} file id $pakIdx"
        }
        var outFile = File(outDir, outName)
        if (outFile.isDirectory) {
          log.warn("PAK entry points to existing directory")
          outName = "__directory from ${file.name} file id $pakIdx"
          outFile = File(outDir, outName)
        }
        if (outFile.exists()) {
          val outFileData = outFile.readBytes()
          if (entry.bytes.contentEquals(outFileData)) {
            // log.warn("Duplicate PAK file entry $outName in PAK ${file.name}")
            duplicates++
            duplicatesSize += entry.bytes.size
          } else {
            duplicatesWithContentCollision.add(outName)
            log.warn("Duplicate PAK file entry $outName with collision name in PAK ${file.name}")
            for (i in 2..Integer.MAX_VALUE) {
              outFile = File(outFile.parentFile.path, "${outFile.nameWithoutExtension} ($i).${outFile.extension}")
              if (!outFile.exists()) {
                break
              }
            }
          }
        } else {
          pakFileEntries++
        }
        entry.writeToFile(outFile)
      }
    }

    outDir.child("pak-files.json")
      .writeJson(refs)
    log.info("Processed files: $pakFiles")
    log.info("Unpacked files: $pakFileEntries")
    log.info("Duplicate files: $duplicates")
    log.info("Duplicate files size: ${readableFileSize(duplicatesSize)}")
    log.info("Duplicate files with content collision: ${duplicatesWithContentCollision.size}")
    if (duplicatesWithContentCollision.size != 0) {
      log.info("Duplicate files with content collision list:")
      log.info(duplicatesWithContentCollision.joinToString(separator = "\n"))
    }
  }
}
