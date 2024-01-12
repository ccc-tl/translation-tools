package tl.extra.ccc.patcher.translation

import kio.KioOutputStream
import kio.util.WINDOWS_932
import kio.util.child
import tl.extra.ccc.file.SjisFilePatcher
import tl.extra.ccc.patcher.file.DungeonSelectBinFilePatcher
import tl.extra.ccc.patcher.file.ExtendedTextBinFilePatcher
import tl.extra.ccc.patcher.file.InfoMatrixDBinFilePatcher
import tl.extra.ccc.patcher.file.InfoMatrixTBinFilePatcher
import tl.extra.ccc.patcher.file.SgCommonTextBinFilePatcher
import tl.extra.ccc.patcher.file.SgNamedTextBinFilePatcher
import tl.extra.ccc.patcher.file.SgTextBinFilePatcher
import tl.extra.ccc.patcher.file.TextBinFilePatcher
import java.io.File
import java.nio.charset.Charset

open class PatcherBasedProcessor(
  private val pakExtract: File,
  private val outDir: File,
  unitDir: File,
  overrides: Map<Int, String> = mapOf(),
) {
  val translation = CccTranslation(unitDir.child("script-japanese.txt"), overrides = overrides, failOnLiteralNewLine = true)

  protected fun patchTextBinFile(path: String, translationOffset: Int, charset: Charset = Charsets.WINDOWS_932) {
    patchFile(path, translationOffset, charset, ::TextBinFilePatcher)
  }

  protected fun patchSgCommonTextBinFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::SgCommonTextBinFilePatcher)
  }

  protected fun patchSgTextBinFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::SgTextBinFilePatcher)
  }

  protected fun patchSgNamedTextBinFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::SgNamedTextBinFilePatcher)
  }

  protected fun patchExtendedTextBinFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::ExtendedTextBinFilePatcher)
  }

  protected fun patchDungeonSelectBinFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::DungeonSelectBinFilePatcher)
  }

  protected fun patchInfoMatrixDBinFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::InfoMatrixDBinFilePatcher)
  }

  protected fun patchInfoMatrixTBinFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
  ) {
    patchFile(path, translationOffset, charset, ::InfoMatrixTBinFilePatcher)
  }

  protected fun patchSjisFile(path: String, translationOffset: Int, charset: Charset = Charsets.WINDOWS_932) {
    patchFile(path, translationOffset, charset, ::SjisFilePatcher)
  }

  private fun patchFile(
    path: String,
    translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932,
    handler: (ByteArray, File, CccTranslation, Int, Charset) -> Any?,
  ) {
    println("Patching $path...")
    val bytes = pakExtract.child(path).readBytes()
    val out = outDir.child(path)
    out.parentFile.mkdirs()
    handler(bytes, out, translation, translationOffset, charset)
  }

  private fun patchRemappedFile(
    path: String,
    translationOffset: Int,
    relocationSpaceStartAddress: Int,
    relocationOut: KioOutputStream,
    charset: Charset = Charsets.WINDOWS_932,
    handler: (ByteArray, File, CccTranslation, Int, Charset, Int, KioOutputStream) -> Any?,
  ) {
    println("Patching $path...")
    val bytes = pakExtract.child(path).readBytes()
    val out = outDir.child(path)
    out.parentFile.mkdirs()
    handler(bytes, out, translation, translationOffset, charset, relocationSpaceStartAddress, relocationOut)
  }
}
