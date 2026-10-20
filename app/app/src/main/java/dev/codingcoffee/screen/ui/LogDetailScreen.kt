package dev.codingcoffee.screen.ui

import android.content.ClipData
import android.os.Build
import android.telecom.Call
import android.telecom.Connection
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.codingcoffee.screen.app
import dev.codingcoffee.screen.data.LogEntry
import dev.codingcoffee.screen.data.str
import dev.codingcoffee.screen.rules.SimpleMatch
import dev.codingcoffee.screen.rules.SimpleRule
import dev.codingcoffee.screen.rules.escapeRegex
import dev.codingcoffee.screen.rules.toPattern
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(id: String?, onBack: () -> Unit, onCreateRule: (pattern: String, name: String) -> Unit) {
    val entries by LocalContext.current.app.callLog.entries.collectAsStateWithLifecycle()
    val entry = entries?.find { it.id == id }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(entry?.let { formatNumber(it.rawNumber ?: it.number) } ?: "Call") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (entry == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TonalCard {
                Column(Modifier.padding(vertical = 8.dp)) {
                    keyFields(entry).forEach { (label, value) ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            overlineContent = { Text(label) },
                            headlineContent = { SelectionContainer { Text(value) } },
                        )
                    }
                }
            }
            entry.number?.let { number ->
                FilledTonalButton(
                    onClick = { onCreateRule(exactNumberPattern(number), formatNumber(number)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Create rule from this number")
                }
            }
            RawJson(entry, snackbar)
        }
    }
}

@Composable
private fun RawJson(entry: LogEntry, snackbar: SnackbarHostState) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val pretty = remember(entry) { entry.json.toString(2) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Raw data", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
            TextButton(onClick = {
                scope.launch {
                    clipboard.setClipEntry(ClipData.newPlainText("Call log entry", pretty).toClipEntry())
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) snackbar.showSnackbar("Copied")
                }
            }) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Copy")
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SelectionContainer {
                Text(
                    text = pretty,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    softWrap = false,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp),
                )
            }
        }
    }
}

private fun keyFields(entry: LogEntry): List<Pair<String, String>> {
    val json = entry.json
    val direction = when (json.optInt("callDirection", Call.Details.DIRECTION_UNKNOWN)) {
        Call.Details.DIRECTION_INCOMING -> "Incoming"
        Call.Details.DIRECTION_OUTGOING -> "Outgoing"
        else -> "Unknown"
    }
    val verification = when (json.optInt("callerNumberVerificationStatus", -1)) {
        Connection.VERIFICATION_STATUS_PASSED -> "Passed"
        Connection.VERIFICATION_STATUS_FAILED -> "Failed"
        Connection.VERIFICATION_STATUS_NOT_VERIFIED -> "Not verified"
        else -> null
    }
    return listOfNotNull(
        "Decision" to if (entry.blocked) "Blocked" else "Allowed",
        "Matched" to entry.reason,
        "Time" to DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(entry.timestamp)),
        "Direction" to direction,
        "Number (raw)" to (entry.rawNumber ?: "—"),
        "Number (normalized)" to (entry.number ?: "—"),
        "Caller name" to (entry.callerName?.takeIf { it.isNotBlank() } ?: "—"),
        verification?.let { "Number verification" to it },
        json.str("accountHandleId")?.let { "SIM account" to it },
    )
}

private fun exactNumberPattern(number: String): String =
    if (number.startsWith("+") && !number.startsWith("+91")) "^${escapeRegex(number)}$"
    else SimpleRule(SimpleMatch.EXACTLY, number).toPattern()
