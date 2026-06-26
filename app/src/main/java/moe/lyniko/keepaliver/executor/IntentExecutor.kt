package moe.lyniko.keepaliver.executor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.first
import moe.lyniko.keepaliver.data.SettingsStore
import moe.lyniko.keepaliver.data.db.IntentEntry
import moe.lyniko.keepaliver.data.model.ExecutionMode
import moe.lyniko.keepaliver.data.model.ExtraItem
import moe.lyniko.keepaliver.data.model.ExtraType
import moe.lyniko.keepaliver.data.model.IntentType
import moe.lyniko.keepaliver.shizuku.ShizukuProcess

object IntentExecutor {

    private const val TAG = "IntentExecutor"

    @Volatile
    private var loggingEnabled = false

    suspend fun executeAll(
        context: Context,
        entries: List<IntentEntry>,
        mode: ExecutionMode,
        onEntryExecuted: ((IntentEntry, Result<Unit>) -> Unit)? = null
    ) {
        loggingEnabled = runCatching {
            SettingsStore(context.applicationContext).settingsFlow.first().loggingEnabled
        }.getOrDefault(false)

        logI { "executeAll: count=${entries.size}, mode=$mode" }
        entries.forEach { entry ->
            val result = executeSingle(context, entry, mode)
            onEntryExecuted?.invoke(entry, result)
        }
    }

    private suspend fun executeSingle(
        context: Context,
        entry: IntentEntry,
        mode: ExecutionMode
    ): Result<Unit> {
        logI { "executeSingle start: mode=$mode, entry=${describeEntry(entry)}" }
        val result = runCatching {
            when (mode) {
                ExecutionMode.NORMAL -> executeNormal(context, entry)
                ExecutionMode.SHIZUKU -> executeShell(entry)
                ExecutionMode.ROOT -> executeRoot(entry)
            }
        }
        result.fold(
            onSuccess = {
                logI { "executeSingle success: id=${entry.id}, name=${entry.name}" }
            },
            onFailure = { e ->
                if (loggingEnabled) {
                    Log.e(TAG, "executeSingle failed: id=${entry.id}, name=${entry.name}", e)
                }
            }
        )
        return result
    }

    private inline fun logI(message: () -> String) {
        if (loggingEnabled) Log.i(TAG, message())
    }

    private inline fun logD(message: () -> String) {
        if (loggingEnabled) Log.d(TAG, message())
    }

    private fun describeEntry(entry: IntentEntry): String = buildString {
        append("id=").append(entry.id)
        append(", name=").append(entry.name)
        append(", type=").append(entry.intentType)
        append(", package=").append(entry.targetPackage)
        append(", class=").append(entry.targetClass)
        append(", action=").append(entry.action)
        append(", data=").append(entry.dataUri)
        append(", category=").append(entry.category)
        append(", flags=").append(entry.flags)
        append(", fgService=").append(entry.useForegroundService)
        append(", extras=").append(entry.extrasJson)
    }

    private fun executeNormal(context: Context, entry: IntentEntry) {
        val intent = buildIntent(context, entry)

        when (entry.intentType) {
            IntentType.ACTIVITY -> {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                logD { "NORMAL startActivity: $intent" }
                context.startActivity(intent)
            }
            IntentType.BROADCAST -> {
                logD { "NORMAL sendBroadcast: $intent" }
                context.sendBroadcast(intent)
            }
            IntentType.SERVICE -> {
                if (entry.useForegroundService) {
                    logD { "NORMAL startForegroundService: $intent" }
                    context.startForegroundService(intent)
                } else {
                    logD { "NORMAL startService: $intent" }
                    context.startService(intent)
                }
            }
        }
    }

    private suspend fun executeShell(entry: IntentEntry) {
        val cmd = ShellCommandBuilder.build(entry)
        logD { "SHIZUKU exec: $cmd" }
        ShizukuProcess.execute(cmd)
    }

    private suspend fun executeRoot(entry: IntentEntry) {
        val cmd = ShellCommandBuilder.build(entry)
        logD { "ROOT exec: su -c $cmd" }
        runCatching {
            val process = java.lang.Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val exitCode = process.waitFor()
            logD { "ROOT exitCode=$exitCode for id=${entry.id}" }
            process.destroy()
        }.getOrThrow()
    }

    private fun buildIntent(context: Context, entry: IntentEntry): Intent {
        val intent = Intent()

        // Component
        if (!entry.targetClass.isNullOrBlank()) {
            intent.component = ComponentName(entry.targetPackage, entry.targetClass)
        } else {
            intent.setPackage(entry.targetPackage)
        }

        // Action
        if (!entry.action.isNullOrBlank()) {
            intent.action = entry.action
        }

        // Data URI
        if (!entry.dataUri.isNullOrBlank()) {
            intent.data = Uri.parse(entry.dataUri)
        }

        // Category
        if (!entry.category.isNullOrBlank()) {
            intent.addCategory(entry.category)
        }

        // Flags
        if (entry.flags != null) {
            intent.flags = entry.flags
        }

        // Extras
        val extras = IntentExtrasParser.parse(entry.extrasJson)
        extras.forEach { item ->
            putExtra(intent, item)
        }

        return intent
    }

    private fun putExtra(intent: Intent, extra: ExtraItem) {
        try {
            when (extra.type) {
                ExtraType.STRING -> intent.putExtra(extra.key, extra.value)
                ExtraType.INT -> intent.putExtra(extra.key, extra.value.toInt())
                ExtraType.LONG -> intent.putExtra(extra.key, extra.value.toLong())
                ExtraType.FLOAT -> intent.putExtra(extra.key, extra.value.toFloat())
                ExtraType.DOUBLE -> intent.putExtra(extra.key, extra.value.toDouble())
                ExtraType.BOOLEAN -> intent.putExtra(extra.key, extra.value.toBoolean())
                ExtraType.URI -> intent.putExtra(extra.key, Uri.parse(extra.value))
                ExtraType.STRING_ARRAY -> intent.putExtra(extra.key, splitStringArray(extra.value))
            }
        } catch (e: Exception) {
            if (loggingEnabled) {
                Log.w(TAG, "putExtra skipped: key=${extra.key}, type=${extra.type}, value=${extra.value}", e)
            }
        }
    }

    // Splits a comma-separated value into elements, matching `am --esa` semantics
    // where a literal comma is escaped as "\,".
    private fun splitStringArray(value: String): Array<String> {
        if (value.isEmpty()) return emptyArray()
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length && value[i + 1] == ',') {
                current.append(',')
                i += 2
            } else if (c == ',') {
                result.add(current.toString())
                current.setLength(0)
                i++
            } else {
                current.append(c)
                i++
            }
        }
        result.add(current.toString())
        return result.toTypedArray()
    }
}
