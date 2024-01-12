#include "util.h"
#include "platform.h"

bool compareAsciiCaseInsensitive(const char* s1, const char* s2) {
    if (s1 == NULL || s2 == NULL) {
        return false;
    }
    while (true) {
        if (*s1 == 0 && *s2 == 0) {
            return true;
        }
        if (*s1 == 0 || *s2 == 0) {
            return false;
        }
        char c1 = asciiToLower(*s1);
        char c2 = asciiToLower(*s2);
        if (c1 != c2) {
            return false;
        }
        s1++;
        s2++;
    }
}

char asciiToLower(char c) {
    if (c >= 'A' && c <= 'Z') {
        return c + 0x20;
    }
    return c;
}

char hexToB(char ch) {
    if (ch >= '0' && ch <= '9') return ch - '0';
    if (ch >= 'A' && ch <= 'F') return ch - 'A' + 10;
    if (ch >= 'a' && ch <= 'f') return ch - 'a' + 10;
    return 0;
}

i32 sdbmHash(const char* text) {
  i32 hash = 0;
  for(int i = 0; text[i] != 0; i++) {
    hash = asciiToLower(text[i]) + (hash << 6) + (hash << 16) - hash;
  }
  return hash;
}
