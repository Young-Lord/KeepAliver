package moe.lyniko.keepaliver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.lyniko.keepaliver.KeepAliverApp
import moe.lyniko.keepaliver.executor.IntentExecutor

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val app = context.applicationContext as KeepAliverApp
                val settings = app.settingsStore.settingsFlow.first()

                if (!settings.bootTriggerEnabled) return@launch

                val entries = app.repository.getEnabledEntries()
                if (entries.isEmpty()) return@launch

                // Each entry uses its own execution mode (field executionMode),
                // falling back to NORMAL if not set
                IntentExecutor.executeAll(context, entries)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
