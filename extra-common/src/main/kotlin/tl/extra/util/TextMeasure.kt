package tl.extra.util

class TextMeasure(
  val asciiSpacing: Int,
  private val warn: (String) -> Unit,
) {
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
            warn("Text measure: Unknown character width: $it, using default (in '$line')")
            0xF
          }
        }
      }
  }
}
