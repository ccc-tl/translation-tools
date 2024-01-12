package tl.extra.ccc.util

import kio.KioOutputStream
import kio.util.toWHex
import java.io.ByteArrayOutputStream

const val remapTextTemplate = "!rmap>!"
const val remapRelativeTextTemplate = "!rmap>@"
const val remapCompiledTextTemplate = "!rmap>#"

fun getTextRemapBytes(addr: Int): ByteArray {
  return getTemplateRelocationBytes(remapTextTemplate, addr)
}

fun getTextRemapRelativeBytes(addr: Int): ByteArray {
  return getTemplateRelocationBytes(remapRelativeTextTemplate, addr)
}

fun getTextRemapCompiledBytes(addr: Int): ByteArray {
  return getTemplateRelocationBytes(remapCompiledTextTemplate, addr)
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
