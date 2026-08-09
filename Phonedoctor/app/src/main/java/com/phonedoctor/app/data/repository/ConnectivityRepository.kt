package com.phonedoctor.app.data.repository

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.phonedoctor.app.domain.model.ConnectivityInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConnectivityRepository(private val context: Context) {

    suspend fun getConnectivityInfo(): ConnectivityInfo = withContext(Dispatchers.Default) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }

        val wifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val mobileConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val internetReachable = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val pm = context.packageManager
        val wifiHardwareAvailable = pm.hasSystemFeature(PackageManager.FEATURE_WIFI)
        val telephonyAvailable = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
        val bluetoothConnectGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
        val bluetoothEnabled = if (bluetoothAdapter != null && bluetoothConnectGranted) {
            runCatching { bluetoothAdapter.isEnabled }.getOrDefault(false)
        } else {
            false
        }

        ConnectivityInfo(
            wifiAvailable = wifiHardwareAvailable,
            wifiConnected = wifiConnected,
            bluetoothAvailable = bluetoothAdapter != null,
            bluetoothEnabled = bluetoothEnabled,
            mobileNetworkAvailable = telephonyAvailable,
            mobileNetworkConnected = mobileConnected,
            internetReachable = internetReachable
        )
    }
}
