package com.screenlens.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenlens.app.data.repository.HistoryRepository
import com.screenlens.app.domain.HistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(historyRepository: HistoryRepository) : ViewModel() {

    val recentScans: StateFlow<List<HistoryItem>> = historyRepository.observeRecent(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
