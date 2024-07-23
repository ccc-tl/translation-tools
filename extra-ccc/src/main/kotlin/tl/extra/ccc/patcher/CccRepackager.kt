package tl.extra.ccc.patcher

import kio.KioOutputStream
import kio.LERandomAccessFile
import kio.util.child
import kio.util.readJson
import kio.util.toWHex
import kmipsx.elf.patchElf
import tl.extra.ccc.cccCpkUnpack
import tl.extra.ccc.cccDnsUnpack
import tl.extra.ccc.cccToolkit
import tl.extra.ccc.cccUnpack
import tl.extra.ccc.patcher.eboot.CccAsmPatcher
import tl.extra.ccc.patcher.translation.CccTranslation
import tl.extra.ccc.patcher.translation.DatTranslationProcessor
import tl.extra.ccc.patcher.translation.EbootTranslationProcessor
import tl.extra.ccc.patcher.translation.InDungeonTranslationProcessor
import tl.extra.ccc.patcher.translation.InfomatrixDTranslationProcessor
import tl.extra.ccc.patcher.translation.InfomatrixTTranslationProcessor
import tl.extra.ccc.patcher.translation.ItemsTranslationProcessor
import tl.extra.ccc.patcher.translation.MiscTranslationProcessor
import tl.extra.ccc.patcher.translation.SgTranslationProcessor
import tl.extra.file.PakFile
import tl.extra.patcher.FileCollector
import tl.extra.patcher.NameReplacement
import tl.extra.patcher.NameReplacementPakFile
import tl.extra.patcher.NameReplacementPattern
import tl.extra.patcher.NameReplacementPatternResults
import tl.extra.patcher.NameReplacements
import tl.extra.patcher.NameReplacer
import tl.extra.patcher.PakUpdater
import tl.extra.patcher.PatchBuildResult
import tl.extra.patcher.PatchFsCreateMode
import tl.extra.patcher.PatchFsCreator
import tl.extra.patcher.SubsTranslationProcessor
import tl.extra.patcher.copyNativeCode
import tl.extra.patcher.downloadRemoteTranslations
import tl.extra.patcher.tryPatchCpkInPlace
import tl.extra.util.PakReferenceMap
import tl.extra.util.TextMeasure
import tl.file.CpkPatchedFile
import tl.pspSdkHome
import tl.util.ScriptWriteMode
import tl.util.WarningCollector
import tl.util.startPpssppWithIso
import java.io.ByteArrayOutputStream
import java.io.File

fun main() {
  val buildResult = repackageCcc(publicBuild = false, downloadTranslations = true)
  startPpssppWithIso(buildResult.isoBuildDir)
}

fun repackageCcc(
  publicBuild: Boolean = true,
  downloadTranslations: Boolean = publicBuild,
  warningCollector: WarningCollector = WarningCollector(),
): PatchBuildResult {
  return CccRepackager(cccToolkit, cccDnsUnpack, cccCpkUnpack, cccUnpack, pspSdkHome, publicBuild, downloadTranslations, warningCollector)
    .buildAll()
}

