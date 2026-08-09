package com.phonedoctor.app.ui.battery

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
import com.phonedoctor.app.databinding.FragmentBatteryBinding
import com.phonedoctor.app.domain.model.BatteryInfo
import com.phonedoctor.app.domain.model.ChargePlug
import com.phonedoctor.app.domain.model.ChargingState
import com.phonedoctor.app.ui.common.InfoRow
import com.phonedoctor.app.ui.common.InfoRowAdapter
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class BatteryFragment : Fragment(R.layout.fragment_battery) {

    private val binding by viewBinding(FragmentBatteryBinding::bind)

    private val viewModel: BatteryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return BatteryViewModel(serviceLocator()) as T
            }
        }
    }

    private val adapter = InfoRowAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerInfo.adapter = adapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonRunCheck.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.batteryInfo.collect { info -> info?.let { render(it) } }
            }
        }
    }

    private fun render(info: BatteryInfo) {
        val na = getString(R.string.common_not_available)
        binding.textLevel.text = info.levelPercent?.let { "$it%" } ?: na
        binding.textChargingStatus.setText(chargingStateLabel(info.chargingState))

        val chargingStatusValue = if (info.chargePlug != ChargePlug.NONE && info.chargePlug != ChargePlug.UNKNOWN) {
            "${getString(chargingStateLabel(info.chargingState))} · ${chargePlugLabel(info.chargePlug)}"
        } else {
            getString(chargingStateLabel(info.chargingState))
        }

        val rows = listOf(
            InfoRow(getString(R.string.battery_level), info.levelPercent?.let { "$it%" } ?: na),
            InfoRow(getString(R.string.battery_charging_status), chargingStatusValue),
            InfoRow(getString(R.string.battery_temperature), info.temperatureCelsius?.let { "%.1f°C".format(it) } ?: na),
            InfoRow(getString(R.string.battery_voltage), info.voltageMillivolts?.let { "$it mV" } ?: na),
            InfoRow(getString(R.string.battery_current), info.currentMicroAmps?.let { "${it / 1000} mA" } ?: na),
            InfoRow(getString(R.string.battery_technology), info.technology ?: na),
            InfoRow(getString(R.string.battery_health), info.healthDescription ?: na)
        )
        adapter.submitList(rows)
    }

    private fun chargingStateLabel(state: ChargingState): Int = when (state) {
        ChargingState.CHARGING -> R.string.battery_charging
        ChargingState.DISCHARGING -> R.string.battery_discharging
        ChargingState.FULL -> R.string.battery_full
        ChargingState.NOT_CHARGING -> R.string.battery_not_charging
        ChargingState.UNKNOWN -> R.string.common_not_available
    }

    private fun chargePlugLabel(plug: ChargePlug): String = when (plug) {
        ChargePlug.AC -> getString(R.string.battery_ac)
        ChargePlug.USB -> getString(R.string.battery_usb)
        ChargePlug.WIRELESS -> getString(R.string.battery_wireless)
        else -> ""
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
