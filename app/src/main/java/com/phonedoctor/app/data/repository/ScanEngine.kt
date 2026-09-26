package com.phonedoctor.app.data.repository

import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.ConnectivityInfo
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.DiagnosticConfidence
import com.phonedoctor.app.domain.model.DiagnosticEvidenceType
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.model.ScoreImpact
import com.phonedoctor.app.domain.model.TestStatus
import com.phonedoctor.app.domain.model.ThermalState
import com.phonedoctor.app.domain.util.HealthScoreCalculator

/** One step completing during a full scan; the UI renders these as they arrive. */
data class ScanProgressEvent(
    val category: DiagnosticCategory,
    val result: CategoryResult,
    val completedCount: Int,
    val totalCount: Int
)

/**
 * Diagnostic Engine 2.0 orchestrates the full scan while keeping a strict
 * distinction between measured health, capabilities, current state and
 * user-verified checks.
 */
class ScanEngine(
    private val batteryRepository: BatteryRepository,
    private val storageRepository: StorageRepository,
    private val memoryRepository: MemoryRepository,
    private val thermalRepository: ThermalRepository,
    private val sensorsRepository: SensorsRepository,
    private val connectivityRepository: ConnectivityRepository
) {

    private val automaticOrder = listOf(
        DiagnosticCategory.BATTERY,
        DiagnosticCategory.STORAGE,
        DiagnosticCategory.MEMORY,
        DiagnosticCategory.CPU,
        DiagnosticCategory.THERMAL
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

    suspend fun runAutomatic(
        cameraAvailable: Boolean,
        onProgress: suspend (ScanProgressEvent) -> Unit = {}
    ): ScanReport {
        val results = mutableListOf<CategoryResult>()
        val order = automaticOrder + tailAutomaticOrder
        var completed = 0

        suspend fun emit(result: CategoryResult) {
            results += result
            completed++
            onProgress(ScanProgressEvent(result.category, result, completed, order.size))
        }

        emit(measureBattery())
        emit(measureStorage())
        emit(measureMemory())
        emit(measureCpu())
        emit(measureThermal())
        emit(measureSensors())
        emit(measureCamera(cameraAvailable))
        emit(measureConnectivity())

        return ScanReport(
            timestampMillis = System.currentTimeMillis(),
            healthScore = HealthScoreCalculator.calculate(results),
            results = results
        )
    }

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
        emit(measureThermal())

        for (category in interactiveOrder) {
            emit(interactiveResult(category, requestUserConfirmation(category)))
        }

        emit(measureSensors())
        emit(measureCamera(cameraAvailable))
        emit(measureConnectivity())

        return ScanReport(
            timestampMillis = System.currentTimeMillis(),
            healthScore = HealthScoreCalculator.calculate(results),
            results = results
        )
    }

    private suspend fun measureBattery(): CategoryResult {
        val info = batteryRepository.getBatteryInfo()
        val health = info.healthDescription
        val temperature = info.temperatureCelsius

        val status = when {
            health.equals("Dead", ignoreCase = true) ||
                health.equals("Overheating", ignoreCase = true) ||
                health.equals("Over voltage", ignoreCase = true) ||
                health.equals("Unspecified failure", ignoreCase = true) -> TestStatus.POOR
            health.equals("Cold", ignoreCase = true) -> TestStatus.FAIR
            temperature != null && temperature >= 50f -> TestStatus.POOR
            temperature != null && (temperature >= 45f || temperature <= 0f) -> TestStatus.FAIR
            health.equals("Good", ignoreCase = true) -> TestStatus.EXCELLENT
            temperature != null -> TestStatus.GOOD
            else -> TestStatus.UNAVAILABLE
        }

        val levelText = info.levelPercent?.let { "$it%" } ?: "level unavailable"
        val temperatureText = temperature?.let { "%.1f°C".format(it) } ?: "temperature unavailable"
        val healthText = health ?: "OEM health unavailable"
        val confidence = if (health != null) {
            DiagnosticConfidence.HIGH
        } else {
            DiagnosticConfidence.MEDIUM
        }

        return CategoryResult(
            category = DiagnosticCategory.BATTERY,
            status = status,
            summary = "$levelText · $temperatureText",
            detail = healthText,
            evidenceType = DiagnosticEvidenceType.MEASURED,
            scoreImpact = ScoreImpact.HEALTH,
            confidence = confidence
        )
    }

    private suspend fun measureStorage(): CategoryResult {
        val info = storageRepository.getStorageInfo()
        val usedPercent = if (info.totalBytes > 0) {
            (info.usedBytes * 100 / info.totalBytes).toInt()
        } else {
            0
        }
        val status = when {
            info.totalBytes <= 0L -> TestStatus.UNAVAILABLE
            usedPercent < 70 -> TestStatus.EXCELLENT
            usedPercent < 85 -> TestStatus.GOOD
            usedPercent < 95 -> TestStatus.FAIR
            else -> TestStatus.POOR
        }
        return CategoryResult(
            category = DiagnosticCategory.STORAGE,
            status = status,
            summary = "$usedPercent% used",
            detail = "Capacity state; not a storage-hardware health measurement",
            evidenceType = DiagnosticEvidenceType.CURRENT_STATE,
            scoreImpact = ScoreImpact.INFORMATIONAL,
            confidence = DiagnosticConfidence.HIGH
        )
    }

    private suspend fun measureMemory(): CategoryResult {
        val info = memoryRepository.getMemoryInfo()
        val usedPercent = if (info.totalBytes > 0) {
            (info.usedBytes * 100 / info.totalBytes).toInt()
        } else {
            0
        }
        val status = when {
            info.totalBytes <= 0L -> TestStatus.UNAVAILABLE
            info.isLowMemory -> TestStatus.FAIR
            else -> TestStatus.GOOD
        }
        return CategoryResult(
            category = DiagnosticCategory.MEMORY,
            status = status,
            summary = "$usedPercent% used",
            detail = if (info.isLowMemory) {
                "Android currently reports low-memory pressure"
            } else {
                "Current RAM utilization"
            },
            evidenceType = DiagnosticEvidenceType.CURRENT_STATE,
            scoreImpact = ScoreImpact.INFORMATIONAL,
            confidence = DiagnosticConfidence.HIGH
        )
    }

    private fun measureCpu(): CategoryResult {
        val cores = Runtime.getRuntime().availableProcessors()
        val status = if (cores > 0) TestStatus.GOOD else TestStatus.UNAVAILABLE
        return CategoryResult(
            category = DiagnosticCategory.CPU,
            status = status,
            summary = "$cores cores detected",
            detail = "Processor availability only; benchmark health is not measured yet",
            evidenceType = DiagnosticEvidenceType.CAPABILITY,
            scoreImpact = ScoreImpact.INFORMATIONAL,
            confidence = DiagnosticConfidence.HIGH
        )
    }

    private suspend fun measureThermal(): CategoryResult {
        val info = thermalRepository.getThermalInfo()
        val status = when (info.state) {
            ThermalState.NONE -> TestStatus.EXCELLENT
            ThermalState.LIGHT -> TestStatus.GOOD
            ThermalState.MODERATE -> TestStatus.FAIR
            ThermalState.SEVERE,
            ThermalState.CRITICAL,
            ThermalState.EMERGENCY,
            ThermalState.SHUTDOWN -> TestStatus.POOR
            ThermalState.UNAVAILABLE -> TestStatus.UNAVAILABLE
        }

        val summary = buildString {
            append(info.state.name.lowercase().replaceFirstChar { it.uppercase() })
            info.currentHeadroom?.let {
                append(" · headroom ")
                append("%.2f".format(it))
            }
        }

        return CategoryResult(
            category = DiagnosticCategory.THERMAL,
            status = status,
            summary = summary,
            detail = info.forecastHeadroom10s?.let {
                "10s forecast headroom: %.2f".format(it)
            },
            evidenceType = DiagnosticEvidenceType.MEASURED,
            scoreImpact = ScoreImpact.HEALTH,
            confidence = DiagnosticConfidence.HIGH
        )
    }

    private fun interactiveResult(
        category: DiagnosticCategory,
        confirmed: Boolean
    ): CategoryResult {
        val status = if (confirmed) TestStatus.EXCELLENT else TestStatus.FAIR
        val summary = if (confirmed) {
            "Confirmed by user"
        } else {
            "User reported an issue"
        }
        return CategoryResult(
            category = category,
            status = status,
            summary = summary,
            evidenceType = DiagnosticEvidenceType.USER_VERIFIED,
            scoreImpact = ScoreImpact.HEALTH,
            confidence = DiagnosticConfidence.HIGH
        )
    }

    private fun measureSensors(): CategoryResult {
        val sensors = sensorsRepository.getAllSensors()
        val availableCount = sensors.count { it.available }
        val status = when {
            sensors.isEmpty() -> TestStatus.UNAVAILABLE
            availableCount > 0 -> TestStatus.GOOD
            else -> TestStatus.UNAVAILABLE
        }
        return CategoryResult(
            category = DiagnosticCategory.SENSORS,
            status = status,
            summary = "$availableCount/${sensors.size} supported",
            detail = "Sensor inventory; unsupported sensors do not reduce health",
            evidenceType = DiagnosticEvidenceType.CAPABILITY,
            scoreImpact = ScoreImpact.INFORMATIONAL,
            confidence = DiagnosticConfidence.HIGH
        )
    }

    private fun measureCamera(cameraAvailable: Boolean): CategoryResult {
        return CategoryResult(
            category = DiagnosticCategory.CAMERA,
            status = if (cameraAvailable) TestStatus.GOOD else TestStatus.UNAVAILABLE,
            summary = if (cameraAvailable) "Camera hardware detected" else "No camera capability detected",
            detail = "Camera presence only; image quality and operation require an interactive test",
            evidenceType = DiagnosticEvidenceType.CAPABILITY,
            scoreImpact = ScoreImpact.INFORMATIONAL,
            confidence = DiagnosticConfidence.HIGH
        )
    }

    private suspend fun measureConnectivity(): CategoryResult {
        val info: ConnectivityInfo = connectivityRepository.getConnectivityInfo()
        val status = when {
            info.internetReachable == true -> TestStatus.EXCELLENT
            info.wifiConnected || info.mobileNetworkConnected -> TestStatus.FAIR
            else -> TestStatus.UNAVAILABLE
        }
        val summary = when {
            info.internetReachable == true -> "Online"
            info.wifiConnected -> "Wi-Fi connected, internet not validated"
            info.mobileNetworkConnected -> "Mobile data connected, internet not validated"
            else -> "No active network connection"
        }
        return CategoryResult(
            category = DiagnosticCategory.CONNECTIVITY,
            status = status,
            summary = summary,
            detail = "Current connectivity state; offline status is not hardware failure",
            evidenceType = DiagnosticEvidenceType.CURRENT_STATE,
            scoreImpact = ScoreImpact.INFORMATIONAL,
            confidence = DiagnosticConfidence.HIGH
        )
    }
}
