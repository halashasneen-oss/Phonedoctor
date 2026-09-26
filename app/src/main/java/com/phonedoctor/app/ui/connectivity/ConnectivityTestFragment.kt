package com.phonedoctor.app.ui.connectivity

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentConnectivityTestBinding
import com.phonedoctor.app.domain.model.ConnectivityInfo
import com.phonedoctor.app.domain.model.NetworkProbeResult
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class ConnectivityTestFragment :
    Fragment(R.layout.fragment_connectivity_test) {

    private val binding by viewBinding(FragmentConnectivityTestBinding::bind)
    private val adapter = ConnectivityRowAdapter()

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refresh()
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerConnectivity.adapter = adapter
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.buttonRunTest.setOnClickListener { refresh() }
        binding.buttonGrantBluetooth.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }
        }
        binding.buttonRunLatencyTest.setOnClickListener {
            runNetworkProbe()
        }

        refresh()
    }

    private fun refresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            val info = serviceLocator()
                .connectivityRepository
                .getConnectivityInfo()
            render(info)
        }
    }

    private fun render(info: ConnectivityInfo) {
        val rows = listOf(
            row(
                R.drawable.ic_wifi,
                R.string.connectivity_wifi,
                if (!info.wifiAvailable) {
                    getString(R.string.common_not_available)
                } else if (info.wifiConnected) {
                    getString(R.string.connectivity_connected)
                } else {
                    getString(R.string.connectivity_disconnected)
                },
                info.wifiConnected
            ),
            row(
                R.drawable.ic_bluetooth,
                R.string.connectivity_bluetooth,
                when {
                    !info.bluetoothAvailable ->
                        getString(R.string.common_not_available)
                    !info.bluetoothPermissionGranted ->
                        getString(R.string.connectivity_permission_required)
                    info.bluetoothEnabled ->
                        getString(R.string.connectivity_available)
                    else ->
                        getString(R.string.connectivity_unavailable)
                },
                info.bluetoothAvailable &&
                    info.bluetoothPermissionGranted &&
                    info.bluetoothEnabled
            ),
            row(
                R.drawable.ic_mobile_data,
                R.string.connectivity_mobile_network,
                if (!info.mobileNetworkAvailable) {
                    getString(R.string.common_not_available)
                } else if (info.mobileNetworkConnected) {
                    getString(R.string.connectivity_connected)
                } else {
                    getString(R.string.connectivity_disconnected)
                },
                info.mobileNetworkConnected
            ),
            row(
                R.drawable.ic_internet,
                R.string.connectivity_internet,
                when (info.internetReachable) {
                    true -> getString(R.string.connectivity_connected)
                    false -> getString(R.string.connectivity_disconnected)
                    null -> getString(R.string.common_not_available)
                },
                info.internetReachable == true
            ),
            row(
                R.drawable.ic_connectivity,
                R.string.connectivity_ethernet,
                if (info.ethernetConnected) {
                    getString(R.string.connectivity_connected)
                } else {
                    getString(R.string.connectivity_disconnected)
                },
                info.ethernetConnected
            ),
            row(
                R.drawable.ic_connectivity,
                R.string.connectivity_vpn,
                if (info.vpnActive) {
                    getString(R.string.connectivity_active)
                } else {
                    getString(R.string.connectivity_inactive)
                },
                info.vpnActive
            ),
            row(
                R.drawable.ic_connectivity,
                R.string.connectivity_nfc,
                if (!info.nfcAvailable) {
                    getString(R.string.common_not_available)
                } else if (info.nfcEnabled) {
                    getString(R.string.connectivity_available)
                } else {
                    getString(R.string.connectivity_unavailable)
                },
                info.nfcAvailable && info.nfcEnabled
            ),
            row(
                R.drawable.ic_connectivity,
                R.string.connectivity_gnss,
                if (info.gpsProviderAvailable) {
                    getString(R.string.connectivity_available)
                } else {
                    getString(R.string.common_not_available)
                },
                info.gpsProviderAvailable
            ),
            row(
                R.drawable.ic_device_info,
                R.string.connectivity_usb,
                getString(
                    R.string.connectivity_usb_devices_fmt,
                    info.usbDevices.size
                ),
                info.usbDevices.isNotEmpty()
            )
        )
        adapter.submitList(rows)

        binding.buttonGrantBluetooth.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                info.bluetoothAvailable &&
                !info.bluetoothPermissionGranted

        binding.textAdvancedConnectivity.text =
            buildAdvancedDetails(info)
    }

    private fun row(
        iconRes: Int,
        labelRes: Int,
        statusText: String,
        positive: Boolean
    ): ConnectivityRow {
        return ConnectivityRow(
            iconRes = iconRes,
            labelRes = labelRes,
            statusText = statusText,
            pillRes = if (positive) {
                R.drawable.bg_pill_success
            } else {
                R.drawable.bg_pill_neutral
            },
            textColorRes = if (positive) {
                R.color.color_success
            } else {
                R.color.color_text_secondary
            }
        )
    }

    private fun buildAdvancedDetails(info: ConnectivityInfo): String {
        val na = getString(R.string.common_not_available)
        val wifi = info.wifiLink
        val wifiDetails = if (wifi == null) {
            na
        } else {
            getString(
                R.string.connectivity_wifi_details_fmt,
                wifi.frequencyMhz?.toString() ?: na,
                wifi.linkSpeedMbps?.toString() ?: na,
                wifi.rxLinkSpeedMbps?.toString() ?: na,
                wifi.txLinkSpeedMbps?.toString() ?: na
            )
        }

        val dns = info.dnsServers
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?: na

        val usb = info.usbDevices
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n") {
                getString(
                    R.string.connectivity_usb_device_fmt,
                    it.vendorId,
                    it.productId,
                    it.deviceClass
                )
            }
            ?: na

        val metered = info.networkMetered?.let {
            getString(
                if (it) {
                    R.string.common_yes
                } else {
                    R.string.common_no
                }
            )
        } ?: na

        return getString(
            R.string.connectivity_advanced_details_fmt,
            wifiDetails,
            info.downstreamBandwidthKbps?.toString() ?: na,
            info.upstreamBandwidthKbps?.toString() ?: na,
            metered,
            getString(
                if (info.captivePortalDetected) {
                    R.string.common_yes
                } else {
                    R.string.common_no
                }
            ),
            dns,
            usb
        )
    }

    private fun runNetworkProbe() {
        binding.buttonRunLatencyTest.isEnabled = false
        binding.progressNetworkProbe.isVisible = true
        binding.textNetworkProbe.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = serviceLocator()
                .networkDiagnosticsEngine
                .runProbe()
            renderProbe(result)
            binding.progressNetworkProbe.isVisible = false
            binding.buttonRunLatencyTest.isEnabled = true
        }
    }

    private fun renderProbe(result: NetworkProbeResult) {
        binding.textNetworkProbe.isVisible = true
        binding.textNetworkProbe.text = if (result.success) {
            getString(
                R.string.connectivity_probe_result_fmt,
                result.host,
                result.resolvedAddressCount,
                result.dnsLatencyMillis ?: 0L,
                result.tcpLatencyMillis ?: 0L
            )
        } else {
            getString(
                R.string.connectivity_probe_failed_fmt,
                result.host,
                result.errorMessage ?: getString(R.string.common_not_available)
            )
        }
    }
}
