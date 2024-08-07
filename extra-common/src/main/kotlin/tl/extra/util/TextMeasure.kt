package tl.extra.util

class TextMeasure(
  val asciiSpacing: Int,
  private val warn: (String) -> Unit,
) {
  private val unknownChars = mutableSetOf<Char>()

  fun measureInGameText(line: String): Int {
    return line
      .sumOf {
        when (it) {
          in ' '..'~' -> {
            asciiSpacing
          }
          // see CccTranslation for remapping config
          in (0x7..0x1F).map { code -> code.toChar() } -> {
            asciiSpacing
          }
          '　', '—', '～', '…', 'お', '―', '鷙', '薗', '☆', '♪', '⑥', '□', '△', '○', '×', '人', '◆', '→', '「', '」', '※' -> {
            0xF
          }
          else -> {
            if (it !in unknownChars) {
              unknownChars.add(it)
              warn("Text measure: Unknown character width: $it, using default (in '$line')")
            }
            0xF
          }
        }
      }
  }
}
