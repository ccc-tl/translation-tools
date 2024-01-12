#include "platform.h"

extern "C" {

// 0: ???
// 1: ??x
// 2: ?x?
// 3: ?xx
// 4: x??
// 5: x?x
// 6: xx?
// 7: xxx

// NOTE: BB (M6) has different mapping because her third skill has 2 textures
// 0: ???
// 1: xx?
// 2: ??1
// 3: xx1
// 4: ??2
// 5: xx2
// 6: ??2
// 7: xx2

#define INFOMATRIX_DEBUG 0

static const i16 m1Offsets[] = {0xFFBA, 0xFFBB, 0xFFBA, 0xFFBB, 0xFF70, 0xFF70, 0xFF70, 0xFF70};
static const i16 m2Offsets[] = {0xFFBA, 0xFFBA, 0xFFBA, 0xFFBA, 0xFF4B, 0xFF4A, 0xFF4B, 0xFF4A};
static const i16 m3Offsets[] = {0xFFBA, 0x0000, 0x0000, 0xFFBB, 0xFFB9, 0x0000, 0x0000, 0xFFB9};
static const i16 m4Offsets[] = {0xFFBA, 0xFFBC, 0x0000, 0x0000, 0x0000, 0x0000, 0xFF70, 0xFF71};
static const i16 m6Offsets[] = {0xFFBA, 0xFF97 - 0xF, 0xFFBA, 0xFF97 - 0xF, 0xFFBC, 0xFF98 - 0xF, 0xFFBC, 0xFF98 - 0xF};
static const i16 m7Offsets[] = {0xFFBA, 0xFFBB, 0xFFBA, 0xFFBB, 0xFF4C, 0xFF4D, 0xFF4C, 0xFF4C};

#if INFOMATRIX_DEBUG == 1
static const i32 infoMatrixCounterMax = 7;
static i32 infoMatrixCounter = 0;
static const i32 infoMatrixDelayMax = 100;
static i32 infoMatrixDelayRemaining = infoMatrixDelayMax;

static u8* infomatrixData = (u8*)(0x08B28F55);
#else
static const u8* infomatrixData = (u8*)(0x08B28F55);
#endif

i32 cccInfomatrixSkill3Fixer(const i32 defaultValue, const u32 matrixIdx, const u32 loopIndex) {
    const u32 matrixId = matrixIdx + 1;
    if (loopIndex != 1) {
        return defaultValue;
    }
    bool skill1Unlocked;
    bool skill2Unlocked;
    bool skill3Unlocked;

#if INFOMATRIX_DEBUG == 1
    infoMatrixDelayRemaining--;
    if (infoMatrixDelayRemaining < 0) {
        infoMatrixDelayRemaining = infoMatrixDelayMax;
        infoMatrixCounter++;
        if (infoMatrixCounter > infoMatrixCounterMax) {
            infoMatrixCounter = 0;
        }
    }
    u32 skill1Byte = 3;
    u32 skill2Byte = 4;
    u32 skill3Byte = 4;
    u32 skill1Bit = 7;
    u32 skill2Bit = 0;
    u32 skill3Bit = 1;
    bool skill1State = (infoMatrixCounter & 0b100) >> 2;
    bool skill2State = (infoMatrixCounter & 0b010) >> 1;
    bool skill3State = (infoMatrixCounter & 0b001);
    infomatrixData[skill1Byte] &= ~(1U << skill1Bit);
    infomatrixData[skill2Byte] &= ~(1U << skill2Bit);
    infomatrixData[skill3Byte] &= ~(1U << skill3Bit);
    if (skill1State) {
        infomatrixData[skill1Byte] |= 1U << skill1Bit;
    }
    if (skill2State) {
        infomatrixData[skill2Byte] |= 1U << skill2Bit;
    }
    if (skill3State) {
        infomatrixData[skill3Byte] |= 1U << skill3Bit;
    }
#endif

    switch (matrixId) {
    case 1:
        skill1Unlocked = isBitSet(infomatrixData[3], 1);
        skill2Unlocked = isBitSet(infomatrixData[3], 2);
        skill3Unlocked = isBitSet(infomatrixData[9], 4);
        return m1Offsets[(skill1Unlocked << 2) | (skill2Unlocked << 1) | skill3Unlocked];
    case 2:
        skill1Unlocked = isBitSet(infomatrixData[4], 5);
        skill2Unlocked = isBitSet(infomatrixData[4], 6);
        skill3Unlocked = isBitSet(infomatrixData[4], 7);
        return m2Offsets[(skill1Unlocked << 2) | (skill2Unlocked << 1) | skill3Unlocked];
    case 3:
        skill1Unlocked = isBitSet(infomatrixData[6], 7);
        skill2Unlocked = isBitSet(infomatrixData[7], 1);
        skill3Unlocked = isBitSet(infomatrixData[7], 1);
        return m3Offsets[(skill1Unlocked << 2) | (skill2Unlocked << 1) | skill3Unlocked];
    case 4:
        skill1Unlocked = isBitSet(infomatrixData[6], 1);
        skill2Unlocked = isBitSet(infomatrixData[6], 1);
        skill3Unlocked = isBitSet(infomatrixData[10], 0);
        return m4Offsets[(skill1Unlocked << 2) | (skill2Unlocked << 1) | skill3Unlocked];
    case 6:
        skill1Unlocked = isBitSet(infomatrixData[2], 3); // 1 & 2
        skill2Unlocked = isBitSet(infomatrixData[2], 4); // 3 v1
        skill3Unlocked = isBitSet(infomatrixData[2], 5); // 3 v2
        return m6Offsets[(skill3Unlocked << 2) | (skill2Unlocked << 1) | skill1Unlocked];
    case 7:
        skill1Unlocked = isBitSet(infomatrixData[3], 7);
        skill2Unlocked = isBitSet(infomatrixData[4], 0);
        skill3Unlocked = isBitSet(infomatrixData[4], 1);
        return m7Offsets[(skill1Unlocked << 2) | (skill2Unlocked << 1) | skill3Unlocked];
    default:
        return defaultValue;
    }
}
}
