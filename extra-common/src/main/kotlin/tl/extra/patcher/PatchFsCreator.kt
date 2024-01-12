package tl.extra.patcher

import kio.util.child
import kio.util.createStdGson
import kio.util.nullStreamHandler
import kio.util.writeJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import tl.extra.file.CmpFile
import tl.file.CpkPatchedFile
import tl.file.PatchFsWriter
import tl.util.createFinePatch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PatchFsCreator(
  private val fineTool: File,
  private val mode: PatchFsCreateMode,
  private val origEboot: File,
  private val origPsnEboot: File,
  private val patchedEboot: File,
  private val cpkExtract: File,
  private val cpkReplacements: Map<String, File>,
  private val cpkPatchedFiles: List<CpkPatchedFile>,
  private val nameReplacements: NameReplacements?,
  private val stockSrcDir: File,
  private val cccIsoBuildDir: File? = null,
) {
  companion object {
    const val MODULE_VERSION = "fate-v3"
  }

  fun createTo(patchFsDir: File, decmpCacheDir: File, patchFsOut: File, pmfPatchFsOut: File) {
    val patchesDir = patchFsDir.child("patches").apply { mkdir() }
    decmpCacheDir.mkdir()
    val patchFsFiles = mutableMapOf<String, File>()
    val pmfPatchFsFiles = mutableMapOf<String, File>()

    patchFsFiles["patch://identity.meta"] = patchFsDir.child("identity.meta").apply {
      writeText(mode.toString().lowercase())
    }
    patchFsFiles["patch://timestamp.meta"] = patchFsDir.child("timestamp.meta").apply {
      writeText(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().withNano(0)))
    }
    patchFsFiles["patch://module-version.meta"] = patchFsDir.child("module-version.meta").apply {
      writeText(MODULE_VERSION)
    }

    println("Creating CPK patch manifest...")
    val cpkManifestFile = patchFsDir.child("cpk-manifest.json")
    cpkManifestFile.writeJson(createStdGson(prettyPrint = false), cpkPatchedFiles)
    patchFsFiles["patch://cpk-manifest.json"] = cpkManifestFile

    if (nameReplacements != null) {
      println("Creating name replacements manifest...")
      val nameReplacementFile = patchFsDir.child("name-replacements.json")
      nameReplacementFile.writeJson(createStdGson(prettyPrint = false), nameReplacements)
      patchFsFiles["patch://name-replacements.json"] = nameReplacementFile
      nameReplacements.forEach { (_, replacement) ->
        replacement.pakFiles.forEach {
          patchFsFiles[it.patchPath] = it.patchFile
        }
      }
    }

    data class PatchTask(val deltaName: String, val fsPath: String, val orig: File, val dest: File)

    listOf(
      PatchTask("eboot.patch", "patch://EBOOT.BIN", origEboot, patchedEboot),
      PatchTask("eboot-psn.patch", "patch://EBOOT-PSN.BIN", origPsnEboot, patchedEboot),
      PatchTask("opnssmp-psn.patch", "patch://OPNSSMP-PSN.BIN", stockSrcDir.child("OPNSSMP-PSN.BIN"), stockSrcDir.child("OPNSSMP.BIN")),
      PatchTask("param-psn.patch", "patch://PARAM-PSN.SFO", stockSrcDir.child("PARAM-PSN.SFO"), stockSrcDir.child("PARAM.SFO")),
    ).forEach {
      println("Creating patch for ${it.dest.name}...")
      val patchFile = patchFsDir.child(it.deltaName)
      createFinePatch(
        fineTool, it.orig, it.dest, patchFile,
        streamHandler = nullStreamHandler()
      )
      patchFsFiles[it.fsPath] = patchFile
    }

    runBlocking {
      cpkReplacements
        .map { (relPath, newFile) ->
          async(Dispatchers.Default) {
            println("Creating patch for $relPath...")
            val srcFile = when (newFile.extension) {
              "cmp" -> {
                val decompFile = decmpCacheDir.child(relPath)
                if (decompFile.exists()) {
                  println("Using cached decompressed CMP $relPath...")
                } else {
                  println("Decompressing CMP $relPath...")
                  decompFile.parentFile.mkdirs()
                  decompFile.writeBytes(CmpFile(cpkExtract.child(relPath)).getData())
                }
                decompFile
              }
              else -> cpkExtract.child(relPath)
            }
            val deltaFile = patchesDir.child(relPath)
            deltaFile.parentFile.mkdirs()
            createFinePatch(fineTool, srcFile, newFile, deltaFile, streamHandler = nullStreamHandler())
            Pair(relPath, deltaFile)
          }
        }
        .map { it.await() }
        .map { (relPath, deltaFile) ->
          if (mode == PatchFsCreateMode.EXTRA && relPath == "movie/FateExtraOP.pmf") {
            pmfPatchFsFiles["cpk://$relPath"] = deltaFile
          } else {
            patchFsFiles["cpk://$relPath"] = deltaFile
          }
        }
    }

    if (mode == PatchFsCreateMode.CCC) {
      fun createPmfPatch(num: String) {
        println("Creating patch for DEMO$num.PMF...")
        val pmfOut = patchFsDir.child("DEMO$num.PMF")
        createFinePatch(
          fineTool,
          stockSrcDir.child("DEMO$num.PMF"),
          cccIsoBuildDir?.child("PSP_GAME/USRDIR/MOVIE/DEMO$num.PMF")
            ?: error("CCC ISO build dir must be set for CCC mode"),
          pmfOut,
          streamHandler = nullStreamHandler()
        )
        pmfPatchFsFiles["patch://DEMO$num.PMF"] = pmfOut
      }
      createPmfPatch("01")
      createPmfPatch("02")
    }

    PatchFsWriter(patchFsFiles, listOf(pmfPatchFsOut.name)).writeTo(patchFsOut)
    PatchFsWriter(pmfPatchFsFiles).writeTo(pmfPatchFsOut)
  }
}

enum class PatchFsCreateMode {
  EXTRA, CCC
}
