#include "subs_draw.h"
#include "util.h"

void subtitle_draw::clearAllSubs() {
    for (u32 i = 0; i < totalMaxSubs; i++) {
        subsCtl[i].clear();
        subsCtl[i].slotId = i;
    }
    for (u32 i = 0; i < totalSubsSpacing; i++) {
        subsSpacingTable[i] = -1;
    }
}

void subtitle_draw::clearSubsLayout(i32 slotId) {
    for (u32 i = 0; i < totalSubsSpacing; i++) {
        if (subsSpacingTable[i] == slotId) {
            subsSpacingTable[i] = -1;
        }
    }
}

i32 subtitle_draw::layoutNewSubs(i32 slotId, u32 lineCount) {
    u32 matched = 0;
    for (u32 i = 0; i < totalSubsSpacing; i++) {
        if (subsSpacingTable[i] == -1) {
            matched++;
            if (matched >= lineCount) {
                u32 startSlot = i - (matched - 1);
                for (u32 j = startSlot; j < startSlot + lineCount; j++) {
                    subsSpacingTable[j] = slotId;
                }
                if (spaceNewUpward) {
                    return baseSubY - (startSlot * lineHeight) - (lineCount * lineHeight);
                } else {
                    return baseSubY + (startSlot * lineHeight);
                }
            }
        } else {
            matched = 0;
        }
    }
    return baseSubY; // fallback
}

subtitle_ctl* subtitle_draw::getBestFreeSubCtl() {
    for (u32 i = 0; i < currentMaxSubs; i++) {
        if (subsCtl[i].parts == NULL) return &subsCtl[i];
    }
    subtitle_ctl* minRemainCtl = &subsCtl[0];
    for (u32 i = 0; i < currentMaxSubs; i++) {
        if (subsCtl[i].totalRemainingTime() < minRemainCtl->totalRemainingTime()) {
            minRemainCtl = &subsCtl[i];
        }
    }
    clearSubsLayout(minRemainCtl->slotId);
    minRemainCtl->clear();
    return minRemainCtl;
}

bool subtitle_draw::subsTickPartTime(subtitle_ctl* ctl) {
    ctl->currentPartRemainTime--;
    if (ctl->currentPartRemainTime == 0) {
        ctl->currentPart++;
        clearSubsLayout(ctl->slotId);
        if (ctl->currentPart == ctl->partsCount) { // no more parts, clear subs
            ctl->clear();
            return true;
        }
        ctl->x = ctl->parts[ctl->currentPart].x;
        if (GameStrlen(ctl->parts[ctl->currentPart].text + 1) > 0) {
            ctl->y = layoutNewSubs(ctl->slotId, ctl->parts[ctl->currentPart].text[0]);
        }
        ctl->currentPartRemainTime = ctl->parts[ctl->currentPart].displayTime;
    }
    return false;
}

void subtitle_draw::subsScheduleSubtitles(const subtitle* sub, const void** audioCtl) {
    if (sub == NULL) {
        return;
    }
    for (u32 i = 0; i < currentMaxSubs; i++) {
        // if this sound is already playing and current part is still the first one then just reset part timer
        subtitle_ctl& ctl = subsCtl[i];
        if (ctl.audioPathHash == sub->audioPathHash && ctl.currentPart == 0) {
            ctl.currentPartRemainTime = ctl.parts[ctl.currentPart].displayTime;
            return;
        }
    }
    subtitle_ctl* ctl = getBestFreeSubCtl();
    ctl->loadSubs(sub);
    ctl->audioCtl = audioCtl;
    ctl->x = ctl->parts[ctl->currentPart].x;
    ctl->y = layoutNewSubs(ctl->slotId, ctl->parts[ctl->currentPart].text[0]);
    ctl->currentPartRemainTime = ctl->parts[ctl->currentPart].displayTime;
}

void subtitle_draw::removeSubsWithAudioCtl(const void** audioCtl) {
    if (audioCtl == NULL) {
        return;
    }
    for (u32 i = 0; i < currentMaxSubs; i++) {
        if (subsCtl[i].audioCtl == audioCtl) {
            removeSubByIndex(i);
        }
    }
}

void subtitle_draw::removeSubsWithAudioPath(const char* const audioPath) {
    if (audioPath == NULL) {
        return;
    }
    const i32 audioPathHash = sdbmHash(audioPath);
    for (u32 i = 0; i < currentMaxSubs; i++) {
        if (subsCtl[i].audioPathHash == audioPathHash) {
            removeSubByIndex(i);
        }
    }
}

void subtitle_draw::removeSubByIndex(i32 index) {
    clearSubsLayout(subsCtl[index].slotId);
    subsCtl[index].clear();
}
