package com.screenlens.app.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Transparent, non-visual trampoline. Android requires the MediaProjection consent
 * intent to be launched with startActivityForResult from an Activity, so this exists
 * purely to show that system dialog (from anywhere — the in-app Scan screen or the
 * Floating Lens bubble) and hand the result to [ScreenCaptureService].
 */
class MediaProjectionPermissionActivity : AppCompatActivity() {

    private val requestCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
        } else {
            lifecycleScope.launch { CaptureResultBus.emit(CaptureEvent.PermissionDenied) }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        requestCapture.launch(projectionManager.createScreenCaptureIntent())
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(
                Intent(context, MediaProjectionPermissionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
