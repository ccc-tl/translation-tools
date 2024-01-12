#pragma once

#include "generated.h"
#include "hook.h"
#include "subs.h"

static const u32 totalMaxSubs = 3;
static const u32 totalSubsSpacing = 30;
static const i32 lineHeight = 18;

struct subtitle_draw {
    subtitle_ctl subsCtl[totalMaxSubs];
    i32 subsSpacingTable[totalSubsSpacing];
    u32 currentMaxSubs;
    i32 drawList;
    i32 baseSubY;
    bool spaceNewUpward;
    u8 textAlpha;

    void clearAllSubs();
    void clearSubsLayout(i32 slotId);
    i32 layoutNewSubs(i32 slotId, u32 lineCount);
    subtitle_ctl* getBestFreeSubCtl();
    bool subsTickPartTime(subtitle_ctl* ctl);
    void subsScheduleSubtitles(const subtitle* sub, const void** audioCtl);
    void removeSubsWithAudioCtl(const void** audioCtl);
    void removeSubsWithAudioPath(const char* const audioPath);
    void removeSubByIndex(i32 index);
};
