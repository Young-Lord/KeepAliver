package moe.lyniko.keepaliver.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.lyniko.keepaliver.data.AppSettings
import moe.lyniko.keepaliver.data.SettingsStore
import moe.lyniko.keepaliver.data.model.ExecutionMode
import moe.lyniko.keepaliver.shizuku.ShizukuHelper

class SettingsViewModel(
    private val settingsStore: SettingsStore
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shizukuState: ShizukuHelper.PermissionState
        get() = ShizukuHelper.checkPermission()

    fun setBootTriggerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setBootTriggerEnabled(enabled) }
    }

    fun setTileTriggerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setTileTriggerEnabled(enabled) }
    }

    fun setSyncTriggerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setSyncTriggerEnabled(enabled) }
    }

    fun setExecutionMode(mode: ExecutionMode) {
        viewModelScope.launch { settingsStore.setExecutionMode(mode.name) }
    }

    fun setSyncInterval(minutes: Int) {
        viewModelScope.launch { settingsStore.setSyncIntervalMinutes(minutes) }
    }

    fun setLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setLoggingEnabled(enabled) }
    }

    fun requestShizukuPermission() {
        ShizukuHelper.checkPermission()
    }

    class Factory(private val settingsStore: SettingsStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsStore) as T
        }
    }
}
