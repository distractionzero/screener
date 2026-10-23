package dev.codingcoffee.screener.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.codingcoffee.screener.rules.DefaultRules
import dev.codingcoffee.screener.rules.Rule
import dev.codingcoffee.screener.rules.RuleAction
import dev.codingcoffee.screener.rules.RuleType
import dev.codingcoffee.screener.rules.isDefault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val rules: List<Rule>,
    val blockUnknown: Boolean,
    val onboarded: Boolean,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val spamNumbers: Set<String> = emptySet(),
)

data class ImportResult(val rules: Int, val spamNumbers: Int)

private val Context.dataStore by preferencesDataStore(name = "settings")

private val RULES = stringPreferencesKey("rules")
private val BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown")
private val ONBOARDED = booleanPreferencesKey("onboarded")
private val THEME = stringPreferencesKey("theme")
private val DEFAULT_OVERRIDES = stringSetPreferencesKey("default_overrides")
private val SPAM_NUMBERS = stringSetPreferencesKey("spam_numbers")
private const val SPAM_FIELD = "spamNumber"

class SettingsStore(context: Context) {
    private val store = context.dataStore

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            rules = prefs.rules(),
            blockUnknown = prefs[BLOCK_UNKNOWN] ?: false,
            onboarded = prefs[ONBOARDED] ?: false,
            theme = ThemeMode.entries.find { it.name == prefs[THEME] } ?: ThemeMode.SYSTEM,
            spamNumbers = prefs[SPAM_NUMBERS].orEmpty(),
        )
    }

    suspend fun saveRule(rule: Rule) = updateRules { rules ->
        if (rules.any { it.id == rule.id }) rules.map { if (it.id == rule.id) rule else it } else rules + rule
    }

    suspend fun deleteRule(id: String) = updateRules { rules -> rules.filterNot { it.id == id } }

    suspend fun insertRule(rule: Rule, index: Int) = updateRules { rules ->
        rules.toMutableList().apply { add(index.coerceIn(0, size), rule) }
    }

    suspend fun exportRules(): String {
        val prefs = store.data.first()
        val rules = prefs.customRules().map { ruleToJson(it) }
        val spam = prefs[SPAM_NUMBERS].orEmpty().sorted().map { JSONObject().put(SPAM_FIELD, it) }
        return (rules + spam).joinToString("\n")
    }

    /** Rules whose id already exists are replaced; built-in and malformed lines are skipped. */
    suspend fun importRules(jsonl: String): ImportResult {
        val lines = jsonl.lineSequence().filter { it.isNotBlank() }
            .mapNotNull { runCatching { JSONObject(it) }.getOrNull() }
            .toList()
        val (spamLines, ruleLines) = lines.partition { it.has(SPAM_FIELD) }
        val spam = spamLines.mapNotNull { it.optString(SPAM_FIELD).takeIf(String::isNotBlank) }.toSet()
        val imported = ruleLines
            .mapNotNull { runCatching { ruleFromJson(it) }.getOrNull() }
            .filterNot { it.isDefault }
            .distinctBy { it.id }
        store.edit { prefs ->
            val rules = prefs.customRules()
            val byId = imported.associateBy { it.id }
            prefs[RULES] = encodeRules(rules.map { byId[it.id] ?: it } + imported.filter { new -> rules.none { it.id == new.id } })
            prefs[SPAM_NUMBERS] = prefs[SPAM_NUMBERS].orEmpty() + spam
        }
        return ImportResult(imported.size, spam.size)
    }

    suspend fun setRuleEnabled(rule: Rule, enabled: Boolean) {
        if (!rule.isDefault) return saveRule(rule.copy(enabled = enabled))
        store.edit { it[DEFAULT_OVERRIDES] = encodeOverrides(it.defaultOverrides() + (rule.id to enabled)) }
    }

    suspend fun addSpamNumber(key: String) {
        store.edit { it[SPAM_NUMBERS] = it[SPAM_NUMBERS].orEmpty() + key }
    }

    suspend fun removeSpamNumber(key: String) {
        store.edit { it[SPAM_NUMBERS] = it[SPAM_NUMBERS].orEmpty() - key }
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

private fun ruleToJson(rule: Rule): JSONObject = JSONObject()
    .put("id", rule.id)
    .put("name", rule.name)
    .put("type", rule.type.name)
    .put("pattern", rule.pattern)
    .put("action", rule.action.name)
    .put("enabled", rule.enabled)

private fun ruleFromJson(o: JSONObject): Rule = Rule(
    id = o.getString("id"),
    name = o.getString("name"),
    type = RuleType.valueOf(o.getString("type")),
    pattern = o.getString("pattern"),
    action = RuleAction.valueOf(o.getString("action")),
    enabled = o.getBoolean("enabled"),
)

private fun encodeRules(rules: List<Rule>): String = JSONArray().apply { rules.forEach { put(ruleToJson(it)) } }.toString()

private fun decodeRules(json: String): List<Rule> {
    val array = JSONArray(json)
    return (0 until array.length()).mapNotNull { i -> runCatching { ruleFromJson(array.getJSONObject(i)) }.getOrNull() }
}
