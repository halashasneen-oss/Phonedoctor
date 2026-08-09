package com.phonedoctor.app.ui.cpu

import android.os.Bundle
import android.view.View
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
import com.phonedoctor.app.domain.model.DeviceInfo
import com.phonedoctor.app.ui.common.InfoRow
import com.phonedoctor.app.ui.common.InfoRowAdapter
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class CpuFragment : Fragment(R.layout.fragment_cpu) {

    private val binding by viewBinding(FragmentCpuBinding::bind)

    private val viewModel: CpuViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return CpuViewModel(serviceLocator()) as T
            }
        }
    }

    private val adapter = InfoRowAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerInfo.adapter = adapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deviceInfo.collect { info -> info?.let { render(it) } }
            }
        }
    }

    private fun render(info: DeviceInfo) {
        val na = getString(R.string.common_not_available)
        val rows = listOf(
            InfoRow(getString(R.string.cpu_manufacturer), info.manufacturer),
            InfoRow(getString(R.string.cpu_model), info.model),
            InfoRow(getString(R.string.cpu_android_version), info.androidVersion),
            InfoRow(getString(R.string.cpu_sdk_version), info.sdkInt.toString()),
            InfoRow(getString(R.string.cpu_architecture), info.cpuAbi),
            InfoRow(getString(R.string.cpu_cores), info.cpuCoreCount.toString()),
            InfoRow(getString(R.string.cpu_supported_abis), info.supportedAbis.joinToString(", ")),
            InfoRow(getString(R.string.cpu_screen_resolution), "${info.screenWidthPx} × ${info.screenHeightPx}"),
            InfoRow(getString(R.string.cpu_refresh_rate), info.refreshRateHz?.let { "%.0f Hz".format(it) } ?: na)
        )
        adapter.submitList(rows)
    }
}
