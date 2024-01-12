package tl.extra.ccc.file

import kio.KioInputStream
import kio.util.isBitSet
import kio.util.toWHex
import java.io.File

/**
 * Parser for 3D .IMM files, along with Python script generator for easy import into Blender
 * */
class ImmFile(bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  private val header: ImmHeader
  private val bones: MutableList<ImmBone>

  init {
    val input = KioInputStream(bytes)

    with(input) {
      header = ImmHeader(
        readInt(), readInt(), readInt(), readInt(), readInt(),
        readInt(), readInt(), readInt(), readInt(), readInt(),
        readInt(), readInt(), readInt(), readInt(), readInt(),
        readInt(), readInt(), readInt(), readInt(), readInt()
      )
      bones = mutableListOf()

      repeat(header.bonesCount) {
        val name = readStringAndTrim(0x20)
        val id = readInt()
        val boneFrames = readInt()
        val unk0 = readInt()
        val unk1 = readInt()
        val unk2 = readInt()
        val posFrames = readInt()
        val rotFrames = readInt()
        val scaleFrames = readInt()
        val posDataOffset = pos() + readInt()
        val rotDataOffset = pos() + readInt()
        val scaleDataOffset = pos() + readInt()
        val unk3 = readInt()
        bones.add(
          ImmBone(
            name,
            id,
            boneFrames,
            unk0,
            unk1,
            unk2,
            posFrames,
            rotFrames,
            scaleFrames,
            posDataOffset,
            rotDataOffset,
            scaleDataOffset,
            unk3
          )
        )
      }

      bones.forEach { bone ->
        setPos(bone.posDataOffset)
        repeat(bone.posFrames) {
          val x = readFloat()
          val y = readFloat()
          val z = readFloat()
          val idx = if (header.isFrameIndexed()) readInt() else 0
          bone.posFramesList.add(ImmPosFrame(x, y, z, idx))
        }

        setPos(bone.rotDataOffset)
        repeat(bone.rotFrames) {
          val x = readShort()
          val y = readShort()
          val z = readShort()
          val w = readShort()
          val idx = if (header.isFrameIndexed()) readShort().toInt() else 0
          bone.rotFramesList.add(ImmRotFrame(w, x, y, z, idx))
        }

        setPos(bone.scaleDataOffset)
        repeat(bone.scaleFrames) {
          val x = readShort()
          val y = readShort()
          val z = readShort()
          val idx = if (header.isFrameIndexed()) readShort().toInt() else 0
          bone.scaleFramesList.add(ImmScaleFrame(x, y, z, idx))
        }
      }

      close()
    }
  }

  fun debugDump() {
    println(header)
    bones.forEach(::println)
    println()
    bones.forEach { bone ->
      println("--------------------- Bone '${bone.name}' ---------------------")
      println()
      println(bone.posFramesList.size)
      bone.posFramesList.forEach(::println)
      println()
      println(bone.rotFramesList.size)
      bone.rotFramesList.forEach(::println)
      println()
      println(bone.scaleFramesList.size)
      bone.scaleFramesList.forEach(::println)
      println()
    }
  }

  fun emitPython(ignoreLastFrames: Int = 1, globalOffset: Int = 0): String {
    val boneScript = StringBuilder()

    var boneFramesExceedAnimation = false

    bones.forEach { bone ->
      val frameScript = StringBuilder()
      bone.posFramesList.forEachIndexed { index, frame ->
        val realIdx = if (header.isFrameIndexed()) frame.frameIndex else index * 2
        frameScript.append("    blender_bone.location = [${frame.x / 10}, ${frame.y / 10}, ${frame.z / 10}]\n")
        frameScript.append("    blender_bone.keyframe_insert(data_path='location', frame=${globalOffset + realIdx})\n")
      }

      val realFrameCount = header.framesCount * 2
      val boneFrameMaxIdx = bone.rotFramesList.last().frameIndex + 1
      if (boneFrameMaxIdx > realFrameCount) {
        boneFramesExceedAnimation = true
      }
      val realFrameList = arrayOfNulls<Any>(Math.max(realFrameCount, boneFrameMaxIdx))
      bone.rotFramesList.forEachIndexed { index, frame ->
        val realIdx = if (header.isFrameIndexed()) frame.frameIndex else index * 2
        realFrameList[realIdx] = frame
        frameScript.append("    blender_bone.rotation_quaternion = [${frame.w} , ${frame.x}, ${frame.y}, ${frame.z}]\n")
        frameScript.append("    blender_bone.keyframe_insert(data_path='rotation_quaternion', frame=${globalOffset + realIdx})\n")
      }

      if (bone.rotFrames > 1) {
        println("Interpolate bone ${bone.name}")
        realFrameList.forEachIndexed { index, obj ->
          if (obj == null) {
            val prevFrame = realFrameList.slice(0 until index)
              .filterNotNull()
              .filterIsInstance<ImmRotFrame>()
              .lastOrNull()
            val nextFrame = realFrameList.slice(index until realFrameList.size)
              .filterNotNull()
              .filterIsInstance<ImmRotFrame>()
              .firstOrNull()

            if (prevFrame == null || nextFrame == null) {
              return@forEachIndexed
            }

            val prevFrameIdx = realFrameList.indexOf(prevFrame)
            val nextFrameIdx = realFrameList.indexOf(nextFrame)

            val missingFrames = nextFrameIdx - prevFrameIdx
            repeat(missingFrames - 1) { iteration ->
              realFrameList[index + iteration] =
                FrameInterpolation(
                  prevFrame,
                  nextFrame,
                  (iteration + 1) / missingFrames.toFloat()
                )
            }
          }
        }

        realFrameList.forEachIndexed { idx, interp ->
          if (interp is FrameInterpolation) {
            frameScript.append("    q1 = Quaternion((${interp.prevFrame.w} , ${interp.prevFrame.x}, ${interp.prevFrame.y}, ${interp.prevFrame.z}))\n")
            frameScript.append("    q2 = Quaternion((${interp.nextFrame.w} , ${interp.nextFrame.x}, ${interp.nextFrame.y}, ${interp.nextFrame.z}))\n")
            frameScript.append("    q3 = q1.slerp(q2, ${interp.factor})\n")
            frameScript.append("    blender_bone.rotation_quaternion = q3\n")
            frameScript.append("    blender_bone.keyframe_insert(data_path='rotation_quaternion', frame=${globalOffset + idx})\n")
          }
        }
      }

      boneScript.append(
        """
blender_bone = get_bone_by_name('${bone.name}')
if not blender_bone is None:
$frameScript
else:
    print ("WARN: No such bone '${bone.name}'")
"""
      )
    }

    if (boneFramesExceedAnimation) {
      println("WARN: At least one bone has more frames than animation header, please manually check animation length.")
    }

    val script = """import bpy
from mathutils import Quaternion
from mathutils import Euler

def get_bone_by_name(name):
    for blender_bone in bpy.data.objects['Armature'].pose.bones.items():
        if blender_bone[0] == name:
            return blender_bone[1]

ctx = bpy.context
ops = bpy.ops

ctx.scene.frame_start = 0
ctx.scene.frame_end = ${header.framesCount * 2 - ignoreLastFrames}
ctx.scene.frame_current = 0

$boneScript
"""
    return script
  }
}

