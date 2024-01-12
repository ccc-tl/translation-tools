#pragma once
#include "platform.h"

struct subtitle_part {
    const char* const text;
    const i16 x;
    const u16 displayTime;
};

struct subtitle {
    const i32 audioPathHash;
    const u8 skipMode;
    const u8 displayMode;
    const u8 dispatchMode;
    const u8 partsCount;
    const subtitle_part* const parts;
};

struct subtitle_ctl {
    u32 slotId;

    i32 audioPathHash;
    u8 skipMode;
    i32 x;
    i32 y;
    u8 partsCount;
    const subtitle_part* parts;
    u16 currentPart;
    u16 currentPartRemainTime;
    const void** audioCtl;

    void clear();
    u16 totalRemainingTime();
    void loadSubs(const subtitle* sub);
};
