package dev.codingcoffee.screen.data

import android.telecom.Call
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class LogEntry(
    val id: String,
    val timestamp: Long,
    val rawNumber: String?,
    val number: String?,
    val callerName: String?,
    val outgoing: Boolean,
    val blocked: Boolean,
    val reason: String,
    val json: JSONObject,
) {
    companion object {
        fun parse(line: String): LogEntry? = runCatching {
            val o = JSONObject(line)
            LogEntry(
                id = o.getString("id"),
                timestamp = o.getLong("timestamp"),
                rawNumber = o.str("rawNumber"),
                number = o.str("number"),
                callerName = o.str("callerDisplayName"),
                outgoing = o.optInt("callDirection", Call.Details.DIRECTION_UNKNOWN) == Call.Details.DIRECTION_OUTGOING,
                blocked = o.optString("decision") == "blocked",
                reason = o.optString("matched"),
                json = o,
            )
        }.getOrNull()
    }
}

fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key)

class CallLogStore(private val file: File, scope: CoroutineScope) {
    private val mutex = Mutex()
    private val _entries = MutableStateFlow<List<LogEntry>?>(null)

    /** Newest first; null until the file has been read. */
    val entries: StateFlow<List<LogEntry>?> = _entries.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                _entries.value = if (file.exists()) file.readLines().mapNotNull(LogEntry::parse).reversed() else emptyList()
            }
        }
    }

    suspend fun append(entry: JSONObject) = withContext(Dispatchers.IO) {
        val line = entry.toString()
        mutex.withLock {
            file.appendText(line + "\n")
            val parsed = LogEntry.parse(line) ?: return@withLock
            _entries.update { listOf(parsed) + it.orEmpty() }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            file.writeText("")
            _entries.value = emptyList()
        }
    }
}
