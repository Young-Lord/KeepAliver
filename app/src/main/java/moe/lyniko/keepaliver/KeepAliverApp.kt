package moe.lyniko.keepaliver

import android.app.Application
import moe.lyniko.keepaliver.data.SettingsStore
import moe.lyniko.keepaliver.data.db.AppDatabase
import moe.lyniko.keepaliver.data.repository.IntentRepository
import moe.lyniko.keepaliver.sync.SyncAccountHelper

class KeepAliverApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: IntentRepository
        private set
    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        repository = IntentRepository(database.intentEntryDao())
        settingsStore = SettingsStore(this)

        SyncAccountHelper.ensureAccount(this)
    }

    companion object {
        lateinit var instance: KeepAliverApp
            private set
    }
}
