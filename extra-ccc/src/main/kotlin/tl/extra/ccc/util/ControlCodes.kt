package tl.extra.ccc.util

fun stripControlCodes(
  text: String,
  replacement: String,
): String {
  val joinGroups: (MatchResult) -> String = { result ->
    result.groups
      .drop(1)
      .joinToString(separator = replacement, transform = { it!!.value })
  }
  return text
    .replace("\\n", replacement)
    .replace("\n", replacement)
    .replace("""#C\d{8}#""".toRegex(), replacement)
    .replace("""#C\d{8,9}""".toRegex(), replacement)
    .replace("""#\[(.*?)]\[(.*?)]#""".toRegex(), joinGroups)
    .replace("#SP13", "～")
    .replace("#SP14", "～")
    .replace("#SP15", "～")
    .replace("#SP16", "～")
    .replace("##CDEF", replacement)
    .replace("#CDEF", replacement)
    .replace("#RUBS#", replacement)
    .replace("#RUBS", replacement)
    .replace("##RUBE", replacement)
    .replace("#RUBE", replacement)
    .replace("#REND", replacement)
    .replace("""#ROFS[-\d]\d{3}""".toRegex(), replacement)
    .replace("""#VAL\d{3}""".toRegex(), replacement)
    .replace("#FAMILY", replacement)
    .replace("#GIVEN", replacement)
    .replace("#NICK", replacement)
    .replace("""#SVT(\[.*?]){3,4}#""".toRegex(), joinGroups)
    .replace("#SVT", replacement)
    .replace("""#T\d""".toRegex(), replacement)
    .replace("""#SP\d{2}""".toRegex(), replacement)
    .replace("""#TITM\d{4}""".toRegex(), replacement)
    .replace("""#TVAL\d{2}""".toRegex(), replacement)
    .normalizeToAscii()
    .trim()
    // English only, not really control codes
    .replace("#1".toRegex(), replacement)
    .replace("#2".toRegex(), replacement)
    .replace("##".toRegex(), replacement)
}

fun replaceControlCodesForTextMeasure(
  text: String,
): String {
  if (text.contains("\n") || text.contains("\\n")) {
    error("When replacing control text for text measure input text can't have literal new lines and actual new lines")
  }
  val longestGroup: (MatchResult) -> String = { result ->
    result.groups
      .drop(1)
      .mapNotNull { it?.value }
      .maxByOrNull { it.length } ?: ""
  }
  return text
    .replace("#SP13", "～")
    .replace("#SP14", "～")
    .replace("#SP15", "～")
    .replace("#SP16", "～")
    .replace("""#C\d{8}#""".toRegex(), "")
    .replace("""#C\d{8,9}""".toRegex(), "")
    .replace("""#\[(.*?)]\[(.*?)]#""".toRegex(), longestGroup)
    .replace("#CDEF", "")
    .replace("#RUBS.*?#RUBE".toRegex(RegexOption.MULTILINE), "")
    .replace("#REND", "")
    .replace("""#ROFS[-\d]\d{3}""".toRegex(), "")
    .replace("""#VAL\d{3}""".toRegex(), "_V")
    .replace("#FAMILY", "LONG_FAMILY")
    .replace("#GIVEN", "LONG__GIVEN")
    .replace("#NICK", "LONG___NICK")
    .replace("""#SVT\[(.*?)]\[(.*?)]\[(.*?)]\[(.*?)]#""".toRegex(), longestGroup)
    .replace("""#SVT\[(.*?)]\[(.*?)]\[(.*?)]#""".toRegex(), longestGroup)
    .replace("#SVT", "LONG__SVT")
    .replace("""#T\d""".toRegex(), "_T__")
    .replace("""#SP\d{2}""".toRegex(), "_SP__")
    .replace("""#TITM\d{4}""".toRegex(), "TITM")
    .replace("""#TVAL\d{2}""".toRegex(), "TVAL")
    .normalizeToAscii()
    .trimEnd()
}

private fun String.normalizeToAscii(): String {
  return this
    .replace("’", "'")
    .replace("“", "\"")
    .replace("”", "\"")
}
