package com.phonedoctor.app.ui.home

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.R
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.domain.model.BatteryInfo
import com.phonedoctor.app.domain.model.ChargingState
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.model.TestStatus
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.domain.util.HealthScoreCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val healthScore: Int = 0,
    val healthStatusRes: Int = R.string.home_status_unknown,
    val lastCheckText: String = "",
    val quickTests: List<QuickTestItem> = emptyList(),
    val moreTools: List<MoreToolItem> = emptyList()
)

class HomeViewModel(
    private val serviceLocator: ServiceLocator,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(moreTools = buildMoreTools()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastReport: ScanReport? = null

    init {
        viewModelScope.launch {
            serviceLocator.historyRepository.latest.collect { latest ->
                lastReport = latest
                refresh(latest)
            }
        }
    }

    /** Re-reads ambient hardware state (battery/storage/memory/etc). Call on onResume. */
    fun refresh() {
        viewModelScope.launch { refresh(lastReport) }
    }

    private suspend fun refresh(latest: ScanReport?) {
        val battery = serviceLocator.batteryRepository.getBatteryInfo()
        val storage = serviceLocator.storageRepository.getStorageInfo()
        val memory = serviceLocator.memoryRepository.getMemoryInfo()
        val sensors = serviceLocator.sensorsRepository.getAllSensors()
        val connectivity = serviceLocator.connectivityRepository.getConnectivityInfo()
        val cameraAvailable = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

        val resultsByCategory = latest?.results?.associateBy { it.category } ?: emptyMap()

        val batteryStatus = batteryStatus(battery)
        val storagePercent = if (storage.totalBytes > 0) (storage.usedBytes * 100 / storage.totalBytes).toInt() else 0
        val storageStatus = percentStatus(storagePercent, invert = false)
        val memoryPercent = if (memory.totalBytes > 0) (memory.usedBytes * 100 / memory.totalBytes).toInt() else 0
        val memoryStatus = percentStatus(memoryPercent, invert = false)
        val sensorsAvailable = sensors.count { it.available }
        val sensorsStatus = when {
            sensorsAvailable == sensors.size -> TestStatus.EXCELLENT
            sensorsAvailable > 0 -> TestStatus.GOOD
            else -> TestStatus.UNAVAILABLE
        }
        val connectivityStatus = when {
            connectivity.internetReachable == true -> TestStatus.EXCELLENT
            connectivity.wifiConnected || connectivity.mobileNetworkConnected -> TestStatus.FAIR
            else -> TestStatus.POOR
        }
        val cameraStatus = if (cameraAvailable) TestStatus.EXCELLENT else TestStatus.UNAVAILABLE

        val quickTests = listOf(
            QuickTestItem(
                R.drawable.ic_battery, R.string.category_battery, R.id.action_home_to_battery,
                batteryStatus, battery.levelPercent?.let { "$it%" } ?: appContext.getString(R.string.common_not_available)
            ),
            QuickTestItem(
                R.drawable.ic_storage, R.string.category_storage, R.id.action_home_to_storage,
                storageStatus, "$storagePercent% used"
            ),
            QuickTestItem(
                R.drawable.ic_memory, R.string.category_memory, R.id.action_home_to_memory,
                memoryStatus, "$memoryPercent% used"
            ),
            fromHistoryOrDefault(
                R.drawable.ic_display, R.string.category_display, R.id.action_home_to_display,
                resultsByCategory[DiagnosticCategory.DISPLAY]
            ),
            fromHistoryOrDefault(
                R.drawable.ic_touch, R.string.category_touch, R.id.action_home_to_touch,
                resultsByCategory[DiagnosticCategory.TOUCH]
            ),
            fromHistoryOrDefault(
                R.drawable.ic_speaker, R.string.category_audio, R.id.action_home_to_speaker,
                resultsByCategory[DiagnosticCategory.AUDIO]
            ),
            fromHistoryOrDefault(
                R.drawable.ic_microphone, R.string.category_microphone, R.id.action_home_to_microphone,
                resultsByCategory[DiagnosticCategory.MICROPHONE]
            ),
            QuickTestItem(
                R.drawable.ic_camera, R.string.category_camera, R.id.action_home_to_camera,
                cameraStatus,
                if (cameraAvailable) appContext.getString(R.string.sensor_available) else appContext.getString(R.string.common_not_available)
            ),
            QuickTestItem(
                R.drawable.ic_sensors, R.string.category_sensors, R.id.action_home_to_sensors,
                sensorsStatus, "$sensorsAvailable/${sensors.size} available"
            ),
            QuickTestItem(
                R.drawable.ic_connectivity, R.string.category_connectivity, R.id.action_home_to_connectivity,
                connectivityStatus,
                if (connectivity.internetReachable == true) appContext.getString(R.string.connectivity_connected) else appContext.getString(R.string.connectivity_disconnected)
            )
        )

        val score = latest?.healthScore ?: 0
        _uiState.value = HomeUiState(
            healthScore = score,
            healthStatusRes = statusLabelRes(if (latest != null) HealthScoreCalculator.scoreToStatus(score) else TestStatus.UNAVAILABLE),
            lastCheckText = latest?.let { FormatUtils.formatRelativeTime(it.timestampMillis) }
                ?: appContext.getString(R.string.home_never_checked),
            quickTests = quickTests,
            moreTools = buildMoreTools()
        )
    }

    private fun fromHistoryOrDefault(
        iconRes: Int,
        titleRes: Int,
        navActionId: Int,
        result: com.phonedoctor.app.domain.model.CategoryResult?
    ): QuickTestItem {
        return if (result != null) {
            QuickTestItem(iconRes, titleRes, navActionId, result.status, result.summary)
        } else {
            QuickTestItem(iconRes, titleRes, navActionId, TestStatus.UNAVAILABLE, appContext.getString(R.string.home_status_unknown))
        }
    }

    private fun batteryStatus(info: BatteryInfo): TestStatus {
        val level = info.levelPercent ?: return TestStatus.UNAVAILABLE
        return when {
            info.chargingState == ChargingState.CHARGING || info.chargingState == ChargingState.FULL -> TestStatus.EXCELLENT
            level >= 50 -> TestStatus.EXCELLENT
            level >= 20 -> TestStatus.GOOD
            level >= 10 -> TestStatus.FAIR
            else -> TestStatus.POOR
        }
    }

    private fun percentStatus(usedPercent: Int, invert: Boolean): TestStatus = when {
        usedPercent < 70 -> TestStatus.EXCELLENT
        usedPercent < 85 -> TestStatus.GOOD
        usedPercent < 95 -> TestStatus.FAIR
        else -> TestStatus.POOR
    }

    private fun statusLabelRes(status: TestStatus): Int = when (status) {
        TestStatus.EXCELLENT -> R.string.home_status_excellent
        TestStatus.GOOD -> R.string.home_status_good
        TestStatus.FAIR -> R.string.home_status_fair
        TestStatus.POOR -> R.string.home_status_poor
        TestStatus.UNAVAILABLE -> R.string.home_status_unknown
    }

    private fun buildMoreTools(): List<MoreToolItem> = listOf(
        MoreToolItem(R.drawable.ic_cpu, R.string.cpu_title, R.id.action_home_to_cpu),
        MoreToolItem(R.drawable.ic_vibration, R.string.vibration_title, R.id.action_home_to_vibration),
        MoreToolItem(R.drawable.ic_flashlight, R.string.flashlight_title, R.id.action_home_to_flashlight)
    )
}
