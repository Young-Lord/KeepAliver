package moe.lyniko.keepaliver.ui.settings

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import moe.lyniko.keepaliver.R
import moe.lyniko.keepaliver.ui.components.DelayedLoadingBox
import moe.lyniko.keepaliver.service.KeepAliveTileService
import moe.lyniko.keepaliver.shizuku.ShizukuHelper

data class SyncIntervalOption(val label: String, val minutes: Int)

private val syncIntervals = listOf(
    SyncIntervalOption("15 minutes", 15),
    SyncIntervalOption("30 minutes", 30),
    SyncIntervalOption("1 hour", 60),
    SyncIntervalOption("6 hours", 360),
    SyncIntervalOption("12 hours", 720),
    SyncIntervalOption("24 hours", 1440),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        val currentSettings = settings
        if (currentSettings == null) {
            DelayedLoadingBox(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Triggers section
            Text("Triggers", style = MaterialTheme.typography.titleMedium)
            SettingsToggle(
                title = "Boot Completed",
                description = "Fire intents when device boots",
                checked = currentSettings.bootTriggerEnabled,
                onCheckedChange = viewModel::setBootTriggerEnabled
            )
            SettingsToggle(
                title = "Quick Settings Tile",
                description = "Fire intents when QS tile becomes visible",
                checked = currentSettings.tileTriggerEnabled,
                onCheckedChange = viewModel::setTileTriggerEnabled
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OutlinedButton(
                    onClick = {
                        val statusBarManager =
                            context.getSystemService(StatusBarManager::class.java)
                        statusBarManager?.requestAddTileService(
                            ComponentName(context, KeepAliveTileService::class.java),
                            context.getString(R.string.app_name),
                            Icon.createWithResource(context, R.drawable.ic_qs_keepalive),
                            context.mainExecutor
                        ) { result ->
                            val message = when (result) {
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                                    "Tile added to Quick Settings"
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                                    "Tile already added"
                                else -> null
                            }
                            message?.let {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Tile to Quick Settings")
                }
            }
            SettingsToggle(
                title = "Account Sync",
                description = "Fire intents on periodic account sync",
                checked = currentSettings.syncTriggerEnabled,
                onCheckedChange = viewModel::setSyncTriggerEnabled
            )

            // Sync Interval (only shown when sync trigger enabled)
            if (currentSettings.syncTriggerEnabled) {
                SyncIntervalPicker(
                    currentMinutes = currentSettings.syncIntervalMinutes,
                    onIntervalSelected = viewModel::setSyncInterval
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Shizuku authorization section
            Text("Shizuku Authorization", style = MaterialTheme.typography.titleMedium)

            val shizukuState = viewModel.shizukuState
            val statusText = when (shizukuState) {
                ShizukuHelper.PermissionState.GRANTED -> "Ready"
                ShizukuHelper.PermissionState.NOT_READY -> "Not Ready"
                ShizukuHelper.PermissionState.DENIED -> "Permission Denied"
                ShizukuHelper.PermissionState.REQUESTED -> "Permission Requested"
            }
            val statusColor = when (shizukuState) {
                ShizukuHelper.PermissionState.GRANTED -> MaterialTheme.colorScheme.primary
                ShizukuHelper.PermissionState.NOT_READY -> MaterialTheme.colorScheme.error
                ShizukuHelper.PermissionState.DENIED -> MaterialTheme.colorScheme.error
                ShizukuHelper.PermissionState.REQUESTED -> MaterialTheme.colorScheme.tertiary
            }

            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status: $statusText",
                        color = statusColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (shizukuState == ShizukuHelper.PermissionState.NOT_READY ||
                        shizukuState == ShizukuHelper.PermissionState.DENIED
                    ) {
                        androidx.compose.material3.Button(
                            onClick = { viewModel.requestShizukuPermission() }
                        ) {
                            Text("Request")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Debug section
            Text("Debug", style = MaterialTheme.typography.titleMedium)
            SettingsToggle(
                title = "Verbose Logging",
                description = "Log intent content and results to logcat (tag: IntentExecutor)",
                checked = currentSettings.loggingEnabled,
                onCheckedChange = viewModel::setLoggingEnabled
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncIntervalPicker(
    currentMinutes: Int,
    onIntervalSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = syncIntervals.find { it.minutes == currentMinutes }?.label ?: "1 hour"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sync Interval") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            syncIntervals.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onIntervalSelected(option.minutes)
                        expanded = false
                    }
                )
            }
        }
    }
}
