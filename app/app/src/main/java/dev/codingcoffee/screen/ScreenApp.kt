package dev.codingcoffee.screen

import android.app.Application
import android.content.Context
import android.util.Log
import dev.codingcoffee.screen.data.CallLogStore
import dev.codingcoffee.screen.data.Settings
import dev.codingcoffee.screen.data.SettingsStore
import dev.codingcoffee.screen.rules.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

class ScreenApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settingsStore: SettingsStore
        private set
    lateinit var callLog: CallLogStore
        private set
    lateinit var settings: StateFlow<Settings?>
        private set
    lateinit var engine: StateFlow<RuleEngine?>
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        callLog = CallLogStore(File(filesDir, "call_log.jsonl"), scope)
        settings = settingsStore.settings
            .catch {
                Log.e(TAG, "Failed to load settings, allowing all calls", it)
                emit(Settings(rules = emptyList(), blockUnknown = false, onboarded = true))
            }
            .stateIn(scope, SharingStarted.Eagerly, null)
        engine = settings.filterNotNull()
            .map { RuleEngine(it.rules, it.blockUnknown, it.spamNumbers) }
            .stateIn(scope, SharingStarted.Eagerly, null)
    }

    companion object {
        const val TAG = "Screen"
    }
}

val Context.app: ScreenApp get() = applicationContext as ScreenApp
