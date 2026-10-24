package dev.codingcoffee.screener.rules

enum class SimpleMatch { STARTS_WITH, EXACTLY, CONTAINS_ANY }

data class SimpleRule(val match: SimpleMatch, val value: String)

private fun numberPattern(prefix: String) = Regex(
    "^" + Regex.escape(prefix) + """(\d+)(""" + Regex.escape("""\d*""") + ")?" + Regex.escape("$") + "$"
)
private val NumberPattern = numberPattern("""^\+""")
private val LegacyIndiaPattern = numberPattern("""^(\+?91|0)?""")
private val PlainWord = Regex("""[\p{L}\p{N} ]+""")

/** Digits with the country code and without the +, e.g. "91140" for "140" typed in India. A leading + skips the home country. */
fun simpleNumberDigits(input: String, region: String): String {
    val s = input.filter { it.isDigit() || it == '+' }
    val digits = s.filter { it.isDigit() }
    if (s.startsWith("+")) return digits
    val code = callingCode(region).takeIf { it != 0 } ?: return ""
    val trunk = trunkPrefix(region)
    val national = if (trunk != null && digits.startsWith(trunk)) digits.drop(trunk.length) else digits
    return if (national.isEmpty()) "" else "$code$national"
}

/** The inverse of [simpleNumberDigits]: home country numbers without the country code, others with a +. */
fun displayNumberDigits(digits: String, region: String): String {
    val code = callingCode(region).toString()
    if (code == "0" || !digits.startsWith(code) || digits.length == code.length) return "+$digits"
    val national = digits.drop(code.length)
    val trunk = trunkPrefix(region)
    return if (trunk != null && national.startsWith(trunk)) trunk + national else national
}

fun simpleWords(input: String): List<String> = input.split(',').map { it.trim() }.filter { it.isNotEmpty() }

fun SimpleRule.isValid(region: String): Boolean = when (match) {
    SimpleMatch.CONTAINS_ANY -> simpleWords(value).isNotEmpty()
    else -> simpleNumberDigits(value, region).isNotEmpty()
}

fun SimpleRule.toPattern(region: String): String = when (match) {
    SimpleMatch.STARTS_WITH -> """^\+""" + simpleNumberDigits(value, region) + """\d*$"""
    SimpleMatch.EXACTLY -> """^\+""" + simpleNumberDigits(value, region) + "$"
    SimpleMatch.CONTAINS_ANY -> "(?i)" + simpleWords(value).joinToString("|") { escapeRegex(it) }
}

fun SimpleRule.describe(region: String): String = when (match) {
    SimpleMatch.STARTS_WITH -> "Starts with ${displayNumberDigits(simpleNumberDigits(value, region), region)}"
    SimpleMatch.EXACTLY -> "Is ${displayNumberDigits(simpleNumberDigits(value, region), region)}"
    SimpleMatch.CONTAINS_ANY -> {
        val words = simpleWords(value)
        "Name contains " + if (words.size == 1) words[0] else words.dropLast(1).joinToString(", ") + " or " + words.last()
    }
}

fun parseSimple(type: RuleType, pattern: String, region: String): SimpleRule? = when (type) {
    RuleType.NUMBER -> {
        val current = NumberPattern.matchEntire(pattern)
        val legacy = if (current == null) LegacyIndiaPattern.matchEntire(pattern) else null
        (current ?: legacy)?.let { m ->
            val digits = (if (legacy != null) "91" else "") + m.groupValues[1]
            val match = if (m.groupValues[2].isEmpty()) SimpleMatch.EXACTLY else SimpleMatch.STARTS_WITH
            SimpleRule(match, displayNumberDigits(digits, region))
        }
    }
    RuleType.CALLER_NAME -> pattern.removePrefix("(?i)").takeIf { pattern.startsWith("(?i)") }
        ?.split('|')
        ?.takeIf { words -> words.all { PlainWord.matches(it) && it.isNotBlank() } }
        ?.let { SimpleRule(SimpleMatch.CONTAINS_ANY, it.joinToString(", ")) }
}
