package dev.codingcoffee.screen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.codingcoffee.screen.app
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.app
    val entries by app.callLog.entries.collectAsStateWithLifecycle()
    val roleHeld = rememberRoleHeld()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Settings") }, scrollBehavior = scrollBehavior) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            SectionHeader("Screening")
            TonalCard {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClickLabel = "Open system settings") {
                        openDefaultAppsSettings(context)
                    },
                    leadingContent = { Icon(Icons.Rounded.Shield, contentDescription = null) },
                    headlineContent = { Text("Call screening role") },
                    supportingContent = {
                        Text(
                            if (roleHeld) "Active. Screen is your caller ID & spam app."
                            else "Inactive. Tap to choose Screen as your caller ID & spam app.",
                            color = if (roleHeld) colors.onSurfaceVariant else colors.error,
                        )
                    },
                )
            }

            SectionHeader("Data")
            TonalCard {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { confirmClear = true },
                    leadingContent = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
                    headlineContent = { Text("Clear call log") },
                    supportingContent = { Text("${entries?.size ?: 0} entries") },
                )
            }

            SectionHeader("About")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Note(
                    Icons.Rounded.Lock,
                    "Everything stays on this device. This app has no internet access.",
                )
                Note(
                    Icons.Rounded.Lightbulb,
                    "Tip: Google Phone's built-in “Filter spam calls” setting works alongside this app. " +
                        "Find it in Phone › Settings › Caller ID & spam.",
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text("Clear call log?") },
            text = { Text("All screened call entries will be removed from this device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    app.scope.launch { app.callLog.clear() }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Note(icon: ImageVector, text: String) {
    TonalCard {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
