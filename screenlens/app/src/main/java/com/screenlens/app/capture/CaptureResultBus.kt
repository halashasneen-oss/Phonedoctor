package com.screenlens.app.capture

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class CaptureEvent {
    data object PermissionDenied : CaptureEvent()
    data object Capturing : CaptureEvent()
    data class Captured(val bitmap: Bitmap) : CaptureEvent()
    data class Failed(val message: String?) : CaptureEvent()
}

/**
 * Process-local bridge between [ScreenCaptureService] (which owns the MediaProjection
 * lifecycle and may be started from the Floating Lens bubble, not just an in-app
 * screen) and whichever UI is currently listening for a capture result.
 */
object CaptureResultBus {
    private val _events = MutableSharedFlow<CaptureEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CaptureEvent> = _events.asSharedFlow()

    suspend fun emit(event: CaptureEvent) {
        _events.emit(event)
    }
}
