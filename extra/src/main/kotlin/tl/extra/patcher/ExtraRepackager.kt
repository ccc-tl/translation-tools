package tl.extra.patcher

import kio.util.child
import kio.util.readJson
import kio.util.toWHex
import kmipsx.elf.patchElf
import tl.extra.extraCpkUnpack
import tl.extra.extraToolkit
import tl.extra.extraUnpack
import tl.extra.file.PakFile
import tl.extra.patcher.eboot.ExtraAsmPatcher
import tl.extra.patcher.translation.DatTranslationProcessor
import tl.extra.patcher.translation.ExtraTranslation
import tl.extra.patcher.translation.MiscTranslationProcessor
import tl.extra.util.PakReferenceMap
import tl.extra.util.TextMeasure
import tl.file.CpkPatchedFile
import tl.pspSdkHome
import tl.util.ScriptWriteMode
import tl.util.WarningCollector
import tl.util.startPpssppWithIso
import java.io.File

fun main() {
  val buildResult = repackageExtra(publicBuild = false, downloadTranslations = true)
  startPpssppWithIso(buildResult.isoBuildDir)
}

fun repackageExtra(
  publicBuild: Boolean = true,
  downloadTranslations: Boolean = publicBuild,
  warningCollector: WarningCollector = WarningCollector(),
): PatchBuildResult {
  return ExtraRepackager(extraToolkit, extraCpkUnpack, extraUnpack, pspSdkHome, publicBuild, downloadTranslations, warningCollector)
    .buildAll()
}

