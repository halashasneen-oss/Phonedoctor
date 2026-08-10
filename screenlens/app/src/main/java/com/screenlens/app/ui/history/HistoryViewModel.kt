package com.screenlens.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenlens.app.data.repository.HistoryRepository
import com.screenlens.app.domain.HistoryItem
import com.screenlens.app.domain.HistoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val historyRepository: HistoryRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filterType = MutableStateFlow<HistoryType?>(null)
    val filterType: StateFlow<HistoryType?> = _filterType.asStateFlow()

    val items: StateFlow<List<HistoryItem>> = combine(_query, _filterType) { q, type -> q to type }
        .flatMapLatest { (q, type) -> historyRepository.observe(type, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(query: String) {
        _query.value = query
    }

    fun setFilter(type: HistoryType?) {
        _filterType.value = type
    }

    fun delete(item: HistoryItem) {
        viewModelScope.launch { historyRepository.delete(item) }
    }

    fun clearAll() {
        viewModelScope.launch { historyRepository.clearAll() }
    }
}