class CccRepackager(
  private val projectDir: File,
  cccDnsExtract: File,
  private val cccCpkExtract: File,
  private val cccPakExtract: File,
  pspSdkDir: File,
  private val publicBuild: Boolean,
  private val downloadTranslations: Boolean,
  private val warningCollector: WarningCollector,
) {
  private val srcDir = projectDir.child("src")
  private val imDir = projectDir.child("im")
  private val buildDir = projectDir.child("build")
  private val toolsDir = projectDir.child("tools")

  private val srcCpkFile = cccDnsExtract.child("GAME.cpk")
  private val srcDecryptedEboot = srcDir.child("NPJH50505.BIN")
  private val customPakFilesDir = srcDir.child("custom-pak-files")
  private val customCpkFilesDir = srcDir.child("custom-cpk-files")
  private val customIsoFilesDir = srcDir.child("custom-iso-files")
  private val isoBaseSrcDir = srcDir.child("iso-base")
  private val stockSrcDir = srcDir.child("stock")
  private val png2TexDir = srcDir.child("png2tex")
  private val translationDir = srcDir.child("translation")

  private val translationDatDir = translationDir.child("dat")
  private val translationEbootDir = translationDir.child("eboot")
  private val translationIndungeonDir = translationDir.child("indungeon")
  private val translationInfomatrixDir = translationDir.child("infomatrix")
  private val translationItemsDir = translationDir.child("items")
  private val translationMiscDir = translationDir.child("misc")
  private val translationSgDir = translationDir.child("sg")
  private val translationSubsDir = translationDir.child("subs")
  private val translationSubsSeDir = translationDir.child("subs-se")

  private val decmpCacheDir = imDir.child("decmp-cache")
  private val translationImDir = imDir.child("translation")
  private val poemImDir = imDir.child("poem")
  private val patchedInterfaceImDir = imDir.child("patched-interface")
  private val pakImDir = imDir.child("pak")
  private val nativeDir = imDir.child("native")
  private val nameReplacementDir = imDir.child("name-replacement")

  private val isoBuildDir = buildDir.child("iso")
  private val outEbootFile = isoBuildDir.child("PSP_GAME/SYSDIR/EBOOT.BIN")
  private val outCpkFile = isoBuildDir.child("PSP_GAME/INSDIR/GAME.DNS")

  private val patchFsOut = buildDir.child("ccc.patchfs")
  private val pmfPatchFsOut = buildDir.child("ccc-pmf.patchfs")

  private val fineTool = toolsDir.child("fine")

  private val remoteTranslationsConfig = mapOf(
    "ccc-dat" to
      mapOf(
        ScriptWriteMode.COMPAT_TRANSLATED to translationDatDir,
        ScriptWriteMode.JSON to translationDatDir,
      ),
    "ccc-eboot" to
      mapOf(
        ScriptWriteMode.COMPAT_TRANSLATED to translationEbootDir,
        ScriptWriteMode.JSON to translationEbootDir,
      ),
    "ccc-indungeon" to
      mapOf(
        ScriptWriteMode.COMPAT_TRANSLATED to translationIndungeonDir,
        ScriptWriteMode.JSON to translationIndungeonDir,
      ),
    "ccc-infomatrix" to
      mapOf(
        ScriptWriteMode.COMPAT_TRANSLATED to translationInfomatrixDir,
        ScriptWriteMode.JSON to translationInfomatrixDir,
      ),
    "ccc-items" to
      mapOf(
        ScriptWriteMode.COMPAT_TRANSLATED to translationItemsDir,
        ScriptWriteMode.JSON to translationItemsDir,
      ),
    "ccc-misc" to
      mapOf(
        ScriptWriteMode.COMPAT_TRANSLATED to translationMiscDir,
        ScriptWriteMode.JSON to translationMiscDir,
      ),
    "ccc-sg" to
      mapOf(
        ScriptWriteMode.COMPAT_TRANSLATED to translationSgDir,
        ScriptWriteMode.JSON to translationSgDir,
      ),
    "ccc-subs" to
      mapOf(
        ScriptWriteMode.COMPAT_TRANSLATED to translationSubsDir,
        ScriptWriteMode.JSON to translationSubsDir,
      ),
    "ccc-subs-se" to
      mapOf(
        ScriptWriteMode.COMPAT_TRANSLATED to translationSubsSeDir,
        ScriptWriteMode.JSON to translationSubsSeDir,
      )
  )

  private val pakRefs by lazy {
    val refsFile = srcDir.child("pak-files.json")
    if (!refsFile.exists()) {
      error("${refsFile.name} is missing")
    }
    refsFile.readJson<PakReferenceMap>()
  }

  private val cccPatcher = CccAsmPatcher(publicBuild, nativeDir, pspSdkDir)

  fun buildAll(): PatchBuildResult {
    preBuildChecks()
    downloadRemoteTranslations(downloadTranslations, remoteTranslationsConfig, warningCollector::warn)
    copyIsoFiles()
    copyCustomIsoFiles()
    val translations = processTranslationUnits()
    patchDats()
    patchPoems()
    patchInterface()
    val pakReplacements = collectPakReplacementFiles()
    assemblePatches(translations, pakReplacements)
    buildCustomPaks(pakReplacements)
    val cpkReplacements = collectCpkReplacementFiles()
    val cpkPatchedFiles = patchCpk(cpkReplacements)
    patchEboot(translations)
    val nameReplacements = createNameReplacementMap()
    createCheatCodeDefinitions()
    createPatch(cpkReplacements, cpkPatchedFiles, nameReplacements)
    postBuildChecks()
    return PatchBuildResult(isoBuildDir, listOf(patchFsOut, pmfPatchFsOut))
  }

  private fun preBuildChecks() {
    println("----- Fate/Extra CCC Internal Patcher -----")
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

  private fun copyCustomIsoFiles() {
    println("--- Copy custom ISO files ---")
    customIsoFilesDir.copyRecursively(isoBuildDir, overwrite = true)
  }

  private fun processTranslationUnits(): CompletedTranslationProcessors {
    println("--- Process translations ---")
    if (!translationDir.exists()) {
      error("Translation directory does not exist")
    }

    val textMeasure = TextMeasure(7, warningCollector::warn)
    val remappedTextDataOut = KioOutputStream(ByteArrayOutputStream())
    DatTranslationProcessor(
      cccPakExtract, translationImDir, translationDatDir, imDir.child("dat-cache.json"), remappedTextDataOut,
      textMeasure, warningCollector::warn
    )
    val eboot = EbootTranslationProcessor(translationEbootDir, warningCollector::warn)
    val infomatrixTranslationDir = translationInfomatrixDir
    InfomatrixDTranslationProcessor(cccPakExtract, translationImDir, infomatrixTranslationDir, textMeasure, warningCollector::warn)
    InfomatrixTTranslationProcessor(cccPakExtract, translationImDir, infomatrixTranslationDir)
    InDungeonTranslationProcessor(cccPakExtract, translationImDir, translationIndungeonDir, textMeasure, warningCollector::warn)
    val items = ItemsTranslationProcessor(cccPakExtract, translationImDir, translationItemsDir)
    MiscTranslationProcessor(cccPakExtract, translationImDir, translationMiscDir, textMeasure, warningCollector::warn)
    SgTranslationProcessor(cccPakExtract, translationImDir, translationSgDir, textMeasure, warningCollector::warn)
    val subs = CccTranslation(
      translationSubsDir.child("script-japanese.txt"),
      stripNewLine = false, replaceLiteralNewLine = false
    )
    val subsSe = CccTranslation(
      translationSubsSeDir.child("script-japanese.txt"),
      stripNewLine = false, replaceLiteralNewLine = false
    )
    val seList = translationSubsSeDir.child("se-list.txt")
      .readLines()
      .filter { it.isNotBlank() }
    SubsTranslationProcessor(
      nativeDir, subs, subsSe, seList,
      translationSubsSeDir.child("idMap.json").readJson(), textMeasure,
      publicBuild, remappedTextDataOut.getAsByteArrayOutputStream().toByteArray(), warningCollector::warn
    )
    return CompletedTranslationProcessors(items, eboot)
  }

  private fun patchDats() {
    println("--- Process DATs ---")
    with(LERandomAccessFile(translationImDir.child("field_new/100/0000.dat"))) {
      // fix choices for 21047
      seek(0x9E74C)
      writeInt(3)
      seek(0x9E75C)
      writeInt(3)
      close()
    }
  }

  private fun patchPoems() {
    println("--- Process poems ---")
    getPoemUpdates().forEach { (name, updates) ->
      patchPoem(name, updates)
    }
  }

  private fun patchPoem(name: String, updates: List<PoemUpdate>) {
    println("Patching poem $name...")
    PoemPatcher(
      cccPakExtract.child("interface/poem/$name.dat"),
      cccPakExtract.child("interface/poem/$name.mob"),
      updates
    ).patchTo(poemImDir.child("interface/poem"))
  }

  private fun patchInterface() {
    println("--- Process interface patches ---")
    println("Patching dialog...")
    DialogPatcher(cccPakExtract.child("interface/dialog/0000.mob"))
      .patchTo(patchedInterfaceImDir.child("interface/dialog"))
    println("Patching dungeon...")
    DungeonPatcher(
      cccPakExtract.child("interface/dungeon/i_dun.mob"),
      cccPakExtract.child("interface/dungeon/i_dun_mass.dat"),
    )
      .patchTo(patchedInterfaceImDir.child("interface/dungeon"))
  }

  private fun assemblePatches(translations: CompletedTranslationProcessors, pakReplacements: Map<String, File>) {
    println("--- Copy native code ---")
    copyNativeCode(this, "FE_CUSTOM_NATIVE_EXTRA_DIR", "/native/extra/", nativeDir, warningCollector::warn)
    copyNativeCode(this, "FE_CUSTOM_NATIVE_CCC_DIR", "/native/ccc/", nativeDir, warningCollector::warn)
    println("--- Assemble patches ---")
    cccPatcher.assemble(
      PakFile(cccCpkExtract.child("pack/PRELOAD.pak")), pakReplacements,
      translations.items.remapper01, translations.items.remapper04,
      srcDir.child("credits.txt").takeIf { it.exists() }?.readText() ?: ""
    )
  }

  private fun buildCustomPaks(pakReplacements: Map<String, File>) {
    println("--- Build modified PAKs ---")
    pakImDir.mkdir()
    PakUpdater(cccCpkExtract, cccPatcher.auxPatchBytes, pakRefs, pakImDir, pakReplacements).buildPaks()
  }

  private fun patchCpk(cpkReplacements: Map<String, File>): List<CpkPatchedFile> {
    println("--- Patch CPK in-place ---")
    return tryPatchCpkInPlace(srcCpkFile, outCpkFile, cpkReplacements, false)
  }

  private fun patchEboot(translations: CompletedTranslationProcessors) {
    println("--- Patch EBOOT.BIN ---")
    patchElf(srcDecryptedEboot, outEbootFile, cccPatcher.bootPatches, 0, 2)
    translations.eboot.patchEboot(outEbootFile)
    patchElf(outEbootFile, outEbootFile, translations.eboot.translationEbootPatches, 0, 2)
  }

  private fun createNameReplacementMap(): NameReplacements {
    println("--- Create name replacement map ---")
    if (!publicBuild) {
      warningCollector.warn("Creating name replacement map skipped")
      return emptyMap()
    }
    val replacer = NameReplacer(outCpkFile, fineTool, nameReplacementDir)
    replacer.processReplacementGroup(
      "Meltryllis",
      listOf(
        NameReplacementPattern("Meltlilith", "Meltryllis")
      ),
      listOf(
        NameReplacementPakFile("pack/osioki_mll.pak", "interface/sg/sg_mll.txb", png2TexDir.child("sg_alt/sg_mll.txb")),
        NameReplacementPakFile("pack/sg_chr_mll.pak", "interface/sg/sg_mll.txb", png2TexDir.child("sg_alt/sg_mll.txb")),
        NameReplacementPakFile("pack/field_map_032.pak", "interface/dayresult/day_result00.txb", png2TexDir.child("dayresult_alt/day_result00.txb")),
        NameReplacementPakFile("pack/field_map_033.pak", "interface/dayresult/day_result00.txb", png2TexDir.child("dayresult_alt/day_result00.txb")),
        NameReplacementPakFile("pack/field_map_071.pak", "interface/dayresult/day_result00.txb", png2TexDir.child("dayresult_alt/day_result00.txb")),
        NameReplacementPakFile("pack/0000/btl_vs.pak", "battle/interface/vs/btl_vs.txb", png2TexDir.child("btl_vs_alt/btl_vs.txb")),
        NameReplacementPakFile("pack/PRELOAD.pak", "interface/cmn/i_con_name.txb", png2TexDir.child("i_con_name_alt/i_con_name.txb"))
      )
    )
    replacer.processReplacementGroup(
      "Maestro",
      listOf(
        NameReplacementPattern("Praetor", "Maestro"),
        NameReplacementPattern("eyes, Prae", "eyes, Maes", descriptorFilter = { it.relPath == "pack/field_map_002.pak" })
      )
    )
    return replacer.getReplacements()
      .toMutableMap()
      .apply {
        put(
          "Meltryllis-EBOOT",
          NameReplacement(
            patterns = listOf(NameReplacementPatternResults("Meltryllis", listOf(0x21B4D4))),
            pakFiles = emptyList()
          )
        )
        val ebootOffset = 0xa0L
        put(
          "LowResRuby-EBOOT",
          NameReplacement(
            patterns = listOf(
              NameReplacementPatternResults("ruby.txb", listOf(0x001BA14C + ebootOffset)),
              NameReplacementPatternResults("ruby.bin", listOf(0x001BA164 + ebootOffset), clearSize = 9),
              NameReplacementPatternResults("\u0009", listOf(0x000E339C + ebootOffset), clearSize = 1),
              NameReplacementPatternResults("\u0009", listOf(0x000E33A4 + ebootOffset), clearSize = 1)
            ),
            pakFiles = emptyList()
          )
        )
      }
  }

  private fun createCheatCodeDefinitions() {
    println("--- Create cheat code definition ---")
    val patchConfigByte0 = (0x08A1F34D - 0x8800000).toWHex().substring(1, 8)
    buildDir.child("NPJH50505.ini").writeText(
      """
_C0 Audio debug mode
_L 0x7$patchConfigByte0 0x00000001
_C0 Memory debug mode
_L 0x7$patchConfigByte0 0x00000002
_C0 Hide subtitles
_L 0x7$patchConfigByte0 0x00000004
_C0 Use original BB Channel drop shadow
_L 0x7$patchConfigByte0 0x00000008
      """.trimIndent().replace("\n", "\r\n")
    )
  }

  private fun createPatch(cpkReplacements: Map<String, File>, cpkPatchedFiles: List<CpkPatchedFile>, nameReplacements: NameReplacements) {
    if (publicBuild) {
      println("--- Create PatchFS ---")
      val patchFsDir = imDir.child("patchfs").apply { mkdir() }
      PatchFsCreator(
        fineTool, PatchFsCreateMode.CCC,
        stockSrcDir.child("NPJH50505.BIN"), stockSrcDir.child("NPJH50505-PSN.BIN"), outEbootFile,
        cccCpkExtract, cpkReplacements, cpkPatchedFiles, nameReplacements,
        stockSrcDir, isoBuildDir,
        warningCollector::warn
      ).createTo(patchFsDir, decmpCacheDir, patchFsOut, pmfPatchFsOut)
    } else {
      patchFsOut.delete()
      pmfPatchFsOut.delete()
    }
  }

  private fun postBuildChecks() {
    println("--- Post build checks ---")
    println("\nBuild finished")
    warningCollector.printSummary()
  }

  private fun collectPakReplacementFiles(): Map<String, File> {
    val collector = FileCollector(projectDir, "PAK", warningCollector::warn)
    collector.includeDir(customPakFilesDir)
    collector.includeDir(translationImDir)
    collector.includeDir(poemImDir)
    collector.includeDir(patchedInterfaceImDir)
    return collector.files
  }

  private fun collectCpkReplacementFiles(): Map<String, File> {
    val collector = FileCollector(projectDir, "CPK", warningCollector::warn)
    collector.includeDir(customCpkFilesDir)
    collector.includeDir(pakImDir.child("patched"))
    return collector.files
  }

  private class CompletedTranslationProcessors(
    val items: ItemsTranslationProcessor,
    val eboot: EbootTranslationProcessor,
  )
}
