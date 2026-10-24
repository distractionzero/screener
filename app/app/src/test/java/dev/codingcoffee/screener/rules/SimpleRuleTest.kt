package dev.codingcoffee.screener.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleRuleTest {

    private fun SimpleRule.matches(subject: String, region: String = INDIA) =
        Regex(toPattern(region)).containsMatchIn(normalizeNumber(subject, region)!!)

    @Test
    fun `starts with matches with or without country code and trunk prefix`() {
        val rule = SimpleRule(SimpleMatch.STARTS_WITH, "140")
        listOf("1409806457", "+911409806457", "01409806457").forEach {
            assertTrue(it, rule.matches(it))
        }
        assertFalse(rule.matches("+919140000000"))
        assertFalse(rule.matches("1601234567"))
    }

    @Test
    fun `starts with works for us numbers`() {
        val rule = SimpleRule(SimpleMatch.STARTS_WITH, "212")
        listOf("(212) 555-0123", "+1 212 555 0123", "12125550123").forEach {
            assertTrue(it, rule.matches(it, "US"))
        }
        assertFalse(rule.matches("(415) 555-0123", "US"))
        assertEquals(simpleNumberDigits("800", "US"), simpleNumberDigits("1 800", "US"))
    }

    @Test
    fun `a leading plus targets another country`() {
        val rule = SimpleRule(SimpleMatch.STARTS_WITH, "+44 20")
        assertTrue(rule.matches("+442079460958"))
        assertFalse(rule.matches("2079460958"))
    }

    @Test
    fun `typed prefix and formatting are stripped`() {
        assertEquals("919876543210", simpleNumberDigits("+91 98765-43210", INDIA))
        assertEquals("918071", simpleNumberDigits("08071", INDIA))
        assertEquals("1212", simpleNumberDigits("(212)", "US"))
        assertEquals("39061234", simpleNumberDigits("06 1234", "IT"))
        assertEquals("", simpleNumberDigits("140", UNKNOWN_REGION))
        val rule = SimpleRule(SimpleMatch.EXACTLY, "+91 98765 43210")
        assertTrue(rule.matches("+919876543210"))
        assertTrue(rule.matches("09876543210"))
        assertFalse(rule.matches("+9198765432101"))
    }

    @Test
    fun `contains any is case insensitive and escaped`() {
        val rule = SimpleRule(SimpleMatch.CONTAINS_ANY, "spam, Fraud , a.b")
        assertEquals("(?i)spam|Fraud|a\\.b", rule.toPattern(INDIA))
        assertTrue(Regex(rule.toPattern(INDIA)).containsMatchIn("Spam Alert"))
        assertTrue(Regex(rule.toPattern(INDIA)).containsMatchIn("FRAUD risk"))
        assertFalse(Regex(rule.toPattern(INDIA)).containsMatchIn("axb"))
    }

    @Test
    fun `generated patterns parse back`() {
        listOf(
            Triple(INDIA, RuleType.NUMBER, SimpleRule(SimpleMatch.STARTS_WITH, "140")),
            Triple(INDIA, RuleType.NUMBER, SimpleRule(SimpleMatch.EXACTLY, "9876543210")),
            Triple(INDIA, RuleType.NUMBER, SimpleRule(SimpleMatch.STARTS_WITH, "+4420")),
            Triple("US", RuleType.NUMBER, SimpleRule(SimpleMatch.STARTS_WITH, "212")),
            Triple(INDIA, RuleType.CALLER_NAME, SimpleRule(SimpleMatch.CONTAINS_ANY, "spam, fraud")),
        ).forEach { (region, type, rule) -> assertEquals(rule, parseSimple(type, rule.toPattern(region), region)) }
    }

    @Test
    fun `describe shows home numbers without the country code`() {
        assertEquals("Starts with 140", SimpleRule(SimpleMatch.STARTS_WITH, "0140").describe(INDIA))
        assertEquals("Is +442079460958", SimpleRule(SimpleMatch.EXACTLY, "+44 20 7946 0958").describe(INDIA))
        assertEquals("Starts with 212", SimpleRule(SimpleMatch.STARTS_WITH, "1 212").describe("US"))
    }

    @Test
    fun `legacy india patterns still parse and match`() {
        val legacy = """^(\+?91|0)?140\d*$"""
        assertEquals(SimpleRule(SimpleMatch.STARTS_WITH, "140"), parseSimple(RuleType.NUMBER, legacy, INDIA))
        assertEquals(SimpleRule(SimpleMatch.STARTS_WITH, "+91140"), parseSimple(RuleType.NUMBER, legacy, "US"))
        assertTrue(Regex(legacy).containsMatchIn(normalizeNumber("01409806457", INDIA)!!))
    }

    @Test
    fun `default rules are shown as simple rules`() {
        DefaultRules.forEach { assertTrue(it.name, parseSimple(it.type, it.pattern, INDIA) != null) }
        assertEquals(
            SimpleRule(SimpleMatch.CONTAINS_ANY, "spam, fraud, scam"),
            parseSimple(RuleType.CALLER_NAME, """(?i)spam|fraud|scam""", INDIA),
        )
    }

    @Test
    fun `hand written regexes stay advanced`() {
        assertNull(parseSimple(RuleType.NUMBER, "^140", INDIA))
        assertNull(parseSimple(RuleType.NUMBER, """^(\+?91|0)?(140|141)\d*$""", INDIA))
        assertNull(parseSimple(RuleType.CALLER_NAME, "(?i)sp.m", INDIA))
        assertNull(parseSimple(RuleType.CALLER_NAME, "spam", INDIA))
    }
}
