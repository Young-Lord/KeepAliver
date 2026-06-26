package moe.lyniko.keepaliver.sync

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.lyniko.keepaliver.KeepAliverApp
import moe.lyniko.keepaliver.data.model.ExecutionMode
import moe.lyniko.keepaliver.executor.IntentExecutor

class SyncAdapter(
    context: Context,
    autoInitialize: Boolean,
    allowParallelSyncs: Boolean
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onPerformSync(
        account: Account,
        extras: Bundle,
        authority: String,
        provider: ContentProviderClient?,
        syncResult: SyncResult
    ) {
        val app = context.applicationContext as KeepAliverApp

        scope.launch {
            try {
                val settings = app.settingsStore.settingsFlow.first()
                if (!settings.syncTriggerEnabled) return@launch

                val entries = app.repository.getEnabledEntries()
                if (entries.isEmpty()) return@launch

                val mode = runCatching {
                    ExecutionMode.valueOf(settings.executionMode)
                }.getOrDefault(ExecutionMode.NORMAL)

                IntentExecutor.executeAll(context, entries, mode)
            } catch (_: Exception) {
                // Sync errors are handled by the sync framework
            }
        }
    }
}
