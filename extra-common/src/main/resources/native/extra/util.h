#pragma once

#include "platform.h"

bool compareAsciiCaseInsensitive(const char* s1, const char* s2);

char asciiToLower(char c);

char hexToB(char ch);

i32 sdbmHash(const char* text);
