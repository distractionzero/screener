package dev.codingcoffee.screener.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private fun rule(
        name: String,
        pattern: String,
        action: RuleAction,
        type: RuleType = RuleType.NUMBER,
        enabled: Boolean = true,
    ) = Rule(name, name, type, pattern, action, enabled)

    @Test
    fun `normalize strips spaces dashes and parentheses`() {
        assertEquals("+919876543210", normalizeNumber("+91 98765-43210"))
        assertEquals("08012345678", normalizeNumber("(080) 1234 5678"))
        assertEquals("1409806457", normalizeNumber("1409806457"))
        assertNull(normalizeNumber(null))
    }

    @Test
    fun `allow beats block`() {
        val engine = RuleEngine(
            listOf(rule("block all", ".*", RuleAction.BLOCK), rule("friend", "^\\+9198", RuleAction.ALLOW)),
            blockUnknown = false,
        )
        assertEquals(Decision(false, "friend"), engine.evaluate("+919876543210", null, incoming = true))
        assertEquals(Decision(true, "block all"), engine.evaluate("+917012345678", null, incoming = true))
    }

    @Test
    fun `allow beats block unknown`() {
        val engine = RuleEngine(listOf(rule("friend", "^\\+9198", RuleAction.ALLOW)), blockUnknown = true)
        assertFalse(engine.evaluate("+919876543210", null, incoming = true).block)
        assertEquals(Decision(true, REASON_UNKNOWN), engine.evaluate("+917012345678", null, incoming = true))
    }

    @Test
    fun `invalid regex is skipped`() {
        val engine = RuleEngine(
            listOf(rule("broken", "(unclosed", RuleAction.ALLOW), rule("140", "^140", RuleAction.BLOCK)),
            blockUnknown = false,
        )
        assertEquals(Decision(true, "140"), engine.evaluate("1409806457", null, incoming = true))
    }

    @Test
    fun `outgoing calls are never blocked`() {
        val engine = RuleEngine(listOf(rule("all", ".*", RuleAction.BLOCK)), blockUnknown = true)
        assertEquals(Decision(false, REASON_NONE), engine.evaluate("1409806457", null, incoming = false))
    }

    @Test
    fun `160 series is allowed even with a broad block rule`() {
        val engine = RuleEngine(DefaultRules + rule("everything", ".*", RuleAction.BLOCK), blockUnknown = true)
        listOf("1601234567", "+911601234567", "911601234567", "01601234567").forEach {
            assertEquals(it, Decision(false, "Service calls (160 series)"), engine.evaluate(it, null, incoming = true))
        }
    }

    @Test
    fun `default rules block 140, cloud telephony and carrier spam label`() {
        val engine = RuleEngine(DefaultRules, blockUnknown = false)
        assertEquals(Decision(true, "Telemarketing (140 series)"), engine.evaluate("+911409806457", null, true))
        assertEquals(Decision(true, "Carrier spam label"), engine.evaluate("+917012345678", "Spam Alert", true))
        assertEquals(Decision(false, REASON_NONE), engine.evaluate("+917012345678", "Asha", true))
        assertEquals(Decision(true, "Bangalore cloud telephony (8071)"), engine.evaluate("+918071234567", null, true))
        assertEquals(Decision(true, "Ahmedabad cloud telephony (7935)"), engine.evaluate("07935123456", null, true))
        assertEquals(Decision(true, "Ahmedabad cloud telephony (7971)"), engine.evaluate("+917971234567", null, true))
    }

    @Test
    fun `spam numbers are blocked regardless of prefix and allow rules`() {
        val engine = RuleEngine(
            listOf(rule("allow all", ".*", RuleAction.ALLOW)),
            blockUnknown = false,
            spamNumbers = setOf(spamKey("+919876543210")),
        )
        listOf("+919876543210", "09876543210", "9876543210").forEach {
            assertEquals(it, Decision(true, REASON_SPAM), engine.evaluate(it, null, incoming = true))
        }
        assertFalse(engine.evaluate("+919876543211", null, incoming = true).block)
    }

    @Test
    fun `disabled rules are ignored`() {
        val engine = RuleEngine(listOf(rule("off", ".*", RuleAction.BLOCK, enabled = false)), blockUnknown = false)
        assertFalse(engine.evaluate("1409806457", null, incoming = true).block)
    }

    @Test
    fun `name rules do not match a missing caller name`() {
        val engine = RuleEngine(listOf(rule("any name", ".*", RuleAction.BLOCK, RuleType.CALLER_NAME)), false)
        assertFalse(engine.evaluate("1409806457", null, incoming = true).block)
    }

    @Test
    fun `escaped number matches itself exactly`() {
        val pattern = "^" + escapeRegex("+911409806457") + "$"
        assertEquals("^\\+911409806457$", pattern)
        val regex = Regex(pattern)
        assertTrue(regex.containsMatchIn("+911409806457"))
        assertFalse(regex.containsMatchIn("911409806457"))
    }
}
