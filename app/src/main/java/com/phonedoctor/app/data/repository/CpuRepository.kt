package com.phonedoctor.app.data.repository

import android.os.Build
import com.phonedoctor.app.domain.model.CpuCoreFrequency
import com.phonedoctor.app.domain.model.CpuInfo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CpuRepository {

    suspend fun getCpuInfo(): CpuInfo = withContext(Dispatchers.IO) {
        val coreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val frequencies = (0 until coreCount).map { core ->
            CpuCoreFrequency(
                coreIndex = core,
                currentKHz = readFrequency(core, "scaling_cur_freq"),
                maxKHz = readFrequency(core, "cpuinfo_max_freq")
                    ?: readFrequency(core, "scaling_max_freq")
            )
        }

        CpuInfo(
            coreCount = coreCount,
            architecture = System.getProperty("os.arch") ?: "Unknown",
            supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty(),
            socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.SOC_MANUFACTURER?.takeIf { it.isNotBlank() }
            } else {
                null
            },
            socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.SOC_MODEL?.takeIf { it.isNotBlank() }
            } else {
                null
            },
            board = Build.BOARD ?: "Unknown",
            hardware = Build.HARDWARE ?: "Unknown",
            frequencies = frequencies
        )
    }

    private fun readFrequency(core: Int, fileName: String): Long? {
        val path = File("/sys/devices/system/cpu/cpu$core/cpufreq/$fileName")
        return runCatching {
            path.readText()
                .trim()
                .toLongOrNull()
                ?.takeIf { it > 0L }
        }.getOrNull()
    }
}
