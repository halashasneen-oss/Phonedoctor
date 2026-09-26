package com.phonedoctor.app.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.domain.model.StorageBenchmarkResult
import com.phonedoctor.app.domain.model.StorageInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StorageUiState(
    val info: StorageInfo? = null,
    val benchmark: StorageBenchmarkResult? = null,
    val benchmarkRunning: Boolean = false,
    val error: String? = null
)

class StorageViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val info = serviceLocator.storageRepository.getStorageInfo()
                _uiState.update { it.copy(info = info, error = null) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.update {
                    it.copy(error = t.message ?: "Storage information unavailable")
                }
            }
        }
    }

    fun runBenchmark() {
        if (_uiState.value.benchmarkRunning) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    benchmarkRunning = true,
                    benchmark = null,
                    error = null
                )
            }
            try {
                val result = serviceLocator.storageBenchmarkEngine.run()
                _uiState.update {
                    it.copy(
                        benchmarkRunning = false,
                        benchmark = result
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.update {
                    it.copy(
                        benchmarkRunning = false,
                        error = t.message ?: "Storage benchmark failed"
                    )
                }
            }
        }
    }
}
