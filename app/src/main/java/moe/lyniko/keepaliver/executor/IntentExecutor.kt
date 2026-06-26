package moe.lyniko.keepaliver.executor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import moe.lyniko.keepaliver.data.db.IntentEntry
import moe.lyniko.keepaliver.data.model.ExecutionMode
import moe.lyniko.keepaliver.data.model.ExtraItem
import moe.lyniko.keepaliver.data.model.ExtraType
import moe.lyniko.keepaliver.data.model.IntentType
import moe.lyniko.keepaliver.shizuku.ShizukuProcess

object IntentExecutor {

    suspend fun executeAll(
        context: Context,
        entries: List<IntentEntry>,
        mode: ExecutionMode,
        onEntryExecuted: ((IntentEntry, Result<Unit>) -> Unit)? = null
    ) {
        entries.forEach { entry ->
            val result = executeSingle(context, entry, mode)
            onEntryExecuted?.invoke(entry, result)
        }
    }

    private suspend fun executeSingle(
        context: Context,
        entry: IntentEntry,
        mode: ExecutionMode
    ): Result<Unit> = runCatching {
        when (mode) {
            ExecutionMode.NORMAL -> executeNormal(context, entry)
            ExecutionMode.SHIZUKU -> executeShell(entry)
            ExecutionMode.ROOT -> executeRoot(entry)
        }
    }

    private fun executeNormal(context: Context, entry: IntentEntry) {
        val intent = buildIntent(context, entry)

        when (entry.intentType) {
            IntentType.ACTIVITY -> {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            IntentType.BROADCAST -> {
                context.sendBroadcast(intent)
            }
            IntentType.SERVICE -> {
                if (entry.useForegroundService) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }

    private suspend fun executeShell(entry: IntentEntry) {
        val cmd = ShellCommandBuilder.build(entry)
        ShizukuProcess.execute(cmd)
    }

    private suspend fun executeRoot(entry: IntentEntry) {
        val cmd = ShellCommandBuilder.build(entry)
        runCatching {
            val process = java.lang.Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            process.waitFor()
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
            }
        } catch (_: Exception) {
            // Skip extras that fail to parse
        }
    }
}
