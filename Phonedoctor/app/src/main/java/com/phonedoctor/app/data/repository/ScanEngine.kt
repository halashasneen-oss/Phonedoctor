package com.phonedoctor.app.data.repository

import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.ChargingState
import com.phonedoctor.app.domain.model.ConnectivityInfo
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.model.TestStatus
import com.phonedoctor.app.domain.util.HealthScoreCalculator

/** One step completing during a full scan; the UI renders these as they arrive. */
data class ScanProgressEvent(
    val category: DiagnosticCategory,
    val result: CategoryResult,
    val completedCount: Int,
    val totalCount: Int
)

/**
 * Orchestrates a full device scan. Automatic categories (battery, storage,
 * memory, sensors, connectivity) are measured directly from system APIs.
 * Categories that need the user to actually look at or listen to the device
 * (display, touch, audio, microphone, camera) call [requestUserConfirmation],
 * which suspends the scan and lets the caller show an inline interactive
 * prompt before resuming — matching a real hardware check rather than a timer.
 */
class ScanEngine(
    private val batteryRepository: BatteryRepository,
    private val storageRepository: StorageRepository,
    private val memoryRepository: MemoryRepository,
    private val sensorsRepository: SensorsRepository,
    private val connectivityRepository: ConnectivityRepository
) {

    private val automaticOrder = listOf(
        DiagnosticCategory.BATTERY,
        DiagnosticCategory.STORAGE,
        DiagnosticCategory.MEMORY,
        DiagnosticCategory.CPU
    )
    private val interactiveOrder = listOf(
        DiagnosticCategory.DISPLAY,
        DiagnosticCategory.TOUCH,
        DiagnosticCategory.AUDIO,
        DiagnosticCategory.MICROPHONE
    )
    private val tailAutomaticOrder = listOf(
        DiagnosticCategory.SENSORS,
        DiagnosticCategory.CAMERA,
        DiagnosticCategory.CONNECTIVITY
    )

    val totalSteps = automaticOrder.size + interactiveOrder.size + tailAutomaticOrder.size

    suspend fun run(
        cameraAvailable: Boolean,
        requestUserConfirmation: suspend (DiagnosticCategory) -> Boolean,
        onProgress: suspend (ScanProgressEvent) -> Unit
    ): ScanReport {
        val results = mutableListOf<CategoryResult>()
        var completed = 0

        suspend fun emit(result: CategoryResult) {
            results += result
            completed++
            onProgress(ScanProgressEvent(result.category, result, completed, totalSteps))
        }

        emit(measureBattery())
        emit(measureStorage())
        emit(measureMemory())
        emit(measureCpu())

        for (category in interactiveOrder) {
            val confirmed = requestUserConfirmation(category)
            emit(interactiveResult(category, confirmed))
        }

        emit(measureSensors())
        emit(measureCamera(cameraAvailable))
        emit(measureConnectivity())

        val score = HealthScoreCalculator.calculate(results)
        return ScanReport(
            timestampMillis = System.currentTimeMillis(),
            healthScore = score,
            results = results
        )
    }

    private suspend fun measureBattery(): CategoryResult {
        val info = batteryRepository.getBatteryInfo()
        val level = info.levelPercent
        val status = when {
            level == null -> TestStatus.UNAVAILABLE
            info.chargingState == ChargingState.CHARGING || info.chargingState == ChargingState.FULL -> TestStatus.EXCELLENT
            level >= 50 -> TestStatus.EXCELLENT
            level >= 20 -> TestStatus.GOOD
            level >= 10 -> TestStatus.FAIR
            else -> TestStatus.POOR
        }
        val summary = if (level != null) "$level%" else "Unavailable"
        return CategoryResult(DiagnosticCategory.BATTERY, status, summary)
    }

    private suspend fun measureStorage(): CategoryResult {
        val info = storageRepository.getStorageInfo()
        val usedPercent = if (info.totalBytes > 0) (info.usedBytes * 100 / info.totalBytes).toInt() else 0
        val status = when {
            usedPercent < 70 -> TestStatus.EXCELLENT
            usedPercent < 85 -> TestStatus.GOOD
            usedPercent < 95 -> TestStatus.FAIR
            else -> TestStatus.POOR
        }
        return CategoryResult(DiagnosticCategory.STORAGE, status, "$usedPercent% used")
    }

    private suspend fun measureMemory(): CategoryResult {
        val info = memoryRepository.getMemoryInfo()
        val usedPercent = if (info.totalBytes > 0) (info.usedBytes * 100 / info.totalBytes).toInt() else 0
        val status = when {
            info.isLowMemory -> TestStatus.POOR
            usedPercent < 70 -> TestStatus.EXCELLENT
            usedPercent < 85 -> TestStatus.GOOD
            else -> TestStatus.FAIR
        }
        return CategoryResult(DiagnosticCategory.MEMORY, status, "$usedPercent% used")
    }

    private fun measureCpu(): CategoryResult {
        val cores = Runtime.getRuntime().availableProcessors()
        val status = if (cores > 0) TestStatus.EXCELLENT else TestStatus.UNAVAILABLE
        return CategoryResult(DiagnosticCategory.CPU, status, "$cores cores")
    }

    private fun interactiveResult(category: DiagnosticCategory, confirmed: Boolean): CategoryResult {
        val status = if (confirmed) TestStatus.EXCELLENT else TestStatus.FAIR
        val summary = if (confirmed) "Confirmed by user" else "User reported an issue"
        return CategoryResult(category, status, summary)
    }

    private fun measureSensors(): CategoryResult {
        val sensors = sensorsRepository.getAllSensors()
        val availableCount = sensors.count { it.available }
        val status = when {
            availableCount == sensors.size -> TestStatus.EXCELLENT
            availableCount >= sensors.size / 2 -> TestStatus.GOOD
            availableCount > 0 -> TestStatus.FAIR
            else -> TestStatus.UNAVAILABLE
        }
        return CategoryResult(DiagnosticCategory.SENSORS, status, "$availableCount/${sensors.size} available")
    }

    private fun measureCamera(cameraAvailable: Boolean): CategoryResult {
        val status = if (cameraAvailable) TestStatus.EXCELLENT else TestStatus.UNAVAILABLE
        val summary = if (cameraAvailable) "Detected" else "No camera detected"
        return CategoryResult(DiagnosticCategory.CAMERA, status, summary)
    }

    private suspend fun measureConnectivity(): CategoryResult {
        val info: ConnectivityInfo = connectivityRepository.getConnectivityInfo()
        val status = when {
            info.internetReachable == true -> TestStatus.EXCELLENT
            info.wifiConnected || info.mobileNetworkConnected -> TestStatus.FAIR
            else -> TestStatus.POOR
        }
        val summary = when {
            info.internetReachable == true -> "Online"
            info.wifiConnected -> "Wi-Fi connected, no internet"
            info.mobileNetworkConnected -> "Mobile data connected, no internet"
            else -> "No connection"
        }
        return CategoryResult(DiagnosticCategory.CONNECTIVITY, status, summary)
    }
}
