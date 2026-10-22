package dev.codingcoffee.screen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.codingcoffee.screen.app
import dev.codingcoffee.screen.data.LogEntry
import dev.codingcoffee.screen.rules.REASON_NONE
import dev.codingcoffee.screen.rules.REASON_SPAM
import dev.codingcoffee.screen.rules.REASON_UNKNOWN
import dev.codingcoffee.screen.rules.spamKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

private enum class Filter(val label: String) { ALL("All"), BLOCKED("Blocked"), ALLOWED("Allowed") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(onOpenEntry: (String) -> Unit) {
    val app = LocalContext.current.app
    val entries by app.callLog.entries.collectAsStateWithLifecycle()
    val settings by app.settings.collectAsStateWithLifecycle()
    val spamNumbers = settings?.spamNumbers.orEmpty()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val roleHeld = rememberRoleHeld()
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val incoming = remember(entries) { entries?.filterNot { it.outgoing } }
    val visible = remember(incoming, filter) {
        incoming.orEmpty().filter {
            when (filter) {
                Filter.ALL -> true
                Filter.BLOCKED -> it.blocked
                Filter.ALLOWED -> !it.blocked
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Activity") }, scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { StatusCard(roleHeld, incoming.orEmpty()) }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Filter.entries.forEach {
                        FilterChip(
                            selected = filter == it,
                            onClick = { filter = it },
                            label = { Text(it.label) },
                        )
                    }
                }
            }
            val all = incoming
            when {
                all == null -> Unit
                all.isEmpty() -> item {
                    EmptyState(
                        icon = Icons.Rounded.Call,
                        title = "No calls screened yet",
                        body = "Calls from unknown numbers will show up here.",
                    )
                }
                visible.isEmpty() -> item {
                    EmptyState(
                        icon = if (filter == Filter.BLOCKED) Icons.Rounded.Block else Icons.Rounded.Person,
                        title = "No ${filter.label.lowercase()} calls",
                        body = "Try another filter.",
                    )
                }
                else -> items(visible, key = { it.id }) { entry ->
                    val number = entry.number?.takeUnless { entry.blocked || spamKey(it) in spamNumbers }
                    CallRow(
                        entry,
                        onClick = { onOpenEntry(entry.id) },
                        onMarkSpam = number?.let { { scope.markSpam(app, it, snackbar) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(roleHeld: Boolean, entries: List<LogEntry>) {
    val requestRole = rememberRoleRequest()
    val (today, week) = remember(entries) { blockedCounts(entries) }
    val colors = MaterialTheme.colorScheme

    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (roleHeld) colors.secondaryContainer else colors.errorContainer,
            contentColor = if (roleHeld) colors.onSecondaryContainer else colors.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = if (roleHeld) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (roleHeld) "Screening active" else "Screening inactive",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (roleHeld) "Unknown callers are checked against your rules."
                        else "Calls aren't being checked. Set Phone Screener as your caller ID & spam app.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (!roleHeld) {
                FilledTonalButton(onClick = requestRole) { Text("Fix") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Stat(today, "Blocked today")
                Stat(week, "Blocked this week")
            }
        }
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun blockedCounts(entries: List<LogEntry>): Pair<Int, Int> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val weekStart = today.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))
    val dates = entries.filter { it.blocked }.map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
    return dates.count { it == today } to dates.count { !it.isBefore(weekStart) }
}

@Composable
private fun CallRow(entry: LogEntry, onClick: () -> Unit, onMarkSpam: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val label = when {
        entry.reason == REASON_UNKNOWN -> "Unknown caller"
        entry.reason == REASON_SPAM -> "Marked as spam"
        entry.reason != REASON_NONE && entry.reason.isNotBlank() -> entry.reason
        else -> entry.callerName?.takeIf { it.isNotBlank() }
    }

    Card(onClick = onClick, shape = CardShape, colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                if (entry.blocked) {
                    IconCircle(Icons.Rounded.Block, "Blocked", colors.errorContainer, colors.error)
                } else {
                    IconCircle(Icons.Rounded.Person, "Allowed", colors.primaryContainer, colors.onPrimaryContainer)
                }
            },
            headlineContent = {
                Text(formatNumber(entry.rawNumber ?: entry.number), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(
                    listOfNotNull(label, relativeTime(entry.timestamp)).joinToString(" · "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = if (onMarkSpam == null) null else {
                { IconButton(onClick = onMarkSpam) { Icon(Icons.Rounded.Report, contentDescription = "Mark as spam") } }
            },
        )
    }
}
