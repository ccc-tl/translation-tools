#include "hook.h"
#include "patch.h"
#include "platform.h"

extern "C" {

static u8 cccBbDropShadowCounter = 0;

void cccBbDropShadowResetCounter(u32 shiftJisSpacing, u32 lineHeight, u32 unkA2, u32 unkA3, u32 unkT0, u32 lineWidth,
                                 const char* text, u32 unkFlagsT3, u32 unkBaseColorSP0, u32 unkSP4, u32 unkSP8, u32 unkSPC,
                                 u32 unkSP10, u32 unkSP14, u32 unkSP18, float x, float y, float sx, float sy) {
    cccBbDropShadowCounter = 0;
    GameDrawFormattedText(shiftJisSpacing, lineHeight, unkA2, unkA3, unkT0, lineWidth, text, unkFlagsT3, unkBaseColorSP0,
                          unkSP4, unkSP8, unkSPC, unkSP10, unkSP14, unkSP18, x, y, sx, sy);
}

void cccBbDropShadowDraw(u32 shiftJisSpacing, u32 lineHeight, u32 unkA2, u32 unkA3, u32 unkT0, u32 lineWidth,
                         const char* text, u32 unkFlagsT3, u32 unkBaseColorSP0, u32 unkSP4, u32 unkSP8, u32 unkSPC,
                         u32 unkSP10, u32 unkSP14, u32 unkSP18, float x, float y, float sx, float sy) {
    if (isCccBbChannelAltShadow()) {
        cccBbDropShadowCounter++;
        if (cccBbDropShadowCounter == 1) {
            // offset X: -2px, offset Y: -2px
            x += 2 + 1;
            y += 2 + 1;
        } else if (cccBbDropShadowCounter == 2) {
            // offset X: 0px, offset Y: -2px
            y += 2 + 1;
        } else if (cccBbDropShadowCounter == 3) {
            // offset X: 2px, offset Y: -2px
            x += -2 + 1;
            y += 2;
        } else {
            return;
        }
        GameDrawFormattedText(shiftJisSpacing, lineHeight, unkA2, unkA3, unkT0, lineWidth, text, unkFlagsT3, unkBaseColorSP0,
                              unkSP4, unkSP8, unkSPC, unkSP10, unkSP14, unkSP18, x, y, sx, sy);
    } else {
        GameDrawFormattedText(shiftJisSpacing, lineHeight, unkA2, unkA3, unkT0, lineWidth, text, unkFlagsT3, unkBaseColorSP0,
                              unkSP4, unkSP8, unkSPC, unkSP10, unkSP14, unkSP18, x, y, sx, sy);
    }
}

} // extern "C"
