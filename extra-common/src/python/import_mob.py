# Fate/Extra and Fate/Extra CCC (PSP) animation importer for Blender

# Script Configuration

# Name of Blender skeleton to which animation data will be applied
skeletonTargetName = 'Armature'

# Animation file path
# You can either enter absolutes path or simply copy file
# next to current Blender file and only type file names
mobPath = '0002.mob'

# Index of the animation to be imported
# Set to -1 to import every animation from the .mob file (useful for testing)
# Importing every animation may take long time, Blender will appear unresponsive
animationToBeLoadedIdx = 0

# This will delete all previous animation data before importing
# Set to True to avoid weird artifacts when testing multiple animations
deleteOldKeyframes = True

# For Fate/Extra CCC you might need to set bone name prefix that
# appears in the mob file. See system console if you don't see any
# changes after running script.
bonePrefix = ""

# ----------------------------------

import os
from pathlib import Path
from collections import namedtuple
from struct import *

import bpy
from mathutils import Quaternion
from mathutils import Euler

ImmHeader = namedtuple('ImmHeader', ['frameCount', 'boneCount', 'unk2', 'otherFrameCount',
                                     'mode', 'unk5', 'unk6', 'unk7',
                                     'unk8', 'unk9', 'unk10', 'unk11',
                                     'unk12', 'unk13', 'unk14', 'unk15',
                                     'unk16', 'unk17', 'unk18', 'unk19'])
ImmBoneHeader = namedtuple('ImmBoneHeader', ['name', 'id', 'boneFrames', 'unk3',
                                             'unk4', 'unk5', 'posFrames', 'rotFrames',
                                             'sclFrames', 'posDataOffset', 'rotDataOffset', 'sclDataOffset',
                                             'unk12'])
ImmBone = namedtuple('ImmBone', ['header', 'posFrames', 'rotFrames', 'sclFrames'])
ImmPosFrame = namedtuple('ImmPosFrame', ['x', 'y', 'z', 'frameIndex', 'fileIndex'])
ImmSclFrame = namedtuple('ImmSclFrame', ['x', 'y', 'z', 'frameIndex', 'fileIndex'])
ImmRotFrame = namedtuple('ImmRotFrame', ['w', 'x', 'y', 'z', 'frameIndex', 'fileIndex'])
ImmRotInterpFrame = namedtuple('ImmRotInterpFrame', ['prevFrame', 'nextFrame', 'factor'])


def importMob():
    mob = Path(mobPath)
    blenderFileDir = os.path.dirname(bpy.data.filepath)
    os.chdir(blenderFileDir)
    if not mob.is_file():
        raise ValueError('Can\'t find source .mob file')
    if deleteOldKeyframes:
        bpy.data.objects[skeletonTargetName].animation_data_clear()

    animationOffsets = readPakHeader()

    if animationToBeLoadedIdx != -1 and animationToBeLoadedIdx >= len(animationOffsets):
        raise ValueError('Animation index out of range, max: ' + str(len(animationOffsets) - 1))

    animationsToLoad = []
    if animationToBeLoadedIdx == -1:
        animationsToLoad = range(len(animationOffsets))
    else:
        animationsToLoad.append(animationToBeLoadedIdx)

    globalOffset = 0
    for animationIdx in animationsToLoad:
        header, frameIndexed = readImmHeader(animationOffsets[animationIdx])
        bones = readImmData(animationOffsets[animationIdx], header, frameIndexed)
        framesUsed = importToBlender(header, frameIndexed, bones, globalOffset)
        globalOffset += framesUsed
    bpy.context.scene.frame_start = 0
    bpy.context.scene.frame_end = globalOffset
    bpy.context.scene.frame_current = 0


def importToBlender(header, frameIndexed, bones, globalOffset):
    maxFramesUsed = 0
    for bone in bones:
        blenderBone = getBoneByName(bone.header.name)
        if blenderBone is None:
            print('WARN: Can\'t find Blender bone: ' + bone.header.name)
            continue
        for posIdx, posFrame in enumerate(bone.posFrames):
            realIdx = posIdx * 2
            if frameIndexed:
                readIdx = posFrame.frameIndex
            blenderBone.location = [posFrame.x / 10, posFrame.y / 10, posFrame.z / 10]
            blenderBone.keyframe_insert(data_path='location', frame=globalOffset + realIdx)

        realFrameCount = header.frameCount * 2
        maxRotFrameIdx = bone.rotFrames[-1].frameIndex + 1
        if maxRotFrameIdx > realFrameCount:
            print("WARN: Bone has more frames than animation header, please manually check animation length.")
        rotFrameList = [None] * max(realFrameCount, maxRotFrameIdx)
        for rotIdx, rotFrame in enumerate(bone.rotFrames):
            realIdx = rotIdx * 2
            if frameIndexed:
                readIdx = rotFrame.frameIndex
            rotFrameList[realIdx] = rotFrame
            blenderBone.rotation_quaternion = [rotFrame.w, rotFrame.x, rotFrame.y, rotFrame.z]
            blenderBone.keyframe_insert(data_path='rotation_quaternion', frame=globalOffset + realIdx)

        if len(bone.rotFrames) < 1:
            continue

        for idx, rotFrame in enumerate(rotFrameList):
            if rotFrame is not None:
                continue
            prevFrame = None
            nextFrame = None
            for frame in reversed(rotFrameList[0:idx]):
                if isinstance(frame, ImmRotFrame):
                    prevFrame = frame
                    break
            for frame in rotFrameList[idx:]:
                if isinstance(frame, ImmRotFrame):
                    nextFrame = frame
                    break
            if prevFrame is None or nextFrame is None:
                continue
            prevFrameIdx = rotFrameList.index(prevFrame)
            nextFrameIdx = rotFrameList.index(nextFrame)
            missingFrames = nextFrameIdx - prevFrameIdx
            for missIdx in range(missingFrames - 1):
                rotFrameList[idx + missIdx] = ImmRotInterpFrame(prevFrame, nextFrame, (missIdx + 1) / missingFrames)

        for rotIdx, rotFrame in enumerate(rotFrameList):
            if not isinstance(rotFrame, ImmRotInterpFrame):
                continue
            q1 = Quaternion((rotFrame.prevFrame.w, rotFrame.prevFrame.x, rotFrame.prevFrame.y, rotFrame.prevFrame.z))
            q2 = Quaternion((rotFrame.nextFrame.w, rotFrame.nextFrame.x, rotFrame.nextFrame.y, rotFrame.nextFrame.z))
            q3 = q1.slerp(q2, rotFrame.factor)
            blenderBone.rotation_quaternion = q3
            blenderBone.keyframe_insert(data_path='rotation_quaternion', frame=globalOffset + rotIdx)
        maxFramesUsed = max(maxFramesUsed, len(rotFrameList), len(bone.posFrames))
    return maxFramesUsed


