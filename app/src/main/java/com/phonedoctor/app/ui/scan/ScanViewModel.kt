package com.phonedoctor.app.ui.scan

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.data.repository.ScanProgressEvent
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.ScanMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ScanListItem(
    val category: DiagnosticCategory,
    val labelRes: Int,
    val state: ScanItemState
)

enum class ScanItemState { PENDING, RUNNING, DONE }

data class ScanUiState(
    val items: List<ScanListItem> = emptyList(),
    val selectedMode: ScanMode? = null,
    val started: Boolean = false,
    val progressPercent: Int = 0,
    val pendingInteraction: DiagnosticCategory? = null,
    val finishedReportId: Long? = null,
    val cancelled: Boolean = false
)

class ScanViewModel(
    private val serviceLocator: ServiceLocator,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val interactionResponse = MutableStateFlow<Boolean?>(null)
    private var scanJob: Job? = null
    private var activeOrder: List<DiagnosticCategory> = emptyList()

    fun start(mode: ScanMode) {
        if (_uiState.value.started) return

        activeOrder = serviceLocator.scanEngine.categoriesFor(mode)
        _uiState.value = ScanUiState(
            items = activeOrder.map {
                ScanListItem(
                    category = it,
                    labelRes = labelResFor(it),
                    state = ScanItemState.PENDING
                )
            },
            selectedMode = mode,
            started = true
        )

        scanJob = viewModelScope.launch {
            try {
                val cameraAvailable = appContext.packageManager.hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_ANY
                )
                activeOrder.firstOrNull()?.let(::markRunning)

                val report = serviceLocator.scanEngine.runMode(
                    mode = mode,
                    cameraAvailable = cameraAvailable,
                    requestUserConfirmation = { category ->
                        awaitUserConfirmation(category)
                    },
                    onProgress = { event ->
                        onProgress(event)
                    }
                )

                val id = serviceLocator.historyRepository.save(report)
                _uiState.value = _uiState.value.copy(
                    finishedReportId = id
                )
            } catch (cancelled: CancellationException) {
                _uiState.value = _uiState.value.copy(
                    cancelled = true,
                    pendingInteraction = null
                )
                throw cancelled
            } finally {
                scanJob = null
            }
        }
    }

    private suspend fun awaitUserConfirmation(
        category: DiagnosticCategory
    ): Boolean {
        markRunning(category)
        interactionResponse.value = null
        _uiState.value = _uiState.value.copy(
            pendingInteraction = category
        )
        val result = interactionResponse.first { it != null }!!
        interactionResponse.value = null
        _uiState.value = _uiState.value.copy(
            pendingInteraction = null
        )
        return result
    }

    fun respondToInteraction(confirmed: Boolean) {
        interactionResponse.value = confirmed
    }

    fun cancel() {
        scanJob?.cancel()
        scanJob = null
        _uiState.value = _uiState.value.copy(
            cancelled = true,
            pendingInteraction = null
        )
    }

    private fun markRunning(category: DiagnosticCategory) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (
                    it.category == category &&
                    it.state == ScanItemState.PENDING
                ) {
                    it.copy(state = ScanItemState.RUNNING)
                } else {
                    it
                }
            }
        )
    }

    private fun onProgress(event: ScanProgressEvent) {
        val nextIndex = activeOrder.indexOf(event.category) + 1
        val nextCategory = activeOrder.getOrNull(nextIndex)

        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { item ->
                when {
                    item.category == event.category ->
                        item.copy(state = ScanItemState.DONE)
                    nextCategory != null &&
                        item.category == nextCategory &&
                        item.state == ScanItemState.PENDING ->
                        item.copy(state = ScanItemState.RUNNING)
                    else -> item
                }
            },
            progressPercent = if (event.totalCount > 0) {
                event.completedCount * 100 / event.totalCount
            } else {
                0
            }
        )
    }

    private fun labelResFor(
        category: DiagnosticCategory
    ): Int = when (category) {
        DiagnosticCategory.BATTERY -> com.phonedoctor.app.R.string.scan_item_battery
        DiagnosticCategory.STORAGE -> com.phonedoctor.app.R.string.scan_item_storage
        DiagnosticCategory.MEMORY -> com.phonedoctor.app.R.string.scan_item_memory
        DiagnosticCategory.CPU -> com.phonedoctor.app.R.string.scan_item_cpu
        DiagnosticCategory.THERMAL -> com.phonedoctor.app.R.string.scan_item_thermal
        DiagnosticCategory.DISPLAY -> com.phonedoctor.app.R.string.scan_item_display
        DiagnosticCategory.TOUCH -> com.phonedoctor.app.R.string.scan_item_touch
        DiagnosticCategory.AUDIO -> com.phonedoctor.app.R.string.scan_item_audio
        DiagnosticCategory.MICROPHONE -> com.phonedoctor.app.R.string.scan_item_microphone
        DiagnosticCategory.SENSORS -> com.phonedoctor.app.R.string.scan_item_sensors
        DiagnosticCategory.CAMERA -> com.phonedoctor.app.R.string.scan_item_camera
        DiagnosticCategory.CONNECTIVITY -> com.phonedoctor.app.R.string.scan_item_connectivity
    }
}
