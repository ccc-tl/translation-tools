package tl.util

import kio.util.toUnsignedInt
import java.io.ByteArrayOutputStream

class ReadableByteArrayOutputStream : ByteArrayOutputStream {
  constructor() : super()
  constructor(size: Int) : super(size)

  @Synchronized
  fun write(byte: Byte) {
    super.write(byte.toUnsignedInt())
  }

  @Synchronized
  fun at(pos: Int): Byte {
    return buf[pos]
  }

  @Synchronized
  fun count(): Int {
    return super.count
  }
}
