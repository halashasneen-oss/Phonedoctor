package com.phonedoctor.app.ui.deviceinfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.google.android.material.snackbar.Snackbar
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentDeviceInformationBinding
import com.phonedoctor.app.domain.model.DeviceInfo
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.ui.common.InfoRow
import com.phonedoctor.app.ui.common.InfoRowAdapter
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class DeviceInformationFragment : Fragment(R.layout.fragment_device_information) {

    private val binding by viewBinding(FragmentDeviceInformationBinding::bind)

    private val viewModel: DeviceInformationViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return DeviceInformationViewModel(serviceLocator()) as T
            }
        }
    }

    private val generalAdapter = InfoRowAdapter()
    private val hardwareAdapter = InfoRowAdapter()
    private val displayAdapter = InfoRowAdapter()
    private val softwareAdapter = InfoRowAdapter()
    private var currentInfo: DeviceInfo? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerGeneral.adapter = generalAdapter
        binding.recyclerHardware.adapter = hardwareAdapter
        binding.recyclerDisplay.adapter = displayAdapter
        binding.recyclerSoftware.adapter = softwareAdapter

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonCopy.setOnClickListener { copyToClipboard() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deviceInfo.collect { info -> info?.let { render(it) } }
            }
        }
    }

    private fun render(info: DeviceInfo) {
        currentInfo = info
        val na = getString(R.string.common_not_available)

        generalAdapter.submitList(
            listOf(
                InfoRow(getString(R.string.cpu_manufacturer), info.manufacturer),
                InfoRow(getString(R.string.cpu_model), info.model),
                InfoRow(getString(R.string.device_info_brand), info.brand)
            )
        )
        hardwareAdapter.submitList(
            listOf(
                InfoRow(getString(R.string.cpu_architecture), info.cpuAbi),
                InfoRow(getString(R.string.cpu_cores), info.cpuCoreCount.toString()),
                InfoRow(getString(R.string.cpu_supported_abis), info.supportedAbis.joinToString(", ")),
                InfoRow(getString(R.string.device_info_board), info.board),
                InfoRow(getString(R.string.device_info_hardware), info.hardware),
                InfoRow(getString(R.string.memory_total), FormatUtils.formatBytes(info.totalRamBytes))
            )
        )
        displayAdapter.submitList(
            listOf(
                InfoRow(getString(R.string.cpu_screen_resolution), "${info.screenWidthPx} × ${info.screenHeightPx}"),
                InfoRow(getString(R.string.device_info_density), "${info.screenDensityDpi} dpi"),
                InfoRow(getString(R.string.cpu_refresh_rate), info.refreshRateHz?.let { "%.0f Hz".format(it) } ?: na)
            )
        )
        softwareAdapter.submitList(
            listOf(
                InfoRow(getString(R.string.cpu_android_version), info.androidVersion),
                InfoRow(getString(R.string.cpu_sdk_version), info.sdkInt.toString())
            )
        )
    }

    private fun copyToClipboard() {
        val info = currentInfo ?: return
        val text = buildString {
            appendLine("${getString(R.string.cpu_manufacturer)}: ${info.manufacturer}")
            appendLine("${getString(R.string.cpu_model)}: ${info.model}")
            appendLine("${getString(R.string.device_info_brand)}: ${info.brand}")
            appendLine("${getString(R.string.cpu_architecture)}: ${info.cpuAbi}")
            appendLine("${getString(R.string.cpu_cores)}: ${info.cpuCoreCount}")
            appendLine("${getString(R.string.cpu_supported_abis)}: ${info.supportedAbis.joinToString(", ")}")
            appendLine("${getString(R.string.device_info_board)}: ${info.board}")
            appendLine("${getString(R.string.device_info_hardware)}: ${info.hardware}")
            appendLine("${getString(R.string.memory_total)}: ${FormatUtils.formatBytes(info.totalRamBytes)}")
            appendLine("${getString(R.string.cpu_screen_resolution)}: ${info.screenWidthPx} x ${info.screenHeightPx}")
            appendLine("${getString(R.string.device_info_density)}: ${info.screenDensityDpi} dpi")
            appendLine("${getString(R.string.cpu_android_version)}: ${info.androidVersion}")
            appendLine("${getString(R.string.cpu_sdk_version)}: ${info.sdkInt}")
        }
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.device_info_title), text))
        Snackbar.make(binding.root, R.string.device_info_copied, Snackbar.LENGTH_SHORT).show()
    }
}
