package dev.codingcoffee.screener.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleRuleTest {

    private fun SimpleRule.matches(subject: String) = Regex(toPattern()).containsMatchIn(subject)

    @Test
    fun `starts with matches with or without country code and trunk prefix`() {
        val rule = SimpleRule(SimpleMatch.STARTS_WITH, "140")
        listOf("1409806457", "+911409806457", "911409806457", "01409806457").forEach {
            assertTrue(it, rule.matches(it))
        }
        assertFalse(rule.matches("+919140000000"))
        assertFalse(rule.matches("1601234567"))
    }

    @Test
    fun `typed prefix and formatting are stripped`() {
        assertEquals("9876543210", simpleNumberDigits("+91 98765-43210"))
        assertEquals("8071", simpleNumberDigits("08071"))
        val rule = SimpleRule(SimpleMatch.EXACTLY, "+91 98765 43210")
        assertTrue(rule.matches("+919876543210"))
        assertTrue(rule.matches("09876543210"))
        assertFalse(rule.matches("+9198765432101"))
    }

    @Test
    fun `contains any is case insensitive and escaped`() {
        val rule = SimpleRule(SimpleMatch.CONTAINS_ANY, "spam, Fraud , a.b")
        assertEquals("(?i)spam|Fraud|a\\.b", rule.toPattern())
        assertTrue(rule.matches("Spam Alert"))
        assertTrue(rule.matches("FRAUD risk"))
        assertFalse(rule.matches("axb"))
    }

    @Test
    fun `generated patterns parse back`() {
        listOf(
            RuleType.NUMBER to SimpleRule(SimpleMatch.STARTS_WITH, "140"),
            RuleType.NUMBER to SimpleRule(SimpleMatch.EXACTLY, "9876543210"),
            RuleType.CALLER_NAME to SimpleRule(SimpleMatch.CONTAINS_ANY, "spam, fraud"),
        ).forEach { (type, rule) -> assertEquals(rule, parseSimple(type, rule.toPattern())) }
    }

    @Test
    fun `default rules are shown as simple rules`() {
        DefaultRules.forEach { assertTrue(it.name, parseSimple(it.type, it.pattern) != null) }
        assertEquals(
            SimpleRule(SimpleMatch.CONTAINS_ANY, "spam, fraud, scam"),
            parseSimple(RuleType.CALLER_NAME, """(?i)spam|fraud|scam"""),
        )
    }

    @Test
    fun `hand written regexes stay advanced`() {
        assertNull(parseSimple(RuleType.NUMBER, "^140"))
        assertNull(parseSimple(RuleType.NUMBER, """^(\+?91|0)?(140|141)\d*$"""))
        assertNull(parseSimple(RuleType.CALLER_NAME, "(?i)sp.m"))
        assertNull(parseSimple(RuleType.CALLER_NAME, "spam"))
    }
}
