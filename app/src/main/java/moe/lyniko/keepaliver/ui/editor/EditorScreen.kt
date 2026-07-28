package moe.lyniko.keepaliver.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import moe.lyniko.keepaliver.ui.components.DelayedLoadingBox
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import moe.lyniko.keepaliver.data.model.ExecutionMode
import moe.lyniko.keepaliver.data.model.ExtraItem
import moe.lyniko.keepaliver.data.model.ExtraType
import moe.lyniko.keepaliver.data.model.IntentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onPickPackage: () -> Unit = {},
    onPickActivity: (packageName: String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    LaunchedEffect(state.testMessage) {
        state.testMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTestMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Intent") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    Button(onClick = { viewModel.save() }) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            DelayedLoadingBox(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Name") },
                isError = state.nameError,
                supportingText = if (state.nameError) {{ Text("Name is required") }} else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Intent Type
            Text("Intent Type", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                IntentType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.intentType == type,
                        onClick = { viewModel.updateIntentType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, IntentType.entries.size)
                    ) {
                        Text(type.name)
                    }
                }
            }

            // Target Package
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = state.targetPackage,
                    onValueChange = viewModel::updateTargetPackage,
                    label = { Text("Target Package") },
                    isError = state.packageError,
                    supportingText = if (state.packageError) {{ Text("Target package is required") }} else null,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onPickPackage,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Pick app"
                    )
                }
            }

            // Target Class
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = state.targetClass,
                    onValueChange = viewModel::updateTargetClass,
                    label = { Text("Target Class (optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (state.targetPackage.isNotBlank()) {
                            onPickActivity(state.targetPackage)
                        }
                    },
                    enabled = state.targetPackage.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Pick activity"
                    )
                }
            }

            // Action
            OutlinedTextField(
                value = state.action,
                onValueChange = viewModel::updateAction,
                label = { Text("Action (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Data URI
            OutlinedTextField(
                value = state.dataUri,
                onValueChange = viewModel::updateDataUri,
                label = { Text("Data URI (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category
            OutlinedTextField(
                value = state.category,
                onValueChange = viewModel::updateCategory,
                label = { Text("Category (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Flags
            OutlinedTextField(
                value = state.flags,
                onValueChange = viewModel::updateFlags,
                label = { Text("Flags (optional, numeric)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Foreground Service toggle (only for SERVICE type)
            if (state.intentType == IntentType.SERVICE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Start as Foreground Service", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.useForegroundService,
                        onCheckedChange = viewModel::updateUseForegroundService
                    )
                }
            }

            // Execution Mode
            Spacer(modifier = Modifier.height(8.dp))
            Text("Execution Mode", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "Leave as \"Default (NORMAL)\" to use normal execution without special permissions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                // Include a "Default" option (null) plus all explicit modes
                val modes = listOf<ExecutionMode?>(null) + ExecutionMode.entries
                modes.forEachIndexed { index, mode ->
                    val label = when (mode) {
                        null -> "Default"
                        else -> mode.name
                    }
                    SegmentedButton(
                        selected = state.executionMode == mode,
                        onClick = { viewModel.updateExecutionMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                    ) {
                        Text(label)
                    }
                }
            }

            // Extras section
            Spacer(modifier = Modifier.height(8.dp))
            Text("Extras", style = MaterialTheme.typography.labelLarge)

            state.extras.forEachIndexed { index, extra ->
                ExtraItemRow(
                    extra = extra,
                    onUpdate = { viewModel.updateExtra(index, it) },
                    onDelete = { viewModel.removeExtra(index) }
                )
            }

            OutlinedButton(
                onClick = viewModel::addExtra,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Extra")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.test() },
                enabled = !state.testing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp).width(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraItemRow(
    extra: ExtraItem,
    onUpdate: (ExtraItem) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = extra.type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ExtraType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    val normalizedValue = if (type == ExtraType.BOOLEAN) {
                                        (extra.value.toBooleanStrictOrNull() ?: false).toString()
                                    } else {
                                        extra.value
                                    }
                                    onUpdate(extra.copy(type = type, value = normalizedValue))
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove extra",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = extra.key,
                onValueChange = { onUpdate(extra.copy(key = it)) },
                label = { Text("Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            when (extra.type) {
                ExtraType.BOOLEAN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Value", modifier = Modifier.weight(1f))
                        val checked = extra.value.toBooleanStrictOrNull() ?: false
                        Switch(
                            checked = checked,
                            onCheckedChange = { onUpdate(extra.copy(value = it.toString())) }
                        )
                    }
                }
                ExtraType.STRING_ARRAY -> {
                    StringArrayValueInput(
                        value = extra.value,
                        onValueChange = { onUpdate(extra.copy(value = it)) }
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = extra.value,
                        onValueChange = { onUpdate(extra.copy(value = it)) },
                        label = { Text("Value") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StringArrayValueInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    val items = remember(value) { decodeStringArray(value) }
    var input by remember { mutableStateOf("") }

    fun addItem() {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        onValueChange(encodeStringArray(items + trimmed))
        input = ""
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (items.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEachIndexed { index, item ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = { Text(item) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier
                                    .size(InputChipDefaults.IconSize)
                                    .clickable {
                                        onValueChange(
                                            encodeStringArray(
                                                items.filterIndexed { i, _ -> i != index }
                                            )
                                        )
                                    }
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Add value") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addItem() }),
            trailingIcon = {
                IconButton(onClick = { addItem() }, enabled = input.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = "Add value")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun decodeStringArray(value: String): List<String> {
    if (value.isEmpty()) return emptyList()
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
    return result
}

private fun encodeStringArray(items: List<String>): String =
    items.joinToString(",") { it.replace(",", "\\,") }
