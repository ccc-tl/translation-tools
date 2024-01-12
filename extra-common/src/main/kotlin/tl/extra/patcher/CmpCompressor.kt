package tl.extra.patcher

import kio.KioInputStream
import kio.KioOutputStream
import kio.util.setBit
import java.io.ByteArrayOutputStream
import java.io.File

/** .CMP (IECP) file compressor. */
class CmpCompressor(file: File, secondPass: Boolean) {
  private val outStream = ByteArrayOutputStream()

  companion object {
    private const val MAX_CODED_PAIR_SIZE = 18
  }

  init {
    val dict = Dictionary(secondPass)
    var flag: Byte = 0
    var flagPos = 0
    var buffer = KioOutputStream(ByteArrayOutputStream())
    var dataBuffer = KioOutputStream(ByteArrayOutputStream())

    val out = KioOutputStream(outStream)
    out.writeString("IECP", 4)
    out.writeInt(file.length().toInt())

    fun writeDataBuffer() {
      out.writeByte(flag)
      out.writeBytes(dataBuffer.getAsByteArrayOutputStream().toByteArray())
      flag = 0
      flagPos = 0
      dataBuffer = KioOutputStream(ByteArrayOutputStream())
    }

    fun writePair(pairBytes: ByteArray, setFlagBit: Boolean) {
      dataBuffer.writeBytes(pairBytes)
      if (setFlagBit) {
        flag = flag.setBit(flagPos)
      }
      flagPos++
      if (flagPos == 8) {
        writeDataBuffer()
      }
    }

    with(KioInputStream(file)) {
      while (!eof()) {
        val byte = readByte()
        buffer.writeByte(byte)
        val bufferBytes = buffer.getAsByteArrayOutputStream().toByteArray()
        val index = dict.getSubArrayIndex(bufferBytes)
        if (index == -1 || bufferBytes.size == MAX_CODED_PAIR_SIZE || eof()) {
          if (bufferBytes.size <= 3) {
            // write normal bytes
            bufferBytes.forEach { bufByte ->
              writePair(byteArrayOf(bufByte), true)
              dict.write(bufByte)
            }
            buffer = KioOutputStream(ByteArrayOutputStream())
          } else {
            // write coded pair
            val bufToWrite = bufferBytes.sliceArray(0 until bufferBytes.lastIndex)
            val bufToWriteIndex = dict.getSubArrayIndex(bufToWrite)
            val codedPair = ((bufToWriteIndex and 0xFF shl 8) or (bufToWriteIndex and 0xF00 ushr 8 shl 4) or (bufToWrite.size - 3 and 0xF)) and 0xFFFF
            val c1 = (codedPair ushr 8 and 0xFF).toByte()
            val c2 = (codedPair and 0xFF).toByte()
            writePair(byteArrayOf(c1, c2), false)
            bufToWrite.forEach { bufByte -> dict.write(bufByte) }
            setPos(pos() - 1)
            buffer = KioOutputStream(ByteArrayOutputStream())
          }
        }
      }
      writeDataBuffer()
    }
  }

  fun getData(): ByteArray = outStream.toByteArray()

  private class Dictionary(private val secondPass: Boolean) {
    val data = ByteArray(4096)
    var pointer = 0xFEE

    fun write(byte: Byte) {
      data[pointer] = byte
      pointer++
      if (pointer == data.size) {
        pointer = 0
      }
    }

    operator fun get(offset: Int): Byte {
      return data[offset]
    }

    fun getSubArrayIndex(needle: ByteArray): Int {
      outer@ for (i in 0..pointer - needle.size) {
        for (j in needle.indices) {
          if (data[i + j] != needle[j]) {
            continue@outer
          }
        }
        return i
      }
      if (secondPass) {
        outer@ for (i in pointer..data.size - needle.size) {
          for (j in needle.indices) {
            if (data[i + j] != needle[j]) {
              continue@outer
            }
          }
          return i
        }
      }
      return -1
    }
  }
}
