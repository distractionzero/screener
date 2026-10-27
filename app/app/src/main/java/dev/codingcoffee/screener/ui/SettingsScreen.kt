package dev.codingcoffee.screener.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.codingcoffee.screener.app
import dev.codingcoffee.screener.data.ThemeMode
import dev.codingcoffee.screener.rules.callingCode
import dev.codingcoffee.screener.rules.isDefault
import dev.codingcoffee.screener.rules.supportedRegions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DEVELOPER_NAME = "Ameya Shenoy"
private const val DEVELOPER_URL = "https://www.codingcoffee.dev"
private const val REPO_URL = "https://github.com/distractionzero/screener"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenPrivacy: () -> Unit, onOpenLicenses: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val settings by app.settings.collectAsStateWithLifecycle()
    val entries by app.callLog.entries.collectAsStateWithLifecycle()
    val roleHeld = rememberRoleHeld()
    val theme = settings?.theme ?: ThemeMode.SYSTEM
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var pickCountry by rememberSaveable { mutableStateOf(false) }
    val region = currentRegion()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val personalRules = settings?.rules?.count { !it.isDefault } ?: 0
    val spamNumbers = settings?.spamNumbers?.size ?: 0

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/jsonl")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                val jsonl = app.settingsStore.exportRules()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(jsonl) }
                }
                "Exported $personalRules rules and $spamNumbers spam numbers"
            }.getOrElse { "Export failed" }
            snackbar.showSnackbar(message)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                val jsonl = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                }
                val result = app.settingsStore.importRules(jsonl)
                "Imported ${result.rules} rules and ${result.spamNumbers} spam numbers"
            }.getOrElse { "Import failed" }
            snackbar.showSnackbar(message)
        }
    }
    val exportActivityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/jsonl")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                val jsonl = app.callLog.exportEntries()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(jsonl) }
                }
                "Exported ${entries?.size ?: 0} calls"
            }.getOrElse { "Export failed" }
            snackbar.showSnackbar(message)
        }
    }
    val importActivityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                val jsonl = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                }
                "Imported ${app.callLog.importEntries(jsonl)} calls"
            }.getOrElse { "Import failed" }
            snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Settings") }, scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbar) },
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
                            if (roleHeld) "Active. Screener is your caller ID & spam app."
                            else "Inactive. Tap to choose Screener as your caller ID & spam app.",
                            color = if (roleHeld) colors.onSurfaceVariant else colors.error,
                        )
                    },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { pickCountry = true },
                    leadingContent = { Icon(Icons.Rounded.Public, contentDescription = null) },
                    headlineContent = { Text("Country") },
                    supportingContent = {
                        Text(countryName(region) + if (settings?.country == null) " (automatic)" else "")
                    },
                )
            }

            SectionHeader("Appearance")
            TonalCard {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                    headlineContent = { Text("Theme") },
                )
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = theme == mode,
                            onClick = { app.scope.launch { app.settingsStore.setTheme(mode) } },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                        ) {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                }
                            )
                        }
                    }
                }
            }

            Note(
                Icons.Rounded.Lock,
                "Everything stays on this device. This app has no internet access.",
                Modifier.padding(top = 24.dp),
            )

            SectionHeader("About")
            TonalCard {
                LinkRow(Icons.Rounded.Policy, "Privacy policy", onClick = onOpenPrivacy)
                LinkRow(Icons.Rounded.Description, "Open source licenses", onClick = onOpenLicenses)
            }

            SectionHeader("Data")
            TonalCard {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { exportLauncher.launch(exportName("phone-screener-rules")) },
                    leadingContent = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                    headlineContent = { Text("Export rules") },
                    supportingContent = { Text("$personalRules personal rules, $spamNumbers spam numbers") },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { importLauncher.launch(arrayOf("*/*")) },
                    leadingContent = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                    headlineContent = { Text("Import rules") },
                    supportingContent = { Text("From a .jsonl export") },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { exportActivityLauncher.launch(exportName("screener-activity")) },
                    leadingContent = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                    headlineContent = { Text("Export activity") },
                    supportingContent = { Text("${entries?.size ?: 0} calls") },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { importActivityLauncher.launch(arrayOf("*/*")) },
                    leadingContent = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                    headlineContent = { Text("Import activity") },
                    supportingContent = { Text("From a .jsonl export, duplicates are skipped") },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { confirmClear = true },
                    leadingContent = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
                    headlineContent = { Text("Clear call log") },
                    supportingContent = { Text("${entries?.size ?: 0} entries") },
                )
            }

            TonalCard(Modifier.padding(top = 24.dp)) {
                LinkRow(Icons.Rounded.Person, "Developer", "$DEVELOPER_NAME\n${DEVELOPER_URL.removePrefix("https://www.")}") {
                    openUri(context, DEVELOPER_URL)
                }
                LinkRow(Icons.Rounded.Code, "Source code", REPO_URL.removePrefix("https://")) {
                    openUri(context, REPO_URL)
                }
            }
        }
    }

    if (pickCountry) {
        CountryDialog(
            selected = settings?.country,
            detected = settings?.detectedRegion ?: region,
            onSelect = { country ->
                pickCountry = false
                app.scope.launch { app.settingsStore.setCountry(country) }
            },
            onDismiss = { pickCountry = false },
        )
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
private fun CountryDialog(selected: String?, detected: String, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val countries = remember { supportedRegions.map { it to countryName(it) }.sortedBy { it.second } }
    val search = query.trim()
    val visible = countries.filter { (code, name) ->
        name.contains(search, ignoreCase = true) || code.equals(search, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Country") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Numbers without a country code are treated as being from here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    if (search.isEmpty()) {
                        item { CountryOption("Automatic (${countryName(detected)})", selected == null) { onSelect(null) } }
                    }
                    items(visible, key = { it.first }) { (code, name) ->
                        CountryOption("$name +${callingCode(code)}", selected == code) { onSelect(code) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CountryOption(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        headlineContent = { Text(label) },
    )
}

private val ExportTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")

private fun exportName(prefix: String) = "$prefix-${LocalDateTime.now().format(ExportTimestamp)}.jsonl"

private fun countryName(region: String): String =
    runCatching { Locale.Builder().setRegion(region).build().displayCountry }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: region

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
    )
}

@Composable
private fun Note(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    TonalCard(modifier) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
