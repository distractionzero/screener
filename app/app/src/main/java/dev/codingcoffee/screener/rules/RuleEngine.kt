package dev.codingcoffee.screener.rules

enum class RuleType { NUMBER, CALLER_NAME }

enum class RuleAction { ALLOW, BLOCK }

data class Rule(
    val id: String,
    val name: String,
    val type: RuleType,
    val pattern: String,
    val action: RuleAction,
    val enabled: Boolean = true,
)

data class Decision(val block: Boolean, val reason: String)

const val REASON_NONE = "none"
const val REASON_UNKNOWN = "unknown caller"
const val REASON_SPAM = "marked as spam"

private val IndiaRules = listOf(
    Rule("default-140", "Telemarketing (140 series)", RuleType.NUMBER, """^\+91140\d*$""", RuleAction.BLOCK),
    Rule("default-160", "Service calls (160 series)", RuleType.NUMBER, """^\+91160\d*$""", RuleAction.ALLOW),
    Rule("default-8071", "Bangalore cloud telephony (8071)", RuleType.NUMBER, """^\+918071\d*$""", RuleAction.BLOCK),
    Rule("default-7935", "Ahmedabad cloud telephony (7935)", RuleType.NUMBER, """^\+917935\d*$""", RuleAction.BLOCK),
    Rule("default-7971", "Ahmedabad cloud telephony (7971)", RuleType.NUMBER, """^\+917971\d*$""", RuleAction.BLOCK),
)

private val SpamLabelRule =
    Rule("default-spam", "Carrier spam label", RuleType.CALLER_NAME, """(?i)spam|fraud|scam""", RuleAction.BLOCK)

val DefaultRules = IndiaRules + SpamLabelRule

fun defaultRules(region: String): List<Rule> = if (region == INDIA) DefaultRules else listOf(SpamLabelRule)

private val DefaultRuleIds = DefaultRules.map { it.id }.toSet()

val Rule.isDefault: Boolean get() = id in DefaultRuleIds

fun compilePattern(pattern: String): Result<Regex> = runCatching { Regex(pattern) }

fun escapeRegex(text: String): String =
    text.fold(StringBuilder()) { sb, c ->
        if (c in """\^$.|?*+()[]{}""") sb.append('\\')
        sb.append(c)
    }.toString()

fun Rule.matches(regex: Regex, number: String?, callerName: String?): Boolean {
    val subject = if (type == RuleType.NUMBER) number else callerName
    return subject != null && regex.containsMatchIn(subject)
}

class RuleEngine(
    rules: List<Rule>,
    private val blockUnknown: Boolean,
    private val spamNumbers: Set<String> = emptySet(),
) {
    private val compiled = rules
        .filter { it.enabled }
        .mapNotNull { rule -> compilePattern(rule.pattern).getOrNull()?.let { rule to it } }
    private val allow = compiled.filter { it.first.action == RuleAction.ALLOW }
    private val block = compiled.filter { it.first.action == RuleAction.BLOCK }

    fun evaluate(number: String?, callerName: String?, incoming: Boolean): Decision {
        if (!incoming) return Decision(block = false, reason = REASON_NONE)
        if (number != null && number in spamNumbers) return Decision(block = true, reason = REASON_SPAM)
        allow.firstOrNull { (rule, regex) -> rule.matches(regex, number, callerName) }
            ?.let { return Decision(block = false, reason = it.first.name) }
        block.firstOrNull { (rule, regex) -> rule.matches(regex, number, callerName) }
            ?.let { return Decision(block = true, reason = it.first.name) }
        if (blockUnknown) return Decision(block = true, reason = REASON_UNKNOWN)
        return Decision(block = false, reason = REASON_NONE)
    }
}
