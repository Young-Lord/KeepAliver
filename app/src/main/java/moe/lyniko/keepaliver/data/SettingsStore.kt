package moe.lyniko.keepaliver.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val bootTriggerEnabled: Boolean = true,
    val tileTriggerEnabled: Boolean = true,
    val syncTriggerEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 60,
    val loggingEnabled: Boolean = false
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val BOOT_TRIGGER = booleanPreferencesKey("boot_trigger_enabled")
        val TILE_TRIGGER = booleanPreferencesKey("tile_trigger_enabled")
        val SYNC_TRIGGER = booleanPreferencesKey("sync_trigger_enabled")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval_minutes")
        val LOGGING_ENABLED = booleanPreferencesKey("logging_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            bootTriggerEnabled = prefs[Keys.BOOT_TRIGGER] ?: true,
            tileTriggerEnabled = prefs[Keys.TILE_TRIGGER] ?: true,
            syncTriggerEnabled = prefs[Keys.SYNC_TRIGGER] ?: true,
            syncIntervalMinutes = prefs[Keys.SYNC_INTERVAL] ?: 60,
            loggingEnabled = prefs[Keys.LOGGING_ENABLED] ?: false
        )
    }

    suspend fun setBootTriggerEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BOOT_TRIGGER] = enabled
        }
    }

    suspend fun setTileTriggerEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TILE_TRIGGER] = enabled
        }
    }

    suspend fun setSyncTriggerEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYNC_TRIGGER] = enabled
        }
    }

    suspend fun setSyncIntervalMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYNC_INTERVAL] = minutes
        }
    }

    suspend fun setLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOGGING_ENABLED] = enabled
        }
    }
}
