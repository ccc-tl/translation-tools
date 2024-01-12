#ifndef HOOK_H_
#define HOOK_H_

#include "platform.h"

typedef u32 (*GameDrawTextDef)(u32 unkA0, u32 unkA1, const char* text, u32 unkA3, u32 colorABGR8888, u32 unkT1, u32 unkT2,
                               u32 unkT3, u32 unkSP0, u32 unkSP4, u32 unkSP8, float x, float y, float sx, float sy);
extern const GameDrawTextDef GameDrawText;

typedef u32 (*GameDrawFormattedTextDef)(u32 shiftJisSpacing, u32 lineHeight, u32 unkA2, u32 unkA3, u32 unkT0, u32 lineWidth,
                                        const char* text, u32 unkFlagsT3, u32 unkBaseColorSP0, u32 unkSP4, u32 unkSP8, u32 unkSPC,
                                        u32 unkSP10, u32 unkSP14, u32 unkSP18, float x, float y, float sx, float sy);
extern const GameDrawFormattedTextDef GameDrawFormattedText;

typedef void (*GamePlayAt3AudioDef)(const void** audioCtl);
extern const GamePlayAt3AudioDef GamePlayAt3Audio;

typedef void* (*GameMemcpyDef)(void* dest, const void* src, u32 size);
extern const GameMemcpyDef GameMemcpy;

typedef i32 (*GameMemcmpDef)(const void* str1, const void* str2, u32 size);
extern const GameMemcmpDef GameMemcmp;

typedef i32 (*GameStrcmpDef)(const char* str, const char* str2);
extern const GameStrcmpDef GameStrcmp;

typedef u32 (*GameStrlenDef)(const char* str);
extern const GameStrlenDef GameStrlen;

typedef char* (*GameStrcpyDef)(char* dest, const char* src);
extern const GameStrcpyDef GameStrcpy;

extern const u32* GameHeapFreeMemory;
extern const u8* PatchConfigByte0;

#endif
