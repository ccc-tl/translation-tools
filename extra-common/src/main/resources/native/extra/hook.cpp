#include "hook.h"

const GameDrawTextDef GameDrawText = (GameDrawTextDef)(0x088B584C);
const GameDrawFormattedTextDef GameDrawFormattedText = (GameDrawFormattedTextDef)(0x088B99D0);
const GamePlayAt3AudioDef GamePlayAt3Audio = (GamePlayAt3AudioDef)(0x088D69DC);

const GameMemcpyDef GameMemcpy = (GameMemcpyDef)(0x0893CAE4);
const GameMemcmpDef GameMemcmp = (GameMemcmpDef)(0x089176FC);
const GameStrcmpDef GameStrcmp = (GameStrcmpDef)(0x08917918);
const GameStrlenDef GameStrlen = (GameStrlenDef)(0x08917990);
const GameStrcpyDef GameStrcpy = (GameStrcpyDef)(0x0891794C);

const u32* GameHeapFreeMemory = (u32*)(0x0897B81C);
const u8* PatchConfigByte0 = (u8*)(0x08949AC7);
