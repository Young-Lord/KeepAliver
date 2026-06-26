package moe.lyniko.keepaliver.service

import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.lyniko.keepaliver.KeepAliverApp
import moe.lyniko.keepaliver.data.model.ExecutionMode
import moe.lyniko.keepaliver.executor.IntentExecutor

class KeepAliveTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        triggerIntents()
    }

    private fun triggerIntents() {
        scope.launch {
            try {
                val app = applicationContext as KeepAliverApp
                val settings = app.settingsStore.settingsFlow.first()

                if (!settings.tileTriggerEnabled) return@launch

                val entries = app.repository.getEnabledEntries()
                if (entries.isEmpty()) return@launch

                val mode = runCatching {
                    ExecutionMode.valueOf(settings.executionMode)
                }.getOrDefault(ExecutionMode.NORMAL)

                IntentExecutor.executeAll(this@KeepAliveTileService, entries, mode)
            } catch (_: Exception) {
                // Silently handle errors from tile execution
            }
        }
    }
}
