package com.phonedoctor.app.ui.cpu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.domain.model.CpuBenchmarkResult
import com.phonedoctor.app.domain.model.CpuInfo
import com.phonedoctor.app.domain.model.CpuStressResult
import com.phonedoctor.app.domain.util.CpuBenchmarkMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpuLabUiState(
    val info: CpuInfo? = null,
    val benchmark: CpuBenchmarkResult? = null,
    val stress: CpuStressResult? = null,
    val benchmarkRunning: Boolean = false,
    val stressRunning: Boolean = false,
    val stressProgress: Int = 0,
    val error: String? = null
)

class CpuViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _uiState = MutableStateFlow(CpuLabUiState())
    val uiState: StateFlow<CpuLabUiState> = _uiState.asStateFlow()

    init {
        refreshInfo()
    }

    fun refreshInfo() {
        viewModelScope.launch {
            try {
                val info = serviceLocator.cpuRepository.getCpuInfo()
                _uiState.update { it.copy(info = info, error = null) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = t.message ?: "CPU information unavailable") }
            }
        }
    }

    fun runBenchmark() {
        val info = _uiState.value.info ?: return
        if (_uiState.value.benchmarkRunning || _uiState.value.stressRunning) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    benchmarkRunning = true,
                    benchmark = null,
                    error = null
                )
            }
            try {
                val result = serviceLocator.cpuBenchmarkEngine.runBenchmark(info.coreCount)
                _uiState.update {
                    it.copy(
                        benchmarkRunning = false,
                        benchmark = result
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        benchmarkRunning = false,
                        error = t.message ?: "CPU benchmark failed"
                    )
                }
            }
        }
    }

    fun runStressTest() {
        val info = _uiState.value.info ?: return
        if (_uiState.value.benchmarkRunning || _uiState.value.stressRunning) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stressRunning = true,
                    stressProgress = 0,
                    stress = null,
                    error = null
                )
            }

            try {
                val before = serviceLocator.thermalRepository.getThermalInfo()
                val samples = serviceLocator.cpuBenchmarkEngine.runStress(
                    coreCount = info.coreCount
                ) { progress ->
                    _uiState.update { state -> state.copy(stressProgress = progress) }
                }
                val after = serviceLocator.thermalRepository.getThermalInfo()

                val firstWindow = samples
                    .take(2)
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.toLong()
                    ?: 0L
                val lastWindow = samples
                    .takeLast(2)
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.toLong()
                    ?: 0L

                val result = CpuStressResult(
                    durationSeconds = samples.size,
                    firstWindowOpsPerSecond = firstWindow,
                    lastWindowOpsPerSecond = lastWindow,
                    sustainedPerformancePercent = CpuBenchmarkMath.sustainedPerformancePercent(
                        firstOpsPerSecond = firstWindow,
                        lastOpsPerSecond = lastWindow
                    ),
                    initialThermalState = before.state,
                    finalThermalState = after.state,
                    initialThermalHeadroom = before.currentHeadroom,
                    finalThermalHeadroom = after.currentHeadroom
                )

                _uiState.update {
                    it.copy(
                        stressRunning = false,
                        stressProgress = 100,
                        stress = result
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        stressRunning = false,
                        error = t.message ?: "CPU stress test failed"
                    )
                }
            }
        }
    }
}
