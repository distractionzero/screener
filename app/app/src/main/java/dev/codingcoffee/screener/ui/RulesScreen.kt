package dev.codingcoffee.screener.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.codingcoffee.screener.app
import dev.codingcoffee.screener.rules.Rule
import dev.codingcoffee.screener.rules.RuleAction
import dev.codingcoffee.screener.rules.RuleType
import dev.codingcoffee.screener.rules.describe
import dev.codingcoffee.screener.rules.isDefault
import dev.codingcoffee.screener.rules.parseSimple
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(onEdit: (String) -> Unit, onAdd: () -> Unit) {
    val app = LocalContext.current.app
    val settings by app.settings.collectAsStateWithLifecycle()
    val rules = settings?.rules.orEmpty()
    val region = currentRegion()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val onToggle: (Rule, Boolean) -> Unit = { rule, enabled ->
        app.scope.launch { app.settingsStore.setRuleEnabled(rule, enabled) }
    }
    val onDelete: (Rule) -> Unit = { rule ->
        val index = rules.filterNot { it.isDefault }.indexOf(rule)
        app.scope.launch { app.settingsStore.deleteRule(rule.id) }
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar("Deleted “${rule.name}”", "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) app.settingsStore.insertRule(rule, index)
        }
    }

    val onDeleteSpam: (String) -> Unit = { key ->
        app.scope.launch { app.settingsStore.removeSpamNumber(key) }
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar("Removed ${formatNumber(key, region)}", "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) app.settingsStore.addSpamNumber(key)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Rules") }, scrollBehavior = scrollBehavior) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Rounded.Add, contentDescription = "Add rule") }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ruleSection(
                title = "Block",
                emptyText = "No block rules yet. Tap + to add one.",
                rules = rules.filter { it.action == RuleAction.BLOCK },
                onEdit = onEdit, onToggle = onToggle, onDelete = onDelete,
            )
            item(key = "header-spam") { SectionHeader("Spam numbers") }
            val spamNumbers = settings?.spamNumbers.orEmpty().sorted()
            if (spamNumbers.isEmpty()) {
                item(key = "empty-spam") {
                    Text(
                        "Numbers you mark as spam in Activity show up here. They're always blocked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
            items(spamNumbers, key = { "spam-$it" }) { key ->
                SpamNumberRow(key, onDelete = { onDeleteSpam(key) })
            }
            item(key = "header-unknown") { SectionHeader("Everyone else") }
            item(key = "block-unknown") {
                BlockUnknownRow(
                    enabled = settings?.blockUnknown ?: false,
                    onChange = { enabled -> app.scope.launch { app.settingsStore.setBlockUnknown(enabled) } },
                )
            }
            ruleSection(
                title = "Allow",
                emptyText = "No allow rules. Allow rules are checked first and always let a call through.",
                rules = rules.filter { it.action == RuleAction.ALLOW },
                onEdit = onEdit, onToggle = onToggle, onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun BlockUnknownRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    TonalCard {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.toggleable(value = enabled, role = Role.Switch, onValueChange = onChange),
            leadingContent = { Icon(Icons.Rounded.PersonOff, contentDescription = null) },
            headlineContent = { Text("Block all unknown callers") },
            supportingContent = {
                Text(
                    "Rejects every call from a number not in your contacts, unless an allow rule matches. " +
                        "This also blocks delivery agents, cab drivers and anyone new.",
                    color = if (enabled) colors.error else colors.onSurfaceVariant,
                )
            },
            trailingContent = { Switch(checked = enabled, onCheckedChange = null) },
        )
    }
}

private fun LazyListScope.ruleSection(
    title: String,
    emptyText: String,
    rules: List<Rule>,
    onEdit: (String) -> Unit,
    onToggle: (Rule, Boolean) -> Unit,
    onDelete: (Rule) -> Unit,
) {
    item(key = "header-$title") { SectionHeader(title) }
    if (rules.isEmpty()) {
        item(key = "empty-$title") {
            Text(
                emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
    items(rules, key = { it.id }) { rule ->
        RuleRow(rule, onClick = { onEdit(rule.id) }, onToggle = { onToggle(rule, it) }, onDelete = { onDelete(rule) })
    }
}

@Composable
private fun RuleRow(rule: Rule, onClick: () -> Unit, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    if (rule.isDefault) {
        Card(
            onClick = onClick,
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        ) {
            RuleListItem(rule = rule, onToggle = onToggle)
        }
        return
    }

    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Surface(color = colors.errorContainer, shape = CardShape, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.onErrorContainer)
                }
            }
        },
    ) {
        Card(
            onClick = onClick,
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            modifier = Modifier.semantics {
                customActions = listOf(CustomAccessibilityAction("Delete") { onDelete(); true })
            },
        ) {
            RuleListItem(rule = rule, onToggle = onToggle)
        }
    }
}

@Composable
private fun SpamNumberRow(key: String, onDelete: () -> Unit) {
    val region = currentRegion()
    TonalCard {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { Icon(Icons.Rounded.Report, contentDescription = null) },
            headlineContent = { Text(formatNumber(key, region)) },
            supportingContent = { Text("Marked as spam") },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Remove ${formatNumber(key, region)} from spam numbers")
                }
            },
        )
    }
}

@Composable
private fun RuleListItem(rule: Rule, onToggle: (Boolean) -> Unit) {
    val kind = if (rule.type == RuleType.NUMBER) "Number" else "Caller name"
    val region = currentRegion()
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(rule.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        overlineContent = { Text(if (rule.isDefault) "$kind · Built-in" else kind) },
        supportingContent = {
            val simple = remember(rule.type, rule.pattern, region) { parseSimple(rule.type, rule.pattern, region) }
            Text(
                simple?.describe(region) ?: rule.pattern,
                fontFamily = if (simple == null) FontFamily.Monospace else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics { contentDescription = "Enable ${rule.name}" },
            )
        },
    )
}
