#include "game.h"
#include "generated.h"
#include "hook.h"
#include "subs.h"
#include "subs_draw.h"
#include "util.h"

extern "C" {

bool audioDebug = false;  // debug render of current audio path
bool memoryDebug = false; // debug render of current game heap free memory
#define SUBS_DEBUG_LOOP 0 // this is for replaying audio, make sure to disable after
#define SUBS_DEBUG_OVERLAY 0
#define SUBS_UNDER_DEBUG "sound/bgm/bgm_bb_voice_02.at3"

static bool initDone = false;
// can be static but easier to debug if not (will generate entry in the symbol list)
subtitle_draw topDraw;
subtitle_draw bottomDraw;
subtitle_draw sgDraw;
subtitle_draw shopDraw;
u32 renderedSubs = 0;

#if SUBS_DEBUG_LOOP == 1
static const void** lastAudioCtl = NULL;
static bool waitReplay = false;
static u32 replayDelay;
#endif

#if MODE_CCC == 1 && !SUBS_DEBUG_LOOP == 1
static const char* cccBbLoopAudioPath1 = "sound/bgm/bgm_bb_voice_02.at3";
static const char* cccBbLoopAudioPath2 = "sound/bgm/bgm_bb_voice_03.at3";
static bool cccIsInBbLoop = false;
#endif

static const char* debugCurrentAudioPath = NULL;
static u32 debugDrawCurrentAudioPathRemainingTime = 0;

u8 extSgSubsTextAlpha = 0;
bool extInBattleMode = false;
u8 extInhibitSubsForFrames = 0;

bool isAudioDebug() {
    if (PatchConfigByte0 != NULL) {
        return audioDebug || (*PatchConfigByte0 & 1);
    }
    return audioDebug;
}

bool isMemoryDebug() {
    if (PatchConfigByte0 != NULL) {
        return memoryDebug || (*PatchConfigByte0 & 2);
    }
    return memoryDebug;
}

bool isSubtitlesHidden() {
    if (PatchConfigByte0 != NULL) {
        return (*PatchConfigByte0 & 4);
    }
    return false;
}

bool isCccBbChannelAltShadow() {
    if (PatchConfigByte0 != NULL) {
        return !(*PatchConfigByte0 & 8);
    }
    return true;
}

void initSubsSystem() {
    if (initDone) {
        return;
    }
    topDraw.clearAllSubs();
    topDraw.drawList = 1;
    topDraw.textAlpha = 0xFF;
    topDraw.baseSubY = 45;
    topDraw.spaceNewUpward = false;
    topDraw.currentMaxSubs = totalMaxSubs;

    bottomDraw.clearAllSubs();
    bottomDraw.drawList = 1;
    bottomDraw.textAlpha = 0xFF;
    bottomDraw.baseSubY = 280;
    bottomDraw.spaceNewUpward = true;
    bottomDraw.currentMaxSubs = totalMaxSubs;

    sgDraw.clearAllSubs();
    sgDraw.drawList = 4;
    sgDraw.textAlpha = 0xFF;
    // sgDraw.baseSubY = 28; // top
    sgDraw.baseSubY = 282; // bottom
    sgDraw.spaceNewUpward = true;
    sgDraw.currentMaxSubs = 1;

    shopDraw.clearAllSubs();
    shopDraw.drawList = 1;
    shopDraw.textAlpha = 0xFF;
    shopDraw.baseSubY = 50;
    shopDraw.spaceNewUpward = false;
    shopDraw.currentMaxSubs = 1;

    initDone = true;
}

const subtitle* getSubsForAudioPathHash(const i32 audioPathHash) {
    for (u32 i = 0; i < subsCount; i++) {
        if (subs[i].audioPathHash == audioPathHash) {
            return &subs[i];
        }
    }
    return NULL;
}

const subtitle* getSubsForAudioPath(const char* audioPath) {
    if (audioPath == NULL) {
        return NULL;
    }
    const i32 audioPathHash = sdbmHash(audioPath);
    return getSubsForAudioPathHash(audioPathHash);
}

void debugAudioHijack(const void** audioCtl) {
    GameStrcpy((char*)*audioCtl, SUBS_UNDER_DEBUG);
}

#if SUBS_DEBUG_OVERLAY == 1
void subsDrawDebugInfo() {
    char tmpBuf[32];
    const subtitle* sub = getSubsForAudioPath(SUBS_UNDER_DEBUG);
    const int pos = 5;
    if (sub == NULL) {
        gameDrawText("debug: null sub", pos, pos);
        return;
    }
    u32 addr = (u32)&sub->parts[0];
    itox(addr + 8, tmpBuf);
    gameDrawText(tmpBuf, pos, pos + lineHeight);

    i32 partY = pos + lineHeight * 2;
    for (u32 i = 0; i < sub->partsCount; i++) {
        itoa(sub->parts[i].displayTime, tmpBuf);
        gameDrawText(tmpBuf, pos, partY);
        partY += lineHeight;
    }

    itoa(renderedSubs, tmpBuf);
    gameDrawText(tmpBuf, pos, partY);
    partY += lineHeight;

#if SUBS_DEBUG_LOOP == 1
    itoa(replayDelay, tmpBuf);
    gameDrawText(tmpBuf, pos, partY);
    partY += lineHeight;
#endif
}
#endif

void scheduleSubsToDraw(const subtitle* sub, const void** audioCtl) {
    topDraw.removeSubsWithAudioCtl(audioCtl);
    bottomDraw.removeSubsWithAudioCtl(audioCtl);
    sgDraw.removeSubsWithAudioCtl(audioCtl);
    shopDraw.removeSubsWithAudioCtl(audioCtl);
    if (sub == NULL || isSubtitlesHidden()) {
        return;
    }
    if (sub->dispatchMode == 1 && extInBattleMode == false) {
        return;
    }
    if (sub->displayMode == 1 && sub->dispatchMode == 0 && extInhibitSubsForFrames != 0) {
        return;
    }
    if (sub->displayMode == 0) {
        bottomDraw.subsScheduleSubtitles(sub, audioCtl);
    } else if (sub->displayMode == 1) {
        topDraw.subsScheduleSubtitles(sub, audioCtl);
    } else if (sub->displayMode == 2) {
        sgDraw.subsScheduleSubtitles(sub, audioCtl);
    } else if (sub->displayMode == 3) {
        shopDraw.subsScheduleSubtitles(sub, audioCtl);
    }
}

#if SUBS_DEBUG_LOOP == 1
void subsLoopDebug() {
    if (waitReplay) {
        replayDelay--;
        if (replayDelay == 0) {
            waitReplay = false;
            const subtitle* sub = getSubsForAudioPath(SUBS_UNDER_DEBUG);
            if (sub != NULL) {
                GamePlayAt3Audio(lastAudioCtl);
                topDraw.removeSubsWithAudioCtl(lastAudioCtl);
                topDraw.subsScheduleSubtitles(sub, lastAudioCtl);
            }
        }
    }
}
#endif

void tickSubsDraw(subtitle_draw* draw) {
    if (extInhibitSubsForFrames > 0) {
        extInhibitSubsForFrames--;
    }
    for (u32 i = 0; i < draw->currentMaxSubs; i++) {
        subtitle_ctl& ctl = draw->subsCtl[i];
        if (ctl.parts == NULL) {
            continue;
        }
#if MODE_CCC == 1 && !SUBS_DEBUG_LOOP == 1
        const void** lastAudioCtl = ctl.audioCtl;
        const i32 lastAudioPathHash = ctl.audioPathHash;
#endif
        bool finished = draw->subsTickPartTime(&ctl);
        if (finished) {
#if MODE_CCC == 1 && !SUBS_DEBUG_LOOP == 1
            if (lastAudioPathHash == sdbmHash(cccBbLoopAudioPath1) || lastAudioPathHash == sdbmHash(cccBbLoopAudioPath2)) {
                cccIsInBbLoop = true;
                const subtitle* loopSub = getSubsForAudioPathHash(lastAudioPathHash);
                scheduleSubsToDraw(loopSub, lastAudioCtl);
            }
#endif
#if SUBS_DEBUG_LOOP == 1
            waitReplay = true;
            replayDelay = 50;
#endif
            continue;
        }
        gameDrawCenteredText(ctl.parts[ctl.currentPart].text + 1, ctl.x, ctl.y, draw->drawList, draw->textAlpha);
        renderedSubs++;
    }
}

// Main entry points from ASM dispatchers

void subsRenderer() {
    if (!initDone) {
        initSubsSystem();
    }

#if SUBS_DEBUG_LOOP == 1
    subsLoopDebug();
#endif
#if SUBS_DEBUG_OVERLAY == 1
    subsDrawDebugInfo();
#endif

    if (isAudioDebug() && debugCurrentAudioPath != NULL) {
        gameDrawText(debugCurrentAudioPath, 4, 264);
        debugDrawCurrentAudioPathRemainingTime--;
        if (debugDrawCurrentAudioPathRemainingTime == 0) {
            debugCurrentAudioPath = NULL;
        }
    }

    if (isMemoryDebug()) {
        char buf[16];
        itoa(*GameHeapFreeMemory / 1024, buf);
        gameDrawText(buf, 440, 264);
    }

    renderedSubs = 0;
    sgDraw.textAlpha = extSgSubsTextAlpha;
    tickSubsDraw(&bottomDraw);
    tickSubsDraw(&topDraw);
    tickSubsDraw(&sgDraw);
    tickSubsDraw(&shopDraw);
}

void subsSoundPlaybackInterceptor(const void** audioCtl) {
    if (!initDone) initSubsSystem();
    if (audioCtl == NULL) {
        return;
    }
#if SUBS_DEBUG_LOOP == 1
    lastAudioCtl = audioCtl;
    debugAudioHijack(audioCtl);
#endif
#if MODE_CCC == 1 && !SUBS_DEBUG_LOOP == 1
    if (cccIsInBbLoop) {
        topDraw.removeSubsWithAudioPath(cccBbLoopAudioPath1);
        topDraw.removeSubsWithAudioPath(cccBbLoopAudioPath2);
        cccIsInBbLoop = false;
    }
#endif
    const char* audioPath = (const char*)*audioCtl;
    debugCurrentAudioPath = audioPath;
    debugDrawCurrentAudioPathRemainingTime = 30 * 10;
    const subtitle* sub = getSubsForAudioPath(audioPath);
    scheduleSubsToDraw(sub, audioCtl);
}

void subsSoundEffectPlaybackInterceptor(const void* ignore, const int bankId, const int unk, const int soundId) {
    UNUSED(ignore);
    UNUSED(unk);
    if (!initDone) {
        initSubsSystem();
    }
    char buf[16];
    itoa(bankId, buf);
    u32 len = GameStrlen(buf);
    buf[len] = '.';
    itoa(soundId, buf + len + 1);
    const subtitle* sub = getSubsForAudioPath(buf);
    scheduleSubsToDraw(sub, NULL); // multiple sound effects can be playing at once so don't remove based on current bank id
}

const char* textRemapper(const char* text) {
    if (text == NULL) {
        return NULL;
    }
    const u32 prefixLength = 5;
    const bool remap = GameMemcmp("!rmap>", text, prefixLength) == 0;
    if (remap) {
        u32 addr = 0;
        for (u32 i = prefixLength + 2; i < prefixLength + 2 + sizeof(u32) * 2; i++) {
            addr = (addr << 4) | (hexToB(text[i]) & 0xF);
        }
        if (text[prefixLength + 1] == '!') {
            return (const char*)addr;
        } else if (text[prefixLength + 1] == '@') {
            return text + addr;
        } else if (text[prefixLength + 1] == '#') {
            return (const char*)(remappedTextData + addr);
        }
    }
    return text;
}

float cccCalcBasicTextWidth(const u8* text, u8 extraWidth) {
    u8 index = 0;
    float width = 0;
    while (true) {
        const u8 b1 = text[index++];
        if (b1 == 0) {
            break;
        }
        if (b1 > 0x7F) {
            width += 15;
            const u8 b2 = text[index++];
            if (b2 == 0) {
                break;
            }
        } else {
            width += 7;
        }
    }
    return width + extraWidth;
}

} // extern "C"
