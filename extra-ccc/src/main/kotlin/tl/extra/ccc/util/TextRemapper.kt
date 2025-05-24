package tl.extra.ccc.util

import kio.KioOutputStream
import kio.util.toWHex
import java.io.ByteArrayOutputStream

const val REMAP_TEXT_TEMPLATE = "!rmap>!"
const val REMAP_RELATIVE_TEXT_TEMPLATE = "!rmap>@"
const val REMAP_COMPILED_TEXT_TEMPLATE = "!rmap>#"

fun getTextRemapBytes(addr: Int): ByteArray {
  return getTemplateRelocationBytes(REMAP_TEXT_TEMPLATE, addr)
}

fun getTextRemapRelativeBytes(addr: Int): ByteArray {
  return getTemplateRelocationBytes(REMAP_RELATIVE_TEXT_TEMPLATE, addr)
}

fun getTextRemapCompiledBytes(addr: Int): ByteArray {
  return getTemplateRelocationBytes(REMAP_COMPILED_TEXT_TEMPLATE, addr)
}

private fun getTemplateRelocationBytes(template: String, addr: Int): ByteArray {
  val outStream = KioOutputStream(ByteArrayOutputStream())
  outStream.writeString(template)
  outStream.writeString(addr.toWHex())
  outStream.writeBytes(ByteArray(1))
  return outStream.getAsByteArrayOutputStream().toByteArray()
}

class TextRemapperResult(
  val relocationBytes: ByteArray,
  val relocationEndAddress: Int,
)
