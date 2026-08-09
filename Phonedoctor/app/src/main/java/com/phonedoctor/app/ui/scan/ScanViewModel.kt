package com.phonedoctor.app.ui.scan

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.data.repository.ScanProgressEvent
import com.phonedoctor.app.domain.model.DiagnosticCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanListItem(
    val category: DiagnosticCategory,
    val labelRes: Int,
    val state: ScanItemState
)

enum class ScanItemState { PENDING, RUNNING, DONE }

data class ScanUiState(
    val items: List<ScanListItem>,
    val progressPercent: Int = 0,
    val pendingInteraction: DiagnosticCategory? = null,
    val finishedReportId: Long? = null,
    val cancelled: Boolean = false
)

class ScanViewModel(
    private val serviceLocator: ServiceLocator,
    private val appContext: Context
) : ViewModel() {

    private val categoryOrder = listOf(
        DiagnosticCategory.BATTERY, DiagnosticCategory.STORAGE, DiagnosticCategory.MEMORY,
        DiagnosticCategory.CPU, DiagnosticCategory.DISPLAY, DiagnosticCategory.TOUCH,
        DiagnosticCategory.AUDIO, DiagnosticCategory.MICROPHONE, DiagnosticCategory.SENSORS,
        DiagnosticCategory.CAMERA, DiagnosticCategory.CONNECTIVITY
    )

    private val _uiState = MutableStateFlow(
        ScanUiState(items = categoryOrder.map { ScanListItem(it, labelResFor(it), ScanItemState.PENDING) })
    )
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var interactionResponse: CompletableDeferred<Boolean>? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val cameraAvailable = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            markRunning(categoryOrder.first())

            val report = serviceLocator.scanEngine.run(
                cameraAvailable = cameraAvailable,
                requestUserConfirmation = { category -> awaitUserConfirmation(category) },
                onProgress = { event: ScanProgressEvent -> onProgress(event) }
            )

            val id = serviceLocator.historyRepository.save(report)
            _uiState.value = _uiState.value.copy(finishedReportId = id)
        }
    }

    private suspend fun awaitUserConfirmation(category: DiagnosticCategory): Boolean {
        markRunning(category)
        val deferred = CompletableDeferred<Boolean>()
        interactionResponse = deferred
        _uiState.value = _uiState.value.copy(pendingInteraction = category)
        val result = deferred.await()
        _uiState.value = _uiState.value.copy(pendingInteraction = null)
        return result
    }

    fun respondToInteraction(confirmed: Boolean) {
        interactionResponse?.complete(confirmed)
        interactionResponse = null
    }

    fun cancel() {
        _uiState.value = _uiState.value.copy(cancelled = true)
    }

    private fun markRunning(category: DiagnosticCategory) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (it.category == category && it.state == ScanItemState.PENDING) it.copy(state = ScanItemState.RUNNING) else it
            }
        )
    }

    private fun onProgress(event: ScanProgressEvent) {
        val nextIndex = categoryOrder.indexOf(event.category) + 1
        val nextCategory = categoryOrder.getOrNull(nextIndex)
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { item ->
                when {
                    item.category == event.category -> item.copy(state = ScanItemState.DONE)
                    nextCategory != null && item.category == nextCategory && item.state == ScanItemState.PENDING ->
                        item.copy(state = ScanItemState.RUNNING)
                    else -> item
                }
            },
            progressPercent = (event.completedCount * 100 / event.totalCount)
        )
    }

    private fun labelResFor(category: DiagnosticCategory): Int = when (category) {
        DiagnosticCategory.BATTERY -> com.phonedoctor.app.R.string.scan_item_battery
        DiagnosticCategory.STORAGE -> com.phonedoctor.app.R.string.scan_item_storage
        DiagnosticCategory.MEMORY -> com.phonedoctor.app.R.string.scan_item_memory
        DiagnosticCategory.CPU -> com.phonedoctor.app.R.string.scan_item_cpu
        DiagnosticCategory.DISPLAY -> com.phonedoctor.app.R.string.scan_item_display
        DiagnosticCategory.TOUCH -> com.phonedoctor.app.R.string.scan_item_touch
        DiagnosticCategory.AUDIO -> com.phonedoctor.app.R.string.scan_item_audio
        DiagnosticCategory.MICROPHONE -> com.phonedoctor.app.R.string.scan_item_microphone
        DiagnosticCategory.SENSORS -> com.phonedoctor.app.R.string.scan_item_sensors
        DiagnosticCategory.CAMERA -> com.phonedoctor.app.R.string.scan_item_camera
        DiagnosticCategory.CONNECTIVITY -> com.phonedoctor.app.R.string.scan_item_connectivity
    }
}