data class ImmHeader(
  val framesCount: Int,
  val bonesCount: Int,
  val unk3: Int,
  val otherFrameCount: Int,
  val mode: Int,

  val unk6: Int,
  val unk7: Int,
  val unk8: Int,
  val unk9: Int,
  val unk10: Int,

  val unk11: Int,
  val unk12: Int,
  val unk13: Int,
  val unk14: Int,
  val unk15: Int,

  val unk16: Int,
  val unk17: Int,
  val unk18: Int,
  val unk19: Int,
  val unk20: Int,
) {
  fun isFrameIndexed(): Boolean = mode.isBitSet(2)
}

data class ImmBone(
  val name: String,
  val id: Int,
  val boneFrames: Int,
  val unk0: Int,
  val unk1: Int,
  val unk2: Int,
  val posFrames: Int,
  val rotFrames: Int,
  val scaleFrames: Int,
  val posDataOffset: Int,
  val rotDataOffset: Int,
  val scaleDataOffset: Int,
  val unk3: Int,
) {

  val posFramesList = mutableListOf<ImmPosFrame>()
  val rotFramesList = mutableListOf<ImmRotFrame>()
  val scaleFramesList = mutableListOf<ImmScaleFrame>()

  override fun toString(): String {
    return "ImmBone(name='$name', id=$id, boneFrames=$boneFrames, unk0=$unk0, unk1=$unk1, unk2=$unk2, " +
      "posFrames=$posFrames, rotFrames=$rotFrames, scaleFrames=$scaleFrames, posDataOffset=0x${posDataOffset.toWHex()}, " +
      "rotDataOffset=0x${rotDataOffset.toWHex()}, scaleDataOffset=0x${scaleDataOffset.toWHex()}, unk3=$unk3)"
  }
}

class ImmPosFrame(val x: Float, val y: Float, val z: Float, val frameIndex: Int = 0) {
  override fun toString(): String {
    return "ImmPosFrame(x=$x, y=$y, z=$z, frameIndex=$frameIndex)"
  }
}

class ImmRotFrame(val w: Short, val x: Short, val y: Short, val z: Short, val frameIndex: Int = 0) {
  override fun toString(): String {
    return "ImmRotFrame(w=$w, x=$x, y=$y, z=$z, frameIndex=$frameIndex)"
  }
}

class ImmScaleFrame(val x: Short, val y: Short, val z: Short, val frameIndex: Int = 0) {
  override fun toString(): String {
    return "ImmScaleFrame(x=$x, y=$y, z=$z, frameIndex=$frameIndex)"
  }
}

class FrameInterpolation(val prevFrame: ImmRotFrame, val nextFrame: ImmRotFrame, val factor: Float)
