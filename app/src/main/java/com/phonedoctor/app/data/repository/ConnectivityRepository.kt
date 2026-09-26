package com.phonedoctor.app.data.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import androidx.core.content.ContextCompat
import com.phonedoctor.app.domain.model.ConnectivityInfo
import com.phonedoctor.app.domain.model.UsbDeviceSummary
import com.phonedoctor.app.domain.model.WifiLinkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConnectivityRepository(private val context: Context) {

    suspend fun getConnectivityInfo(): ConnectivityInfo = withContext(Dispatchers.Default) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)
        }
        val linkProperties = activeNetwork?.let {
            connectivityManager.getLinkProperties(it)
        }

        val wifiConnected =
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val mobileConnected =
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val ethernetConnected =
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val vpnActive =
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val internetReachable = capabilities?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )

        val pm = context.packageManager
        val wifiHardwareAvailable = pm.hasSystemFeature(PackageManager.FEATURE_WIFI)
        val telephonyAvailable = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
        val bluetoothConnectGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

        val bluetoothEnabled = if (
            bluetoothAdapter != null &&
            bluetoothConnectGranted
        ) {
            runCatching { bluetoothAdapter.isEnabled }.getOrDefault(false)
        } else {
            false
        }

        val wifiLink = if (wifiConnected) readWifiLink() else null

        val nfcAdapter = runCatching {
            NfcAdapter.getDefaultAdapter(context)
        }.getOrNull()

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsProviderAvailable = runCatching {
            locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)
        }.getOrDefault(false)

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevices = runCatching {
            usbManager.deviceList.values.map { device ->
                UsbDeviceSummary(
                    deviceName = device.deviceName,
                    vendorId = device.vendorId,
                    productId = device.productId,
                    deviceClass = device.deviceClass
                )
            }
        }.getOrDefault(emptyList())

        ConnectivityInfo(
            wifiAvailable = wifiHardwareAvailable,
            wifiConnected = wifiConnected,
            bluetoothAvailable = bluetoothAdapter != null,
            bluetoothEnabled = bluetoothEnabled,
            mobileNetworkAvailable = telephonyAvailable,
            mobileNetworkConnected = mobileConnected,
            internetReachable = internetReachable,
            bluetoothPermissionGranted = bluetoothConnectGranted,
            ethernetConnected = ethernetConnected,
            vpnActive = vpnActive,
            networkMetered = capabilities?.let {
                !it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            },
            captivePortalDetected = capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL
            ) == true,
            downstreamBandwidthKbps = capabilities?.linkDownstreamBandwidthKbps,
            upstreamBandwidthKbps = capabilities?.linkUpstreamBandwidthKbps,
            dnsServers = linkProperties?.dnsServers
                ?.map { it.hostAddress ?: it.toString() }
                .orEmpty(),
            wifiLink = wifiLink,
            nfcAvailable = nfcAdapter != null,
            nfcEnabled = runCatching { nfcAdapter?.isEnabled == true }.getOrDefault(false),
            gpsProviderAvailable = gpsProviderAvailable,
            usbDevices = usbDevices
        )
    }

    private fun readWifiLink(): WifiLinkInfo? {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = runCatching { wifiManager.connectionInfo }.getOrNull() ?: return null

        return WifiLinkInfo(
            frequencyMhz = info.frequency.takeIf { it > 0 },
            linkSpeedMbps = info.linkSpeed.takeIf { it > 0 },
            rxLinkSpeedMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.rxLinkSpeedMbps.takeIf { it > 0 }
            } else {
                null
            },
            txLinkSpeedMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.txLinkSpeedMbps.takeIf { it > 0 }
            } else {
                null
            }
        )
    }
}
