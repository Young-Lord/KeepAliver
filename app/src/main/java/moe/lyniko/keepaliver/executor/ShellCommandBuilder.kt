package moe.lyniko.keepaliver.executor

import moe.lyniko.keepaliver.data.db.IntentEntry
import moe.lyniko.keepaliver.data.model.IntentType

object ShellCommandBuilder {

    fun build(entry: IntentEntry): String {
        val sb = StringBuilder()

        when (entry.intentType) {
            IntentType.ACTIVITY -> sb.append("am start")
            IntentType.BROADCAST -> sb.append("am broadcast")
            IntentType.SERVICE -> {
                if (entry.useForegroundService) {
                    sb.append("am start-foreground-service")
                } else {
                    sb.append("am startservice")
                }
            }
        }

        // Component
        val component = if (!entry.targetClass.isNullOrBlank()) {
            "${entry.targetPackage}/${entry.targetClass}"
        } else {
            entry.targetPackage
        }
        sb.append(" -n ").append(escapeShell(component))

        // Action
        if (!entry.action.isNullOrBlank()) {
            sb.append(" -a ").append(escapeShell(entry.action))
        }

        // Data URI
        if (!entry.dataUri.isNullOrBlank()) {
            sb.append(" -d ").append(escapeShell(entry.dataUri))
        }

        // Category
        if (!entry.category.isNullOrBlank()) {
            sb.append(" -c ").append(escapeShell(entry.category))
        }

        // Flags
        if (entry.flags != null) {
            sb.append(" -f ").append(entry.flags)
        }

        // Extras
        val extras = IntentExtrasParser.parse(entry.extrasJson)
        extras.forEach { item ->
            when (item.type) {
                moe.lyniko.keepaliver.data.model.ExtraType.STRING ->
                    sb.append(" --es ").append(escapeShell(item.key)).append(" ").append(escapeShell(item.value))
                moe.lyniko.keepaliver.data.model.ExtraType.INT ->
                    sb.append(" --ei ").append(escapeShell(item.key)).append(" ").append(item.value)
                moe.lyniko.keepaliver.data.model.ExtraType.LONG ->
                    sb.append(" --el ").append(escapeShell(item.key)).append(" ").append(item.value)
                moe.lyniko.keepaliver.data.model.ExtraType.FLOAT ->
                    sb.append(" --ef ").append(escapeShell(item.key)).append(" ").append(item.value)
                moe.lyniko.keepaliver.data.model.ExtraType.DOUBLE ->
                    sb.append(" --ed ").append(escapeShell(item.key)).append(" ").append(item.value)
                moe.lyniko.keepaliver.data.model.ExtraType.BOOLEAN ->
                    sb.append(" --ez ").append(escapeShell(item.key)).append(" ").append(escapeShell(item.value))
                moe.lyniko.keepaliver.data.model.ExtraType.URI ->
                    sb.append(" --eu ").append(escapeShell(item.key)).append(" ").append(escapeShell(item.value))
            }
        }

        return sb.toString()
    }

    private fun escapeShell(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }
}
