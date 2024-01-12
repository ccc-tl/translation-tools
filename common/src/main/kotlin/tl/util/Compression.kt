package tl.util

import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

fun compressBytes(input: ByteArray): ByteArray {
  val output = ByteArrayOutputStream()
  val stream = DeflaterOutputStream(output)
  stream.write(input)
  stream.close()
  return output.toByteArray()
}
