package tl.extra.ccc.file

import kio.KioInputStream
import kio.util.toWHex
import tl.extra.file.tex.TexFile
import java.io.File

/**
 * Partial .PSP.IM model parser
 */
class PspImFile(val file: File) {
  val textures = mutableListOf<TexFile>()
  val textureEntries: Array<PspImTexEntry>

  init {
    val input = KioInputStream(file.readBytes())
    val fileTextures = mutableListOf<PspImTexEntry>()

    with(input) {
      val boneCount = readInt()
      val boneOffset = readInt()
      val texCount = readInt()
      val texOffset = readInt()
      val matCount = readInt()
      val matOffset = readInt()
      val meshCount = readInt()
      val meshOffset = readInt()
      println(
        "$boneCount bones (at ${boneOffset.toWHex()}), $texCount textures (at ${texOffset.toWHex()}), " +
          "$matCount materials (at ${matOffset.toWHex()}), $meshCount meshes (at ${meshOffset.toWHex()}), "
      )

      setPos(texOffset)
      val textureSectionBytes =
        readBytes(matOffset - texOffset + 0x20) // 20 bytes were missing, might be some other control data in there
      processTextures(texCount, textureSectionBytes)
      close()
    }

    textureEntries = fileTextures.toTypedArray()
  }

  private fun processTextures(texCount: Int, textureSectionBytes: ByteArray): Unit =
    with(KioInputStream(textureSectionBytes)) {
      readInt()
      val texInfos = mutableListOf<TexInfoEntry>()
      repeat(texCount) {
        readInt()
        readInt()
        val texName = readString(0x20).replace("\u0000", "")
        val texOffset = pos() + readInt()
        readInt()
        texInfos.add(TexInfoEntry(texName, texOffset))
      }

      texInfos.forEachIndexed { idx, texInfoEntry ->
        val textureEnd = texInfos.getOrNull(idx + 1)?.offset ?: size.toInt()
        println("Process texture ${texInfoEntry.name}")
        setPos(texInfoEntry.offset)
        val textureSize = textureEnd - texInfoEntry.offset
        val textureBytes = readBytes(textureSize)
        textures.add(TexFile(textureBytes))
      }
    }
}

private class TexInfoEntry(val name: String, val offset: Int)

class PspImTexEntry(val name: String, val bytes: ByteArray)