def getBoneByName(name):
    for blender_bone in bpy.data.objects[skeletonTargetName].pose.bones.items():
        if bonePrefix + blender_bone[0] == name:
            return blender_bone[1]


def readPakHeader():
    sizes = []
    fileStart = 0
    with open(mobPath, 'rb') as mob:
        stream = LEInputStream(mob)
        count = stream.readShort()
        hasPaths = stream.readShort() & 0xffff == 0x8000
        for i in range(count):
            sizes.append(stream.readInt())
        while stream.tell() % 16 != 0:
            stream.readByte()
        fileStart = stream.tell()
    offsets = []
    for size in sizes:
        if hasPaths:
            fileStart += 0x40
        offsets.append(fileStart)
        fileStart += size
    return offsets


def readImmHeader(immOffset):
    with open(mobPath, 'rb') as mob:
        stream = LEInputStream(mob)
        stream.seek(immOffset)
        header = ImmHeader(
            stream.readInt(), stream.readInt(), stream.readInt(), stream.readInt(),
            stream.readInt(), stream.readInt(), stream.readInt(), stream.readInt(),
            stream.readInt(), stream.readInt(), stream.readInt(), stream.readInt(),
            stream.readInt(), stream.readInt(), stream.readInt(), stream.readInt(),
            stream.readInt(), stream.readInt(), stream.readInt(), stream.readInt()
        )
    frameIndexed = header.mode & 0b100
    return header, frameIndexed


def readImmData(immOffset, header, frameIndexed):
    with open(mobPath, 'rb') as mob:
        stream = LEInputStream(mob)
        stream.seek(immOffset + 0x50)
        bones = []
        for _ in range(header.boneCount):
            headerStart = stream.tell()
            name = stream.readString()
            stream.seek(headerStart + 0x20)
            immId = stream.readInt()
            boneFrames = stream.readInt()
            unk3 = stream.readInt()
            unk4 = stream.readInt()
            unk5 = stream.readInt()
            posFrames = stream.readInt()
            rotFrames = stream.readInt()
            sclFrames = stream.readInt()
            posDataOffset = stream.tell() + stream.readInt()
            rotDataOffset = stream.tell() + stream.readInt()
            sclDataOffset = stream.tell() + stream.readInt()
            unk12 = stream.readInt()
            bones.append(
                ImmBoneHeader(
                    name, immId, boneFrames, unk3,
                    unk4, unk5, posFrames, rotFrames,
                    sclFrames, posDataOffset, rotDataOffset,
                    sclDataOffset, unk12
                )
            )
        bonesData = []
        for bone in bones:
            posFrames = []
            rotFrames = []
            sclFrames = []

            stream.seek(bone.posDataOffset)
            for fileIdx in range(bone.posFrames):
                x = stream.readFloat()
                y = stream.readFloat()
                z = stream.readFloat()
                frameIdx = 0
                if frameIndexed:
                    frameIdx = stream.readInt()
                posFrames.append(ImmPosFrame(x, y, z, frameIdx, fileIdx))

            stream.seek(bone.rotDataOffset)
            for fileIdx in range(bone.rotFrames):
                x = stream.readShort()
                y = stream.readShort()
                z = stream.readShort()
                w = stream.readShort()
                frameIdx = 0
                if frameIndexed:
                    frameIdx = stream.readShort()
                rotFrames.append(ImmRotFrame(w, x, y, z, frameIdx, fileIdx))

            stream.seek(bone.sclDataOffset)
            for fileIdx in range(bone.sclFrames):
                x = stream.readShort()
                y = stream.readShort()
                z = stream.readShort()
                frameIdx = 0
                if frameIndexed:
                    frameIdx = stream.readShort()
                sclFrames.append(ImmSclFrame(x, y, z, frameIdx, fileIdx))

            bonesData.append(ImmBone(bone, posFrames, rotFrames, sclFrames))
    return bonesData


class LEInputStream:
    def __init__(self, stream):
        self.stream = stream

    def readByte(self):
        return unpack('b', self.stream.read(1))[0]

    def readShort(self):
        return unpack('<h', self.stream.read(2))[0]

    def readInt(self):
        return unpack('<i', self.stream.read(4))[0]

    def readFloat(self):
        return unpack('<f', self.stream.read(4))[0]

    def readString(self):
        data = bytearray()
        while True:
            byte = self.readByte()
            if byte == 0:
                break
            data.append(byte)
        return data.decode('ascii')

    def tell(self):
        return self.stream.tell()

    def seek(self, offset, whence=0):
        self.stream.seek(offset, whence)


importMob()