internal class ExtraRepackager(
  private val projectDir: File,
  private val extraUsCpkExtract: File,
  private val extraUsPakExtract: File,
  pspSdkDir: File,
  private val publicBuild: Boolean,
  private val downloadTranslations: Boolean,
  private val warningCollector: WarningCollector,
) {
  private val srcDir = projectDir.child("src")
  private val imDir = projectDir.child("im")
  private val buildDir = projectDir.child("build")
  private val toolsDir = projectDir.child("tools")

  private val srcCpkFile = srcDir.child("data.cpk")
  private val srcDecryptedEboot = srcDir.child("ULUS10576.BIN")
  private val customPakFilesDir = srcDir.child("custom-pak-files")
  private val customCpkFilesDir = srcDir.child("custom-cpk-files")
  private val isoBaseSrcDir = srcDir.child("iso-base")
  private val stockSrcDir = srcDir.child("stock")
  private val translationDir = srcDir.child("translation")

  private val decmpCacheDir = imDir.child("decmp-cache")
  private val pakImDir = imDir.child("pak")
  private val nativeDir = imDir.child("native")
  private val nameReplacementDir = imDir.child("name-replacement")

  private val isoBuildDir = buildDir.child("iso")
  private val outEbootFile = isoBuildDir.child("PSP_GAME/SYSDIR/EBOOT.BIN")
  private val outCpkFile = isoBuildDir.child("PSP_GAME/USRDIR/data.cpk")

  private val patchFsOut = buildDir.child("extra.patchfs")
  private val pmfPatchFsOut = buildDir.child("extra-pmf.patchfs")

  private val fineTool = toolsDir.child("fine")

  private val pakRefs by lazy {
    val refsFile = srcDir.child("pak-files.json")
    if (!refsFile.exists()) {
      error("${refsFile.name} is missing")
    }
    refsFile.readJson<PakReferenceMap>()
  }

  private val extraAsmPatcher = ExtraAsmPatcher(nativeDir, pspSdkDir)

  private val remoteTranslationsConfig = mapOf(
    "extra-dat" to mapOf(ScriptWriteMode.COMPAT_TRANSLATED to translationDir.child("dat")),
    "extra-misc" to mapOf(ScriptWriteMode.COMPAT_TRANSLATED to translationDir.child("misc")),
    "extra-subs" to mapOf(ScriptWriteMode.COMPAT_TRANSLATED to translationDir.child("subs")),
    "extra-subs-se" to mapOf(ScriptWriteMode.COMPAT_TRANSLATED to translationDir.child("subs-se"))
  )

  fun buildAll(): PatchBuildResult {
    preBuildChecks()
    downloadRemoteTranslations(downloadTranslations, remoteTranslationsConfig, warningCollector::warn)
    copyIsoFiles()
    processTranslationUnits()
    val pakReplacements = collectPakReplacementFiles()
    assemblePatches(pakReplacements)
    buildCustomPaks(pakReplacements)
    val cpkReplacements = collectCpkReplacementFiles()
    val cpkPatchedFiles = patchCpk(cpkReplacements)
    patchEboot()
    val nameReplacements = createNameReplacementMap()
    createCheatCodeDefinitions()
    createPatch(cpkReplacements, cpkPatchedFiles, nameReplacements)
    postBuildChecks()
    return PatchBuildResult(isoBuildDir, listOf(patchFsOut, pmfPatchFsOut))
  }

  private fun preBuildChecks() {
    println("----- Fate/Extra Internal Patcher -----")
    println("Project directory: ${projectDir.absolutePath}")
    if (!projectDir.exists()) {
      error("Project dir does not exist")
    }
    if (!publicBuild) {
      warningCollector.warn("Creating debug build")
    }
  }

  private fun copyIsoFiles() {
    println("--- Copy ISO files ---")
    isoBuildDir.mkdirs()
    println("Copying base files...")
    isoBaseSrcDir.copyRecursively(isoBuildDir, overwrite = true)
    if (outCpkFile.exists()) {
      println("CPK already exists")
    } else {
      println("Copying CPK...")
      srcCpkFile.copyTo(outCpkFile)
    }
  }

  private fun processTranslationUnits() {
    println("--- Process translation units ---")
    DatTranslationProcessor(extraUsPakExtract, translationDir.child("dat"), imDir.child("dat"), warningCollector::warn)
    MiscTranslationProcessor(extraUsPakExtract, srcDir, translationDir.child("misc"), imDir.child("misc"))
    val subs = ExtraTranslation(
      translationDir.child("subs/script-japanese-orig.txt"),
      checkForAsciiOnly = true
    )
    val subsSe = ExtraTranslation(
      translationDir.child("subs-se/script-japanese.txt"),
      checkForAsciiOnly = true
    )
    val seList = translationDir.child("subs-se/se-list.txt")
      .readLines()
      .filter { it.isNotBlank() }
    SubsTranslationProcessor(
      nativeDir, subs, subsSe, seList,
      null, TextMeasure(8, warningCollector::warn),
      publicBuild, ByteArray(0), warningCollector::warn
    )
  }

  private fun assemblePatches(pakReplacements: Map<String, File>) {
    println("--- Copy native code ---")
    copyNativeCode(this, "FE_CUSTOM_NATIVE_EXTRA_DIR", "/native/extra/", nativeDir, warningCollector::warn)
    println("--- Assemble patches ---")
    extraAsmPatcher.assemble(PakFile(extraUsCpkExtract.child("pack/PRELOAD.pak")), pakReplacements)
  }

  private fun buildCustomPaks(pakReplacements: Map<String, File>) {
    println("--- Build modified PAKs ---")
    pakImDir.mkdir()
    PakUpdater(extraUsCpkExtract, extraAsmPatcher.auxPatchBytes, pakRefs, pakImDir, pakReplacements)
      .buildPaks()
  }

  private fun patchCpk(cpkReplacements: Map<String, File>): List<CpkPatchedFile> {
    println("--- Patch CPK in-place ---")
    return tryPatchCpkInPlace(srcCpkFile, outCpkFile, cpkReplacements, rewriteCmpToPak = true)
  }

  private fun patchEboot() {
    println("--- Patch EBOOT.BIN ---")
    patchElf(srcDecryptedEboot, outEbootFile, extraAsmPatcher.bootPatches, 0, 2)
    outEbootFile.copyTo(outEbootFile.resolveSibling("BOOT.BIN"), overwrite = true)
  }

  private fun createNameReplacementMap(): NameReplacements {
    println("--- Create name replacement map ---")
    if (!publicBuild) {
      warningCollector.warn("Creating name replacement map skipped")
      return emptyMap()
    }
    val replacer = NameReplacer(outCpkFile, fineTool, nameReplacementDir)
    replacer.processReplacementGroup(
      "Maestro",
      listOf(
        NameReplacementPattern("Praetor", "Maestro")
      )
    )
    return replacer.getReplacements()
      .toMutableMap()
      .apply {
        val ebootOffset = 0xa0L
        put(
          "LowResRuby-EBOOT",
          NameReplacement(
            patterns = listOf(
              NameReplacementPatternResults("ruby.txb", listOf(0x0014FCF4 + ebootOffset)),
              NameReplacementPatternResults("ruby.bin", listOf(0x0014FD0C + ebootOffset), clearSize = 9),
              NameReplacementPatternResults("\u0009", listOf(0x000B0FC0 + ebootOffset), clearSize = 1),
              NameReplacementPatternResults("\u0009", listOf(0x000B0FC8 + ebootOffset), clearSize = 1)
            ),
            pakFiles = emptyList()
          )
        )
      }
  }

  private fun createCheatCodeDefinitions() {
    println("--- Create cheat code definition ---")
    val patchConfigByte0 = (0x08949AC7 - 0x8800000).toWHex().substring(1, 8)
    buildDir.child("ULUS10576.ini").writeText(
      """
_C0 Audio debug mode
_L 0x7$patchConfigByte0 0x00000001
_C0 Memory debug mode
_L 0x7$patchConfigByte0 0x00000002
_C0 Hide subtitles
_L 0x7$patchConfigByte0 0x00000004
      """.trimIndent().replace("\n", "\r\n")
    )
  }

  private fun createPatch(cpkReplacements: Map<String, File>, cpkPatchedFiles: List<CpkPatchedFile>, nameReplacements: NameReplacements) {
    if (publicBuild) {
      println("--- Create PatchFS ---")
      val patchFsDir = imDir.child("patchfs").apply { mkdir() }
      PatchFsCreator(
        fineTool, PatchFsCreateMode.EXTRA,
        stockSrcDir.child("ULUS10576.BIN"), stockSrcDir.child("ULUS10576-PSN.BIN"), outEbootFile,
        extraUsCpkExtract, cpkReplacements, cpkPatchedFiles, nameReplacements,
        stockSrcDir
      ).createTo(patchFsDir, decmpCacheDir, patchFsOut, pmfPatchFsOut)
    } else {
      patchFsOut.delete()
      pmfPatchFsOut.delete()
    }
  }

  private fun postBuildChecks() {
    println("\nBuild finished")
    warningCollector.printSummary()
  }

  private fun collectPakReplacementFiles(): Map<String, File> {
    val collector = FileCollector(projectDir, "PAK", warningCollector::warn)
    collector.includeDir(customPakFilesDir)
    collector.includeDir(imDir.child("dat/patched"))
    collector.includeDir(imDir.child("misc/patched"))
    return collector.files
  }

  private fun collectCpkReplacementFiles(): Map<String, File> {
    val collector = FileCollector(projectDir, "CPK", warningCollector::warn)
    collector.includeDir(customCpkFilesDir)
    collector.includeDir(imDir.child("pak/patched"))
    return collector.files
  }
}
