package com.phonedoctor.app.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.domain.model.MemoryBenchmarkResult
import com.phonedoctor.app.domain.model.MemoryInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemoryUiState(
    val info: MemoryInfo? = null,
    val benchmark: MemoryBenchmarkResult? = null,
    val benchmarkRunning: Boolean = false,
    val error: String? = null
)

class MemoryViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val info = serviceLocator.memoryRepository.getMemoryInfo()
                _uiState.update { it.copy(info = info, error = null) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _uiState.update {
                    it.copy(error = t.message ?: "Memory information unavailable")
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
                val result = serviceLocator.memoryBenchmarkEngine.run()
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
                        error = t.message ?: "Memory benchmark failed"
                    )
                }
            }
        }
    }
}
