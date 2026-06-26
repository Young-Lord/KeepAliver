package moe.lyniko.keepaliver.ui.editor

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.lyniko.keepaliver.data.SettingsStore
import moe.lyniko.keepaliver.data.db.IntentEntry
import moe.lyniko.keepaliver.data.model.ExecutionMode
import moe.lyniko.keepaliver.data.model.ExtraItem
import moe.lyniko.keepaliver.data.model.ExtraType
import moe.lyniko.keepaliver.data.model.IntentType
import moe.lyniko.keepaliver.data.repository.IntentRepository
import moe.lyniko.keepaliver.executor.IntentExecutor
import moe.lyniko.keepaliver.executor.IntentExtrasParser

data class EditorUiState(
    val name: String = "",
    val intentType: IntentType = IntentType.ACTIVITY,
    val targetPackage: String = "",
    val targetClass: String = "",
    val action: String = "",
    val dataUri: String = "",
    val category: String = "",
    val flags: String = "",
    val extras: List<ExtraItem> = emptyList(),
    val useForegroundService: Boolean = false,
    val nameError: Boolean = false,
    val packageError: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val testing: Boolean = false,
    val testMessage: String? = null
)

class EditorViewModel(
    private val application: Application,
    private val repository: IntentRepository,
    private val settingsStore: SettingsStore,
    private val entryId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(isLoading = entryId != -1L))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        if (entryId != -1L) {
            loadEntry(entryId)
        }
    }

    private fun loadEntry(id: Long) {
        viewModelScope.launch {
            val entry = repository.getEntryById(id)
            if (entry == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }
            _uiState.value = EditorUiState(
                name = entry.name,
                intentType = entry.intentType,
                targetPackage = entry.targetPackage,
                targetClass = entry.targetClass ?: "",
                action = entry.action ?: "",
                dataUri = entry.dataUri ?: "",
                category = entry.category ?: "",
                flags = entry.flags?.toString() ?: "",
                extras = IntentExtrasParser.parse(entry.extrasJson),
                useForegroundService = entry.useForegroundService,
                isLoading = false
            )
        }
    }

    fun updateName(name: String) { _uiState.value = _uiState.value.copy(name = name, nameError = false) }
    fun updateIntentType(type: IntentType) { _uiState.value = _uiState.value.copy(intentType = type) }
    fun updateTargetPackage(pkg: String) { _uiState.value = _uiState.value.copy(targetPackage = pkg, packageError = false) }
    fun updateTargetClass(cls: String) { _uiState.value = _uiState.value.copy(targetClass = cls) }
    fun updateAction(action: String) { _uiState.value = _uiState.value.copy(action = action) }
    fun updateDataUri(uri: String) { _uiState.value = _uiState.value.copy(dataUri = uri) }
    fun updateCategory(category: String) { _uiState.value = _uiState.value.copy(category = category) }
    fun updateFlags(flags: String) { _uiState.value = _uiState.value.copy(flags = flags) }
    fun updateUseForegroundService(value: Boolean) { _uiState.value = _uiState.value.copy(useForegroundService = value) }

    fun addExtra() {
        val extras = _uiState.value.extras + ExtraItem("", "", ExtraType.STRING)
        _uiState.value = _uiState.value.copy(extras = extras)
    }

    fun updateExtra(index: Int, extra: ExtraItem) {
        val extras = _uiState.value.extras.toMutableList()
        if (index in extras.indices) {
            extras[index] = extra
            _uiState.value = _uiState.value.copy(extras = extras)
        }
    }

    fun removeExtra(index: Int) {
        val extras = _uiState.value.extras.toMutableList()
        if (index in extras.indices) {
            extras.removeAt(index)
            _uiState.value = _uiState.value.copy(extras = extras)
        }
    }

    fun save() {
        val state = _uiState.value
        var hasError = false

        if (state.name.isBlank()) {
            _uiState.value = state.copy(nameError = true)
            hasError = true
        }
        if (state.targetPackage.isBlank()) {
            _uiState.value = _uiState.value.copy(packageError = true)
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            val entry = IntentEntry(
                id = if (entryId == -1L) 0 else entryId,
                name = state.name.trim(),
                intentType = state.intentType,
                targetPackage = state.targetPackage.trim(),
                targetClass = state.targetClass.trim().ifBlank { null },
                action = state.action.trim().ifBlank { null },
                dataUri = state.dataUri.trim().ifBlank { null },
                category = state.category.trim().ifBlank { null },
                flags = state.flags.trim().toIntOrNull(),
                extrasJson = IntentExtrasParser.toJson(state.extras),
                useForegroundService = state.useForegroundService,
                enabled = true
            )

            if (entryId == -1L) {
                repository.insertEntry(entry)
            } else {
                repository.updateEntry(entry)
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun test() {
        val state = _uiState.value
        if (state.testing) return

        if (state.targetPackage.isBlank()) {
            _uiState.value = state.copy(
                packageError = true,
                testMessage = "Target package is required to test"
            )
            return
        }

        val entry = IntentEntry(
            id = 0,
            name = state.name.trim().ifBlank { "Test" },
            intentType = state.intentType,
            targetPackage = state.targetPackage.trim(),
            targetClass = state.targetClass.trim().ifBlank { null },
            action = state.action.trim().ifBlank { null },
            dataUri = state.dataUri.trim().ifBlank { null },
            category = state.category.trim().ifBlank { null },
            flags = state.flags.trim().toIntOrNull(),
            extrasJson = IntentExtrasParser.toJson(state.extras),
            useForegroundService = state.useForegroundService,
            enabled = true
        )

        _uiState.value = state.copy(testing = true, testMessage = null)
        viewModelScope.launch {
            val mode = runCatching {
                ExecutionMode.valueOf(settingsStore.settingsFlow.first().executionMode)
            }.getOrDefault(ExecutionMode.NORMAL)

            var failure: Throwable? = null
            IntentExecutor.executeAll(application, listOf(entry), mode) { _, result ->
                result.onFailure { failure = it }
            }
            val err = failure
            val message = err?.let { "Test failed: ${it.message ?: it.javaClass.simpleName}" }
            _uiState.value = _uiState.value.copy(testing = false, testMessage = message)
        }
    }

    fun clearTestMessage() {
        _uiState.value = _uiState.value.copy(testMessage = null)
    }

    class Factory(
        private val application: Application,
        private val repository: IntentRepository,
        private val settingsStore: SettingsStore,
        private val entryId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(application, repository, settingsStore, entryId) as T
        }
    }
}
