#pragma once

#include "platform.h"

void gameDrawText(const char* text, float x, float y);
void gameDrawTextDropShadow(const char* text, float x, float y);
void gameDrawCenteredText(const char* text, float x, float y, int drawList, u8 textAlpha);
