package com.phonedoctor.app.ui.cpu

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentCpuBinding
import com.phonedoctor.app.domain.model.CpuInfo
import com.phonedoctor.app.domain.model.ThermalState
import com.phonedoctor.app.ui.common.InfoRow
import com.phonedoctor.app.ui.common.InfoRowAdapter
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import java.text.NumberFormat
import kotlinx.coroutines.launch

class CpuFragment : Fragment(R.layout.fragment_cpu) {

    private val binding by viewBinding(FragmentCpuBinding::bind)

    private val viewModel: CpuViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                @Suppress("UNCHECKED_CAST")
                return CpuViewModel(serviceLocator()) as T
            }
        }
    }

    private val adapter = InfoRowAdapter()
    private val numberFormat = NumberFormat.getIntegerInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerInfo.adapter = adapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonRefresh.setOnClickListener { viewModel.refreshInfo() }
        binding.buttonRunBenchmark.setOnClickListener { viewModel.runBenchmark() }
        binding.buttonRunStress.setOnClickListener { viewModel.runStressTest() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: CpuLabUiState) {
        state.info?.let(::renderInfo)

        val busy = state.benchmarkRunning || state.stressRunning
        binding.buttonRefresh.isEnabled = !busy
        binding.buttonRunBenchmark.isEnabled = !busy && state.info != null
        binding.buttonRunStress.isEnabled = !busy && state.info != null

        binding.progressBenchmark.isVisible = state.benchmarkRunning
        binding.textBenchmarkResult.isVisible = state.benchmark != null
        binding.textBenchmarkNote.isVisible = state.benchmark != null
        state.benchmark?.let { result ->
            binding.textBenchmarkResult.text = getString(
                R.string.cpu_benchmark_result_fmt,
                formatOps(result.singleThreadOpsPerSecond),
                formatOps(result.multiThreadOpsPerSecond),
                result.workerCount
            )
        }

        binding.progressStress.isVisible = state.stressRunning
        binding.textStressProgress.isVisible = state.stressRunning
        binding.progressStress.progress = state.stressProgress
        binding.textStressProgress.text = getString(
            R.string.cpu_stress_progress_fmt,
            state.stressProgress
        )

        binding.textStressResult.isVisible = state.stress != null
        state.stress?.let { result ->
            binding.textStressResult.text = getString(
                R.string.cpu_stress_result_fmt,
                result.durationSeconds,
                formatOps(result.firstWindowOpsPerSecond),
                formatOps(result.lastWindowOpsPerSecond),
                result.sustainedPerformancePercent,
                getString(thermalStateLabel(result.initialThermalState)),
                getString(thermalStateLabel(result.finalThermalState)),
                formatHeadroom(result.initialThermalHeadroom),
                formatHeadroom(result.finalThermalHeadroom)
            )
        }

        binding.textCpuError.isVisible = state.error != null
        binding.textCpuError.text = state.error.orEmpty()
    }

    private fun renderInfo(info: CpuInfo) {
        val na = getString(R.string.common_not_available)
        val rows = mutableListOf(
            InfoRow(
                getString(R.string.cpu_soc_manufacturer),
                info.socManufacturer ?: na
            ),
            InfoRow(
                getString(R.string.cpu_soc_model),
                info.socModel ?: na
            ),
            InfoRow(getString(R.string.cpu_architecture), info.architecture),
            InfoRow(getString(R.string.cpu_cores), info.coreCount.toString()),
            InfoRow(
                getString(R.string.cpu_supported_abis),
                info.supportedAbis.joinToString(", ").ifBlank { na }
            ),
            InfoRow(getString(R.string.cpu_board), info.board),
            InfoRow(getString(R.string.cpu_hardware), info.hardware)
        )

        info.frequencies.forEach { core ->
            val currentMHz = core.currentKHz?.div(1000L)
            val maxMHz = core.maxKHz?.div(1000L)
            val value = when {
                currentMHz != null && maxMHz != null -> getString(
                    R.string.cpu_frequency_current_max_fmt,
                    currentMHz,
                    maxMHz
                )
                currentMHz != null -> getString(
                    R.string.cpu_frequency_current_fmt,
                    currentMHz
                )
                maxMHz != null -> getString(
                    R.string.cpu_frequency_max_fmt,
                    maxMHz
                )
                else -> null
            }
            if (value != null) {
                rows += InfoRow(
                    getString(R.string.cpu_core_frequency_fmt, core.coreIndex + 1),
                    value
                )
            }
        }

        adapter.submitList(rows)
    }

    private fun formatOps(value: Long): String =
        numberFormat.format(value)

    private fun formatHeadroom(value: Float?): String =
        value?.let { "%.2f".format(it) } ?: getString(R.string.common_not_available)

    private fun thermalStateLabel(state: ThermalState): Int = when (state) {
        ThermalState.NONE -> R.string.thermal_state_none
        ThermalState.LIGHT -> R.string.thermal_state_light
        ThermalState.MODERATE -> R.string.thermal_state_moderate
        ThermalState.SEVERE -> R.string.thermal_state_severe
        ThermalState.CRITICAL -> R.string.thermal_state_critical
        ThermalState.EMERGENCY -> R.string.thermal_state_emergency
        ThermalState.SHUTDOWN -> R.string.thermal_state_shutdown
        ThermalState.UNAVAILABLE -> R.string.common_not_available
    }
}
