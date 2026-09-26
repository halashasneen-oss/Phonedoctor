package com.phonedoctor.app.ui.thermal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.domain.model.ThermalInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThermalViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _thermalInfo = MutableStateFlow<ThermalInfo?>(null)
    val thermalInfo: StateFlow<ThermalInfo?> = _thermalInfo.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _thermalInfo.value = serviceLocator.thermalRepository.getThermalInfo()
        }
    }
}
