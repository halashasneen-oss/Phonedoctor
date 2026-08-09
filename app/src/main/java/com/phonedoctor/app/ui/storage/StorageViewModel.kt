package com.phonedoctor.app.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.domain.model.StorageBreakdown
import com.phonedoctor.app.domain.model.StorageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StorageViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    val storageInfo: StateFlow<StorageInfo?> = _storageInfo.asStateFlow()

    private val _breakdown = MutableStateFlow<StorageBreakdown?>(null)
    val breakdown: StateFlow<StorageBreakdown?> = _breakdown.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _storageInfo.value = serviceLocator.storageRepository.getStorageInfo()
        }
    }

    fun loadBreakdown() {
        viewModelScope.launch {
            _breakdown.value = serviceLocator.storageRepository.getStorageBreakdown()
        }
    }
}
