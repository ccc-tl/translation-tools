package tl.extra.ccc.patcher.translation

import kio.util.child
import tl.extra.ccc.patcher.ItemParam01Remapper
import tl.extra.ccc.patcher.ItemParam04Remapper
import java.io.File

class ItemsTranslationProcessor(pakExtract: File, outDir: File, unitDir: File) {
  val remapper01: ItemParam01Remapper
  val remapper04: ItemParam04Remapper

  init {
    val jp01Data = pakExtract.child("cmn/item_param_01.bin").readBytes()
    val jp04Data = pakExtract.child("cmn/item_param_04.bin").readBytes()
    val itemsTranslation = CccTranslation(unitDir.child("script-japanese.txt"), failOnLiteralNewLine = true)
    remapper01 = ItemParam01Remapper(
      jp01Data,
      outDir.child("cmn/item_param_01.bin"),
      itemsTranslation,
      0
    )
    remapper04 = ItemParam04Remapper(
      jp04Data,
      outDir.child("cmn/item_param_04.bin"),
      itemsTranslation,
      452
    )
  }
}
