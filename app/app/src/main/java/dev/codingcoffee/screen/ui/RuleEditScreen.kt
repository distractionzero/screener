package dev.codingcoffee.screen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.codingcoffee.screen.app
import dev.codingcoffee.screen.rules.Rule
import dev.codingcoffee.screen.rules.RuleAction
import dev.codingcoffee.screen.rules.RuleType
import dev.codingcoffee.screen.rules.compilePattern
import dev.codingcoffee.screen.rules.normalizeNumber
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.regex.PatternSyntaxException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditScreen(id: String?, initialPattern: String?, initialName: String?, onDone: () -> Unit) {
    val app = LocalContext.current.app
    val existing = remember(id) { app.settings.value?.rules?.find { it.id == id } }

    var name by rememberSaveable { mutableStateOf(existing?.name ?: initialName.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: RuleType.NUMBER) }
    var action by rememberSaveable { mutableStateOf(existing?.action ?: RuleAction.BLOCK) }
    var pattern by rememberSaveable { mutableStateOf(existing?.pattern ?: initialPattern.orEmpty()) }
    var sample by rememberSaveable { mutableStateOf("") }

    val regex = remember(pattern) { compilePattern(pattern) }
    val patternError = regex.exceptionOrNull()?.let {
        if (it is PatternSyntaxException) {
            if (it.index >= 0) "${it.description} near position ${it.index + 1}" else it.description
        } else {
            it.message ?: "Invalid pattern"
        }
    }
    val canSave = name.isNotBlank() && pattern.isNotBlank() && patternError == null
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(if (existing == null) "New rule" else "Edit rule") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, contentDescription = "Cancel") }
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            val rule = Rule(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                type = type,
                                pattern = pattern,
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Labeled("Match on") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    RuleType.entries.forEachIndexed { i, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(i, RuleType.entries.size),
                        ) { Text(if (option == RuleType.NUMBER) "Number" else "Caller name") }
                    }
                }
            }

            Labeled("Action") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    RuleAction.entries.forEachIndexed { i, option ->
                        SegmentedButton(
                            selected = action == option,
                            onClick = { action = option },
                            shape = SegmentedButtonDefaults.itemShape(i, RuleAction.entries.size),
                        ) { Text(if (option == RuleAction.ALLOW) "Allow" else "Block") }
                    }
                }
            }

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
                            "Matched against the number with spaces, dashes and brackets removed, e.g. +911409806457."
                        } else {
                            "Matched against the caller name your carrier sends, if any. Use (?i) to ignore case."
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            val testResult = regex.getOrNull()?.takeIf { sample.isNotBlank() }?.let {
                val subject = if (type == RuleType.NUMBER) normalizeNumber(sample).orEmpty() else sample
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
                            null -> "Type a sample to check your pattern."
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
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
