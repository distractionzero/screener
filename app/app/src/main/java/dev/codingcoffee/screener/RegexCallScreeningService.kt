package dev.codingcoffee.screener

import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import dev.codingcoffee.screener.rules.Decision
import dev.codingcoffee.screener.rules.REASON_NONE
import dev.codingcoffee.screener.rules.RuleEngine
import dev.codingcoffee.screener.rules.normalizeNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID

class RegexCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val timestamp = System.currentTimeMillis()
        val app = application as ScreenApp
        val rawNumber = runCatching { callDetails.handle?.schemeSpecificPart }.getOrNull()
        val callerName = runCatching { callDetails.callerDisplayName }.getOrNull()
        val direction = runCatching { callDetails.callDirection }.getOrDefault(Call.Details.DIRECTION_UNKNOWN)
        val incoming = direction == Call.Details.DIRECTION_INCOMING

        val engine = awaitEngine(app)
        val number = normalizeNumber(rawNumber, app.settings.value?.region)
        val decision = try {
            engine?.evaluate(number, callerName, incoming)
        } catch (e: Exception) {
            Log.e(ScreenApp.TAG, "Rule evaluation failed, allowing call", e)
            null
        } ?: Decision(block = false, reason = REASON_NONE)

        val response = if (decision.block) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .setSkipCallLog(false)
                .build()
        } else {
            CallResponse.Builder().build()
        }
        respondToCall(callDetails, response)
        if (direction == Call.Details.DIRECTION_OUTGOING) return

        app.scope.launch(Dispatchers.IO) {
            runCatching { app.callLog.append(logEntry(callDetails, timestamp, rawNumber, number, decision)) }
                .onFailure { Log.e(ScreenApp.TAG, "Failed to write call log", it) }
        }
    }

    // Settings load in a few ms on a cold start; Telecom gives us ~5s, so wait at most 2s.
    private fun awaitEngine(app: ScreenApp): RuleEngine? =
        app.engine.value ?: runBlocking { withTimeoutOrNull(2_000) { app.engine.filterNotNull().first() } }

    private fun logEntry(
        details: Call.Details,
        timestamp: Long,
        rawNumber: String?,
        number: String?,
        decision: Decision,
    ): JSONObject = JSONObject().apply {
        put("id", UUID.randomUUID().toString())
        put("timestamp", timestamp)
        put("rawNumber", rawNumber ?: JSONObject.NULL)
        put("number", number ?: JSONObject.NULL)
        safePut("handlePresentation") { details.handlePresentation }
        safePut("callerDisplayName") { details.callerDisplayName }
        safePut("callerDisplayNamePresentation") { details.callerDisplayNamePresentation }
        safePut("callDirection") { details.callDirection }
        safePut("callerNumberVerificationStatus") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) details.callerNumberVerificationStatus else null
        }
        safePut("callProperties") { details.callProperties }
        safePut("callCapabilities") { details.callCapabilities }
        safePut("accountHandleId") { details.accountHandle?.id }
        safePut("extras") { details.extras?.toJson() }
        safePut("intentExtras") { details.intentExtras?.toJson() }
        put("decision", if (decision.block) "blocked" else "allowed")
        put("matched", decision.reason)
    }
}

private inline fun JSONObject.safePut(key: String, read: () -> Any?) {
    val value = try {
        read() ?: JSONObject.NULL
    } catch (e: Exception) {
        "<error: ${e.javaClass.simpleName}>"
    }
    put(key, value)
}

@Suppress("DEPRECATION")
private fun Bundle.toJson(): JSONObject = JSONObject().also { json ->
    for (key in keySet()) json.safePut(key) { stringify(get(key)) }
}

private fun stringify(value: Any?): Any = when {
    value == null -> JSONObject.NULL
    value is Bundle -> value.toJson()
    value.javaClass.isArray -> (0 until java.lang.reflect.Array.getLength(value))
        .joinToString(prefix = "[", postfix = "]") { stringify(java.lang.reflect.Array.get(value, it)).toString() }
    else -> value.toString()
}
