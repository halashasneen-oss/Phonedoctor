package com.screenlens.app.capture

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.screenlens.app.R
import com.screenlens.app.ScreenLensApp
import com.screenlens.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Captures exactly one frame of the screen via MediaProjection + VirtualDisplay +
 * ImageReader, emits it on [CaptureResultBus], then tears everything down
 * immediately — MediaProjection is never left running in the background.
 */
class ScreenCaptureService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var handlerThread: HandlerThread? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var finished = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            teardown()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(), foregroundServiceType())
        serviceScope.launch { CaptureResultBus.emit(CaptureEvent.Capturing) }
        beginCapture(resultCode, data)
        return START_NOT_STICKY
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }

    private fun beginCapture(resultCode: Int, data: Intent) {
        try {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = projectionManager.getMediaProjection(resultCode, data)
                ?: throw IllegalStateException("MediaProjection unavailable")
            mediaProjection = projection

            val thread = HandlerThread("ScreenLens-Capture").apply { start() }
            handlerThread = thread
            val handler = Handler(thread.looper)

            // Required since Android 14: register a callback before creating the
            // VirtualDisplay, or MediaProjection throws.
            projection.registerCallback(projectionCallback, handler)

            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val densityDpi = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            reader.setOnImageAvailableListener({ onImageAvailable(reader, width, height) }, handler)

            virtualDisplay = projection.createVirtualDisplay(
                "ScreenLens-Capture",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )
        } catch (t: Throwable) {
            serviceScope.launch { CaptureResultBus.emit(CaptureEvent.Failed(t.message)) }
            teardown()
        }
    }

    private fun onImageAvailable(reader: ImageReader, width: Int, height: Int) {
        if (finished) return
        val image = try {
            reader.acquireLatestImage()
        } catch (t: Throwable) {
            null
        } ?: return

        val bitmap = try {
            val plane = image.planes[0]
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val raw = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            raw.copyPixelsFromBuffer(plane.buffer)
            if (rowPadding == 0) raw else Bitmap.createBitmap(raw, 0, 0, width, height).also { raw.recycle() }
        } catch (t: Throwable) {
            null
        } finally {
            image.close()
        }

        finished = true
        if (bitmap != null) {
            serviceScope.launch { CaptureResultBus.emit(CaptureEvent.Captured(bitmap)) }
        } else {
            serviceScope.launch { CaptureResultBus.emit(CaptureEvent.Failed(null)) }
        }
        teardown()
    }

    private fun teardown() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.let {
            it.unregisterCallback(projectionCallback)
            it.stop()
        }
        mediaProjection = null
        handlerThread?.quitSafely()
        handlerThread = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        teardown()
        serviceScope.cancel()
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, ScreenLensApp.CHANNEL_SCREEN_CAPTURE)
            .setContentTitle(getString(R.string.notif_capture_title))
            .setContentText(getString(R.string.notif_capture_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
