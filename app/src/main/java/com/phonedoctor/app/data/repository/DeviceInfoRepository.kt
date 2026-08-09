package com.phonedoctor.app.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.phonedoctor.app.domain.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceInfoRepository(private val context: Context) {

    suspend fun getDeviceInfo(): DeviceInfo = withContext(Dispatchers.Default) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        val display = windowManager.defaultDisplay
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        val refreshRate = runCatching { display.refreshRate }.getOrNull()?.takeIf { it > 0f }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        DeviceInfo(
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            brand = Build.BRAND ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            supportedAbis = Build.SUPPORTED_ABIS?.toList() ?: emptyList(),
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            screenWidthPx = metrics.widthPixels,
            screenHeightPx = metrics.heightPixels,
            screenDensityDpi = metrics.densityDpi,
            refreshRateHz = refreshRate,
            totalRamBytes = memInfo.totalMem,
            board = Build.BOARD ?: "Unknown",
            hardware = Build.HARDWARE ?: "Unknown"
        )
    }
}
