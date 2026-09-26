package com.phonedoctor.app.ui.storage

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import com.phonedoctor.app.databinding.FragmentStorageBinding
import com.phonedoctor.app.domain.model.StorageInfo
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.domain.util.PerformanceBenchmarkMath
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class StorageFragment : Fragment(R.layout.fragment_storage) {

    private val binding by viewBinding(FragmentStorageBinding::bind)

    private val viewModel: StorageViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                @Suppress("UNCHECKED_CAST")
                return StorageViewModel(serviceLocator()) as T
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonViewBreakdown.setOnClickListener { openSystemStorageSettings() }
        binding.buttonRunStorageBenchmark.setOnClickListener {
            viewModel.runBenchmark()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun openSystemStorageSettings() {
        val primary = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        val fallback = Intent(Settings.ACTION_SETTINGS)
        runCatching { startActivity(primary) }
            .onFailure { startActivity(fallback) }
    }

    private fun render(state: StorageUiState) {
        state.info?.let(::renderStorage)

        binding.buttonRunStorageBenchmark.isEnabled = !state.benchmarkRunning
        binding.progressStorageBenchmark.isVisible = state.benchmarkRunning
        binding.textStorageBenchmarkResult.isVisible = state.benchmark != null
        binding.textStorageBenchmarkNote.isVisible = state.benchmark != null

        state.benchmark?.let { result ->
            val writeMiB = PerformanceBenchmarkMath.mebibytesPerSecond(
                result.writeBytesPerSecond
            )
            val readMiB = PerformanceBenchmarkMath.mebibytesPerSecond(
                result.readBytesPerSecond
            )
            binding.textStorageBenchmarkResult.text = getString(
                R.string.storage_benchmark_result_fmt,
                FormatUtils.formatBytes(result.testBytes),
                writeMiB,
                result.writeDurationMillis,
                readMiB,
                result.readDurationMillis
            )
        }

        binding.textStorageError.isVisible = state.error != null
        binding.textStorageError.text = state.error.orEmpty()
    }

    private fun renderStorage(info: StorageInfo) {
        binding.textUsedFree.text = getString(
            R.string.storage_used
        ) + ": " + FormatUtils.formatBytes(info.usedBytes) +
            " / " + FormatUtils.formatBytes(info.totalBytes)

        val percent = if (info.totalBytes > 0) {
            (info.usedBytes * 100 / info.totalBytes).toInt()
        } else {
            0
        }

        binding.progressStorage.setProgressCompat(percent, true)
        binding.textUsed.text =
            "${getString(R.string.storage_used)}: ${FormatUtils.formatBytes(info.usedBytes)}"
        binding.textFree.text =
            "${getString(R.string.storage_free)}: ${FormatUtils.formatBytes(info.freeBytes)}"
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
