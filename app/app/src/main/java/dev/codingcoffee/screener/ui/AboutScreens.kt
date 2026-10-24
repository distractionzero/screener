package dev.codingcoffee.screener.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val APACHE_2 = "https://www.apache.org/licenses/LICENSE-2.0"

private data class Library(val name: String, val owner: String, val license: String, val url: String)

private val Libraries = listOf(
    Library("AndroidX Activity, Core, Lifecycle", "Google", "Apache License 2.0", APACHE_2),
    Library("AndroidX Navigation", "Google", "Apache License 2.0", APACHE_2),
    Library("AndroidX DataStore", "Google", "Apache License 2.0", APACHE_2),
    Library(
        "Protocol Buffers (bundled in DataStore)",
        "Google",
        "BSD 3-Clause License",
        "https://github.com/protocolbuffers/protobuf/blob/main/LICENSE",
    ),
    Library("Jetpack Compose UI, Foundation, Material 3", "Google", "Apache License 2.0", APACHE_2),
    Library("Material Icons and Material Symbols", "Google", "Apache License 2.0", APACHE_2),
    Library("Kotlin Standard Library", "JetBrains", "Apache License 2.0", APACHE_2),
    Library("kotlinx.coroutines", "JetBrains", "Apache License 2.0", APACHE_2),
    Library("libphonenumber", "Google", "Apache License 2.0", APACHE_2),
)

private val PrivacyPolicy = listOf(
    "Last updated" to "20 October 2026",
    "Summary" to "Screener does not collect, send, or share any of your data. " +
        "Everything it handles stays on your phone.",
    "What the app sees" to "When a call arrives from a number that isn't in your contacts, Android passes the " +
        "app the phone number, the caller name provided by your network (if any), and technical details " +
        "about the call. The app checks these against your rules on the device to decide whether to " +
        "reject the call. Calls from your contacts and calls you make are not recorded.",
    "What the app stores" to "A log of screened incoming calls, your rules, and your settings. These are kept " +
        "in the app's private storage on this device and are excluded from cloud backups and " +
        "device-to-device transfers.",
    "No internet access" to "The app does not request the internet permission, so it cannot send anything " +
        "off your device. It has no analytics, advertising, crash reporting, or third-party tracking.",
    "Permissions" to "The app only asks to be your caller ID & spam app (the call screening role). " +
        "You can revoke it at any time in system settings.",
    "Your control" to "You can clear the call log at any time in Settings. Uninstalling the app deletes all " +
        "of its data.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScaffold(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = content,
    )
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    DetailScaffold("Privacy policy", onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PrivacyPolicy.forEach { (heading, body) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        heading,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(body, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    DetailScaffold("Open source licenses", onBack) { padding ->
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
            items(Libraries) { library ->
                TonalCard {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(onClickLabel = "Open license") { openUri(context, library.url) },
                        headlineContent = { Text(library.name) },
                        supportingContent = { Text("${library.owner} · ${library.license}") },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                        },
                    )
                }
            }
            item {
                Text(
                    "Also includes other AndroidX libraries these depend on, all under the Apache License 2.0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
