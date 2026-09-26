package com.phonedoctor.app.domain.model

data class WifiLinkInfo(
    val frequencyMhz: Int?,
    val linkSpeedMbps: Int?,
    val rxLinkSpeedMbps: Int?,
    val txLinkSpeedMbps: Int?
)

data class UsbDeviceSummary(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int
)

data class ConnectivityInfo(
    val wifiAvailable: Boolean,
    val wifiConnected: Boolean,
    val bluetoothAvailable: Boolean,
    val bluetoothEnabled: Boolean,
    val mobileNetworkAvailable: Boolean,
    val mobileNetworkConnected: Boolean,
    val internetReachable: Boolean?,
    val bluetoothPermissionGranted: Boolean = true,
    val ethernetConnected: Boolean = false,
    val vpnActive: Boolean = false,
    val networkMetered: Boolean? = null,
    val captivePortalDetected: Boolean = false,
    val downstreamBandwidthKbps: Int? = null,
    val upstreamBandwidthKbps: Int? = null,
    val dnsServers: List<String> = emptyList(),
    val wifiLink: WifiLinkInfo? = null,
    val nfcAvailable: Boolean = false,
    val nfcEnabled: Boolean = false,
    val gpsProviderAvailable: Boolean = false,
    val usbDevices: List<UsbDeviceSummary> = emptyList()
)

data class NetworkProbeResult(
    val host: String,
    val dnsLatencyMillis: Long?,
    val tcpLatencyMillis: Long?,
    val resolvedAddressCount: Int,
    val success: Boolean,
    val errorMessage: String? = null
)
