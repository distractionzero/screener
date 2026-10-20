package dev.codingcoffee.screen.rules

enum class SimpleMatch { STARTS_WITH, EXACTLY, CONTAINS_ANY }

data class SimpleRule(val match: SimpleMatch, val value: String)

private const val NUMBER_PREFIX = """^(\+?91|0)?"""
private val NumberPattern = Regex(
    "^" + Regex.escape(NUMBER_PREFIX) + """(\d+)(""" + Regex.escape("""\d*""") + ")?" + Regex.escape("$") + "$"
)
private val PlainWord = Regex("""[\p{L}\p{N} ]+""")

/** Strips formatting and the +91 / 0 prefix, which every generated number pattern already allows. */
fun simpleNumberDigits(input: String): String {
    val s = input.filter { it.isDigit() || it == '+' }
    val local = when {
        s.startsWith("+91") -> s.drop(3)
        s.startsWith("0") -> s.drop(1)
        else -> s
    }
    return local.filter { it.isDigit() }
}

fun simpleWords(input: String): List<String> = input.split(',').map { it.trim() }.filter { it.isNotEmpty() }

fun SimpleRule.isValid(): Boolean = when (match) {
    SimpleMatch.CONTAINS_ANY -> simpleWords(value).isNotEmpty()
    else -> simpleNumberDigits(value).isNotEmpty()
}

fun SimpleRule.toPattern(): String = when (match) {
    SimpleMatch.STARTS_WITH -> NUMBER_PREFIX + simpleNumberDigits(value) + """\d*$"""
    SimpleMatch.EXACTLY -> NUMBER_PREFIX + simpleNumberDigits(value) + "$"
    SimpleMatch.CONTAINS_ANY -> "(?i)" + simpleWords(value).joinToString("|") { escapeRegex(it) }
}

fun SimpleRule.describe(): String = when (match) {
    SimpleMatch.STARTS_WITH -> "Starts with ${simpleNumberDigits(value)}"
    SimpleMatch.EXACTLY -> "Is ${simpleNumberDigits(value)}"
    SimpleMatch.CONTAINS_ANY -> {
        val words = simpleWords(value)
        "Name contains " + if (words.size == 1) words[0] else words.dropLast(1).joinToString(", ") + " or " + words.last()
    }
}

fun parseSimple(type: RuleType, pattern: String): SimpleRule? = when (type) {
    RuleType.NUMBER -> NumberPattern.matchEntire(pattern)?.let { m ->
        SimpleRule(if (m.groupValues[2].isEmpty()) SimpleMatch.EXACTLY else SimpleMatch.STARTS_WITH, m.groupValues[1])
    }
    RuleType.CALLER_NAME -> pattern.removePrefix("(?i)").takeIf { pattern.startsWith("(?i)") }
        ?.split('|')
        ?.takeIf { words -> words.all { PlainWord.matches(it) && it.isNotBlank() } }
        ?.let { SimpleRule(SimpleMatch.CONTAINS_ANY, it.joinToString(", ")) }
}
