package moe.lyniko.keepaliver.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle

object SyncAccountHelper {

    private const val ACCOUNT_NAME = "keepaliver"
    const val ACCOUNT_TYPE = "moe.lyniko.keepaliver"
    const val AUTHORITY = "moe.lyniko.keepaliver.provider"

    fun ensureAccount(context: Context): Account {
        val accountManager = AccountManager.get(context)
        val account = Account(ACCOUNT_NAME, ACCOUNT_TYPE)

        if (!accountManager.addAccountExplicitly(account, null, null)) {
            // Account already exists — that's fine
        }

        ContentResolver.setIsSyncable(account, AUTHORITY, 1)
        return account
    }

    fun setSyncAutomatically(context: Context, enabled: Boolean) {
        val account = Account(ACCOUNT_NAME, ACCOUNT_TYPE)
        ContentResolver.setSyncAutomatically(account, AUTHORITY, enabled)

        if (enabled) {
            ContentResolver.setIsSyncable(account, AUTHORITY, 1)
        }
    }

    fun updateSyncInterval(context: Context, intervalMinutes: Long) {
        val account = Account(ACCOUNT_NAME, ACCOUNT_TYPE)
        ContentResolver.setIsSyncable(account, AUTHORITY, 1)

        // Remove existing periodic sync
        ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle.EMPTY)

        // Add new periodic sync (Android enforces minimum of ~60s for testing, but
        // the actual minimum is ~15 minutes on most devices)
        ContentResolver.addPeriodicSync(
            account,
            AUTHORITY,
            Bundle.EMPTY,
            intervalMinutes
        )
    }

    fun isSyncActive(context: Context): Boolean {
        val account = Account(ACCOUNT_NAME, ACCOUNT_TYPE)
        return ContentResolver.getSyncAutomatically(account, AUTHORITY)
    }
}
