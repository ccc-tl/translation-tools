#include "subs.h"

void subtitle_ctl::clear() {
    audioPathHash = 0;
    skipMode = 0;
    x = 0;
    y = 0;
    partsCount = 0;
    parts = NULL;
    currentPart = 0;
    currentPartRemainTime = 0;
    audioCtl = NULL;
}

u16 subtitle_ctl::totalRemainingTime() {
    u16 timeLeft = currentPartRemainTime;
    for (u32 i = currentPart + 1; i < partsCount; i++) {
        timeLeft += parts[i].displayTime;
    }
    return timeLeft;
}

void subtitle_ctl::loadSubs(const subtitle* sub) {
    audioPathHash = sub->audioPathHash;
    skipMode = sub->skipMode;
    partsCount = sub->partsCount;
    parts = sub->parts;
    currentPart = 0;
    currentPartRemainTime = parts[currentPart].displayTime;
}
