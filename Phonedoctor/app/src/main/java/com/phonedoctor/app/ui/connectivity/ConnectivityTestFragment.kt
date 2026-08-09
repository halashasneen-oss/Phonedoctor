package com.phonedoctor.app.ui.connectivity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.data.repository.ConnectivityRepository
import com.phonedoctor.app.databinding.FragmentConnectivityTestBinding
import com.phonedoctor.app.domain.model.ConnectivityInfo
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class ConnectivityTestFragment : Fragment(R.layout.fragment_connectivity_test) {

    private val binding by viewBinding(FragmentConnectivityTestBinding::bind)
    private val repository by lazy { ConnectivityRepository(requireContext().applicationContext) }
    private val adapter = ConnectivityRowAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerConnectivity.adapter = adapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonRunTest.setOnClickListener { refresh() }
        refresh()
    }

    private fun refresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            val info = repository.getConnectivityInfo()
            render(info)
        }
    }

    private fun render(info: ConnectivityInfo) {
        val rows = listOf(
            ConnectivityRow(
                R.drawable.ic_wifi, R.string.connectivity_wifi,
                if (info.wifiConnected) R.string.connectivity_connected else R.string.connectivity_disconnected,
                if (info.wifiConnected) R.drawable.bg_pill_success else R.drawable.bg_pill_neutral,
                if (info.wifiConnected) R.color.color_success else R.color.color_text_secondary
            ),
            ConnectivityRow(
                R.drawable.ic_bluetooth, R.string.connectivity_bluetooth,
                if (!info.bluetoothAvailable) R.string.common_not_available
                else if (info.bluetoothEnabled) R.string.connectivity_available else R.string.connectivity_unavailable,
                if (info.bluetoothAvailable && info.bluetoothEnabled) R.drawable.bg_pill_success else R.drawable.bg_pill_neutral,
                if (info.bluetoothAvailable && info.bluetoothEnabled) R.color.color_success else R.color.color_text_secondary
            ),
            ConnectivityRow(
                R.drawable.ic_mobile_data, R.string.connectivity_mobile_network,
                if (!info.mobileNetworkAvailable) R.string.common_not_available
                else if (info.mobileNetworkConnected) R.string.connectivity_connected else R.string.connectivity_disconnected,
                if (info.mobileNetworkConnected) R.drawable.bg_pill_success else R.drawable.bg_pill_neutral,
                if (info.mobileNetworkConnected) R.color.color_success else R.color.color_text_secondary
            ),
            ConnectivityRow(
                R.drawable.ic_internet, R.string.connectivity_internet,
                when (info.internetReachable) {
                    true -> R.string.connectivity_connected
                    false -> R.string.connectivity_disconnected
                    null -> R.string.common_not_available
                },
                if (info.internetReachable == true) R.drawable.bg_pill_success else R.drawable.bg_pill_danger,
                if (info.internetReachable == true) R.color.color_success else R.color.color_danger
            )
        )
        adapter.submitList(rows)
    }
}
