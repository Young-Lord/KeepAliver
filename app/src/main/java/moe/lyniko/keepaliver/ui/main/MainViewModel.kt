package moe.lyniko.keepaliver.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.lyniko.keepaliver.data.db.IntentEntry
import moe.lyniko.keepaliver.data.repository.IntentRepository

class MainViewModel(
    private val repository: IntentRepository
) : ViewModel() {

    val entries: StateFlow<List<IntentEntry>> = repository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleEnabled(entry: IntentEntry) {
        viewModelScope.launch {
            repository.updateEntry(entry.copy(enabled = !entry.enabled))
        }
    }

    fun deleteEntry(entry: IntentEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    class Factory(private val repository: IntentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
