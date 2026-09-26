package com.phonedoctor.app.ui.thermal

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
import com.phonedoctor.app.databinding.FragmentThermalBinding
import com.phonedoctor.app.domain.model.ThermalInfo
import com.phonedoctor.app.domain.model.ThermalState
import com.phonedoctor.app.ui.common.InfoRow
import com.phonedoctor.app.ui.common.InfoRowAdapter
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class ThermalFragment : Fragment(R.layout.fragment_thermal) {

    private val binding by viewBinding(FragmentThermalBinding::bind)

    private val viewModel: ThermalViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                @Suppress("UNCHECKED_CAST")
                return ThermalViewModel(serviceLocator()) as T
            }
        }
    }

    private val adapter = InfoRowAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerInfo.adapter = adapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonRefresh.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.thermalInfo.collect { info ->
                    info?.let { render(it) }
                }
            }
        }
    }

    private fun render(info: ThermalInfo) {
        val na = getString(R.string.common_not_available)
        binding.textThermalStatus.setText(stateLabel(info.state))

        val rows = listOf(
            InfoRow(
                getString(R.string.thermal_status),
                getString(stateLabel(info.state))
            ),
            InfoRow(
                getString(R.string.thermal_headroom_current),
                info.currentHeadroom?.let { "%.2f".format(it) } ?: na
            ),
            InfoRow(
                getString(R.string.thermal_headroom_forecast),
                info.forecastHeadroom10s?.let { "%.2f".format(it) } ?: na
            ),
            InfoRow(
                getString(R.string.thermal_severe_threshold),
                info.severeThreshold?.let { "%.2f".format(it) } ?: na
            )
        )
        adapter.submitList(rows)
    }

    private fun stateLabel(state: ThermalState): Int = when (state) {
        ThermalState.NONE -> R.string.thermal_state_none
        ThermalState.LIGHT -> R.string.thermal_state_light
        ThermalState.MODERATE -> R.string.thermal_state_moderate
        ThermalState.SEVERE -> R.string.thermal_state_severe
        ThermalState.CRITICAL -> R.string.thermal_state_critical
        ThermalState.EMERGENCY -> R.string.thermal_state_emergency
        ThermalState.SHUTDOWN -> R.string.thermal_state_shutdown
        ThermalState.UNAVAILABLE -> R.string.common_not_available
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
