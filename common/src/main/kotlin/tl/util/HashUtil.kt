package tl.util

fun sdbmHash(bytes: ByteArray): Int {
  var hash = 0
  bytes.forEach { byte ->
    hash = byte + (hash shl 6) + (hash shl 16) - hash
  }
  return hash
}

fun ensureNoSdbmHashCollisions(byteArrays: List<ByteArray>) {
  val hashes = mutableSetOf<Int>()
  byteArrays.forEach { bytes ->
    val hash = sdbmHash(bytes)
    if (!hashes.add(hash)) {
      error("Hash collision found")
    }
  }
}
