package moe.lyniko.keepaliver.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import moe.lyniko.keepaliver.sync.SyncAdapter

class SyncService : Service() {

    private val syncAdapter by lazy {
        SyncAdapter(this, autoInitialize = false, allowParallelSyncs = false)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return syncAdapter.syncAdapterBinder
    }
}
