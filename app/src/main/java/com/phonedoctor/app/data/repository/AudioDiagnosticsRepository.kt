package com.phonedoctor.app.data.repository

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.phonedoctor.app.domain.model.AudioRouteInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioDiagnosticsRepository(private val context: Context) {

    suspend fun getRoutes(): AudioRouteInfo = withContext(Dispatchers.Default) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        AudioRouteInfo(
            outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .map(::deviceLabel)
                .distinct(),
            inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                .map(::deviceLabel)
                .distinct()
        )
    }

    private fun deviceLabel(device: AudioDeviceInfo): String {
        val type = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in speaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth audio"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth headset"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in microphone"
            AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
            else -> "Audio device ${device.type}"
        }
        val product = device.productName?.toString()?.trim().orEmpty()
        return if (product.isNotBlank() && !product.equals(type, ignoreCase = true)) {
            "$type · $product"
        } else {
            type
        }
    }
}
