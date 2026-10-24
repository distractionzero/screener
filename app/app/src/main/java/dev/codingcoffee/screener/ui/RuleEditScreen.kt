package dev.codingcoffee.screener.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.codingcoffee.screener.app
import dev.codingcoffee.screener.rules.Rule
import dev.codingcoffee.screener.rules.RuleAction
import dev.codingcoffee.screener.rules.RuleType
import dev.codingcoffee.screener.rules.SimpleMatch
import dev.codingcoffee.screener.rules.SimpleRule
import dev.codingcoffee.screener.rules.compilePattern
import dev.codingcoffee.screener.rules.describe
import dev.codingcoffee.screener.rules.isDefault
import dev.codingcoffee.screener.rules.isValid
import dev.codingcoffee.screener.rules.normalizeNumber
import dev.codingcoffee.screener.rules.parseSimple
import dev.codingcoffee.screener.rules.toPattern
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.regex.PatternSyntaxException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditScreen(id: String?, initialPattern: String?, initialName: String?, onDone: () -> Unit) {
    val app = LocalContext.current.app
    val existing = remember(id) { app.settings.value?.rules?.find { it.id == id } }
    if (existing != null && existing.isDefault) {
        BuiltInRuleScreen(existing.id, onDone)
        return
    }
    val region = currentRegion()
    val startPattern = existing?.pattern ?: initialPattern.orEmpty()
    val startType = existing?.type ?: RuleType.NUMBER
    val parsed = remember { parseSimple(startType, startPattern, region) }

    var advanced by rememberSaveable { mutableStateOf(startPattern.isNotEmpty() && parsed == null) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: initialName.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(startType) }
    var action by rememberSaveable { mutableStateOf(existing?.action ?: RuleAction.BLOCK) }
    var pattern by rememberSaveable { mutableStateOf(startPattern) }
    var match by rememberSaveable { mutableStateOf(parsed?.match ?: SimpleMatch.STARTS_WITH) }
    var value by rememberSaveable { mutableStateOf(parsed?.value.orEmpty()) }
    var sample by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val simple = SimpleRule(match, value)
    val effectivePattern = if (advanced) pattern else simple.takeIf { it.isValid(region) }?.toPattern(region).orEmpty()
    val regex = remember(effectivePattern) { compilePattern(effectivePattern) }
    val patternError = regex.exceptionOrNull()?.let {
        if (it is PatternSyntaxException) {
            if (it.index >= 0) "${it.description} near position ${it.index + 1}" else it.description
        } else {
            it.message ?: "Invalid pattern"
        }
    }
    val canSave = effectivePattern.isNotBlank() && patternError == null
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun switchMode(toAdvanced: Boolean) {
        if (toAdvanced && simple.isValid(region)) pattern = simple.toPattern(region)
        if (!toAdvanced) parseSimple(type, pattern, region)?.let { match = it.match; value = it.value }
        if (!toAdvanced && type == RuleType.CALLER_NAME) match = SimpleMatch.CONTAINS_ANY
        advanced = toAdvanced
    }

    fun changeType(newType: RuleType) {
        if (newType == type) return
        type = newType
        match = if (newType == RuleType.CALLER_NAME) SimpleMatch.CONTAINS_ANY else SimpleMatch.STARTS_WITH
        value = ""
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(if (existing == null) "New rule" else "Edit rule") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, contentDescription = "Cancel") }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete rule")
                        }
                    }
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            val fallbackName = if (advanced) effectivePattern else simple.describe(region)
                            val rule = Rule(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                name = name.trim().ifEmpty { fallbackName },
                                type = type,
                                pattern = effectivePattern,
                                action = action,
                                enabled = existing?.enabled ?: true,
                            )
                            app.scope.launch { app.settingsStore.saveRule(rule) }
                            onDone()
                        },
                    ) { Text("Save") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Choice(
                label = null,
                options = listOf(false to "Simple", true to "Advanced"),
                selected = advanced,
                onSelect = ::switchMode,
            )
            Choice(
                label = "Action",
                options = listOf(RuleAction.BLOCK to "Block", RuleAction.ALLOW to "Allow"),
                selected = action,
                onSelect = { action = it },
            )
            Choice(
                label = "Match on",
                options = listOf(RuleType.NUMBER to "Number", RuleType.CALLER_NAME to "Caller name"),
                selected = type,
                onSelect = ::changeType,
            )

            if (advanced) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Pattern (regex)") },
                    singleLine = true,
                    isError = patternError != null,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
                    supportingText = {
                        Text(
                            patternError ?: if (type == RuleType.NUMBER) {
                                "Matched against the number in international format, e.g. +12125550123."
                            } else {
                                "Matched against the caller name your carrier sends, if any. Use (?i) to ignore case."
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (type == RuleType.NUMBER) {
                Choice(
                    label = "Number",
                    options = listOf(SimpleMatch.STARTS_WITH to "Starts with", SimpleMatch.EXACTLY to "Is exactly"),
                    selected = match,
                    onSelect = { match = it },
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (match == SimpleMatch.STARTS_WITH) "Starting digits" else "Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    supportingText = { Text("No need to add the country code. Start with + to match another country.") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Caller name contains") },
                    placeholder = { Text("spam, fraud") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    supportingText = {
                        Text("Separate words with commas. Case doesn't matter. Only works if your carrier sends a name.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (optional)") },
                placeholder = { Text(if (advanced) "My rule" else simple.takeIf { it.isValid(region) }?.describe(region) ?: "My rule") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            val testResult = regex.getOrNull()?.takeIf { sample.isNotBlank() && effectivePattern.isNotEmpty() }?.let {
                val subject = if (type == RuleType.NUMBER) normalizeNumber(sample, region).orEmpty() else sample
                it.containsMatchIn(subject)
            }
            OutlinedTextField(
                value = sample,
                onValueChange = { sample = it },
                label = { Text(if (type == RuleType.NUMBER) "Test a number" else "Test a caller name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = if (type == RuleType.NUMBER) KeyboardType.Phone else KeyboardType.Text,
                ),
                supportingText = {
                    Text(
                        text = when (testResult) {
                            true -> "Match"
                            false -> "No match"
                            null -> "Type a sample to check your rule."
                        },
                        color = when (testResult) {
                            true -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("Delete rule?") },
            text = { Text("“${existing.name}” will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    app.scope.launch { app.settingsStore.deleteRule(existing.id) }
                    onDone()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuiltInRuleScreen(id: String, onBack: () -> Unit) {
    val app = LocalContext.current.app
    val settings by app.settings.collectAsStateWithLifecycle()
    val rule = settings?.rules?.find { it.id == id }
    val region = currentRegion()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(rule?.name ?: "Built-in rule") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (rule == null) return@Scaffold
        val simple = remember(rule.type, rule.pattern, region) { parseSimple(rule.type, rule.pattern, region) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = CardShape, color = colors.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.onSecondaryContainer)
                    Text(
                        "This is a built-in rule, so it's read-only. It can't be edited or deleted, only turned off.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSecondaryContainer,
                    )
                }
            }
            TonalCard {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.toggleable(
                        value = rule.enabled,
                        role = Role.Switch,
                        onValueChange = { enabled -> app.scope.launch { app.settingsStore.setRuleEnabled(rule, enabled) } },
                    ),
                    headlineContent = { Text("Enabled") },
                    trailingContent = { Switch(checked = rule.enabled, onCheckedChange = null) },
                )
            }
            TonalCard {
                Column(Modifier.padding(vertical = 8.dp)) {
                    listOfNotNull(
                        "Action" to if (rule.action == RuleAction.BLOCK) "Block" else "Allow",
                        "Match on" to if (rule.type == RuleType.NUMBER) "Number" else "Caller name",
                        simple?.let { "Matches" to it.describe(region) },
                    ).forEach { (label, value) ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            overlineContent = { Text(label) },
                            headlineContent = { Text(value) },
                        )
                    }
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        overlineContent = { Text("Pattern (regex)") },
                        headlineContent = { SelectionContainer { Text(rule.pattern, fontFamily = FontFamily.Monospace) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> Choice(label: String?, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (option, text) ->
                SegmentedButton(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(text) }
            }
        }
    }
}
