package com.phonedoctor.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.domain.model.ScanReport
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TestHistoryViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    val history: StateFlow<List<ScanReport>> = serviceLocator.historyRepository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { serviceLocator.historyRepository.clearAll() }
    }
}
