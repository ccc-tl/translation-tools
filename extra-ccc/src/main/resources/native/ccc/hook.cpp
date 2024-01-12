#include "hook.h"

const GameDrawTextDef GameDrawText = (GameDrawTextDef)(0x088E7C28);
const GameDrawFormattedTextDef GameDrawFormattedText = (GameDrawFormattedTextDef)(0x088EBBD4);
const GamePlayAt3AudioDef GamePlayAt3Audio = (GamePlayAt3AudioDef)(0x0891024C);

const GameMemcpyDef GameMemcpy = (GameMemcpyDef)(0x089999D4);
const GameMemcmpDef GameMemcmp = (GameMemcmpDef)(0x0898CE90);
const GameStrcmpDef GameStrcmp = (GameStrcmpDef)(0x0898D4A0);
const GameStrlenDef GameStrlen = (GameStrlenDef)(0x0898D518);
const GameStrcpyDef GameStrcpy = (GameStrcpyDef)(0x0898D4D4);

const u32* GameHeapFreeMemory = (u32*)(0x08A2E69C);
const u8* PatchConfigByte0 = (u8*)(0x08A1F34D);
