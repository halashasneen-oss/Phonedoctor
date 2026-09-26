package com.phonedoctor.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.domain.model.ScanMode
import com.phonedoctor.app.domain.model.ScanReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter {
    ALL,
    HEALTH,
    PERFORMANCE
}

class TestHistoryViewModel(
    private val serviceLocator: ServiceLocator
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)

    val history: StateFlow<List<ScanReport>> = combine(
        serviceLocator.historyRepository.history,
        filter
    ) { reports, selected ->
        when (selected) {
            HistoryFilter.ALL -> reports
            HistoryFilter.HEALTH -> reports.filter {
                it.scanMode != ScanMode.PERFORMANCE
            }
            HistoryFilter.PERFORMANCE -> reports.filter {
                it.scanMode == ScanMode.PERFORMANCE
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }

    fun clearHistory() {
        viewModelScope.launch {
            serviceLocator.historyRepository.clearAll()
        }
    }
}
