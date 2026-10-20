package dev.codingcoffee.screen.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.codingcoffee.screen.rules.DefaultRules
import dev.codingcoffee.screen.rules.Rule
import dev.codingcoffee.screen.rules.RuleAction
import dev.codingcoffee.screen.rules.RuleType
import dev.codingcoffee.screen.rules.isDefault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val rules: List<Rule>,
    val blockUnknown: Boolean,
    val onboarded: Boolean,
    val theme: ThemeMode = ThemeMode.SYSTEM,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

private val RULES = stringPreferencesKey("rules")
private val BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown")
private val ONBOARDED = booleanPreferencesKey("onboarded")
private val THEME = stringPreferencesKey("theme")
private val DEFAULT_OVERRIDES = stringSetPreferencesKey("default_overrides")

class SettingsStore(context: Context) {
    private val store = context.dataStore

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            rules = prefs.rules(),
            blockUnknown = prefs[BLOCK_UNKNOWN] ?: false,
            onboarded = prefs[ONBOARDED] ?: false,
            theme = ThemeMode.entries.find { it.name == prefs[THEME] } ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun saveRule(rule: Rule) = updateRules { rules ->
        if (rules.any { it.id == rule.id }) rules.map { if (it.id == rule.id) rule else it } else rules + rule
    }

    suspend fun deleteRule(id: String) = updateRules { rules -> rules.filterNot { it.id == id } }

    suspend fun insertRule(rule: Rule, index: Int) = updateRules { rules ->
        rules.toMutableList().apply { add(index.coerceIn(0, size), rule) }
    }

    suspend fun setRuleEnabled(rule: Rule, enabled: Boolean) {
        if (!rule.isDefault) return saveRule(rule.copy(enabled = enabled))
        store.edit { it[DEFAULT_OVERRIDES] = encodeOverrides(it.defaultOverrides() + (rule.id to enabled)) }
    }

    suspend fun setBlockUnknown(enabled: Boolean) {
        store.edit { it[BLOCK_UNKNOWN] = enabled }
    }

    suspend fun setTheme(theme: ThemeMode) {
        store.edit { it[THEME] = theme.name }
    }

    suspend fun setOnboarded() {
        store.edit { it[ONBOARDED] = true }
    }

    private suspend fun updateRules(transform: (List<Rule>) -> List<Rule>) {
        store.edit { it[RULES] = encodeRules(transform(it.customRules())) }
    }
}

private fun Preferences.storedRules(): List<Rule> = this[RULES]?.let(::decodeRules).orEmpty()

private fun Preferences.customRules(): List<Rule> = storedRules().filterNot { it.isDefault }

private fun Preferences.defaultOverrides(): Map<String, Boolean> =
    this[DEFAULT_OVERRIDES].orEmpty().associate { it.substringBefore('=') to (it.substringAfter('=') == "true") }

private fun encodeOverrides(overrides: Map<String, Boolean>): Set<String> =
    overrides.map { (id, enabled) -> "$id=$enabled" }.toSet()

private fun Preferences.rules(): List<Rule> {
    val overrides = defaultOverrides()
    return DefaultRules.map { rule -> overrides[rule.id]?.let { rule.copy(enabled = it) } ?: rule } + customRules()
}

private fun encodeRules(rules: List<Rule>): String = JSONArray().apply {
    rules.forEach { rule ->
        put(
            JSONObject()
                .put("id", rule.id)
                .put("name", rule.name)
                .put("type", rule.type.name)
                .put("pattern", rule.pattern)
                .put("action", rule.action.name)
                .put("enabled", rule.enabled)
        )
    }
}.toString()

private fun decodeRules(json: String): List<Rule> {
    val array = JSONArray(json)
    return (0 until array.length()).mapNotNull { i ->
        runCatching {
            val o = array.getJSONObject(i)
            Rule(
                id = o.getString("id"),
                name = o.getString("name"),
                type = RuleType.valueOf(o.getString("type")),
                pattern = o.getString("pattern"),
                action = RuleAction.valueOf(o.getString("action")),
                enabled = o.getBoolean("enabled"),
            )
        }.getOrNull()
    }
}
