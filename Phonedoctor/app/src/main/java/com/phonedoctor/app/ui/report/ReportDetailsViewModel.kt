package com.phonedoctor.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.domain.model.ScanReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportDetailsViewModel(
    private val serviceLocator: ServiceLocator,
    private val reportId: Long
) : ViewModel() {

    private val _report = MutableStateFlow<ScanReport?>(null)
    val report: StateFlow<ScanReport?> = _report.asStateFlow()

    init {
        viewModelScope.launch {
            _report.value = serviceLocator.historyRepository.getById(reportId)
        }
    }
}
