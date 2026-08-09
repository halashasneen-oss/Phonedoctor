package com.phonedoctor.app.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phonedoctor.app.PhoneDoctorApp
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.ui.MainActivity
import kotlinx.coroutines.flow.first

/**
 * Runs a lightweight background health check using only the automatic
 * categories (no interactive prompts make sense with the app in the
 * background) and posts a local notification with the result.
 */
class AutoHealthCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PhoneDoctorApp
        val serviceLocator = app.serviceLocator
        val engine = serviceLocator.scanEngine

        val report = engine.run(
            cameraAvailable = false,
            requestUserConfirmation = { true }, // background run: skip interactive categories favorably rather than blocking
            onProgress = { }
        )
        // Interactive categories (display/touch/audio/microphone) are not meaningful
        // without user presence, so exclude them from the persisted background result.
        val filtered = report.copy(
            results = report.results.filterNot {
                it.category == DiagnosticCategory.DISPLAY ||
                    it.category == DiagnosticCategory.TOUCH ||
                    it.category == DiagnosticCategory.AUDIO ||
                    it.category == DiagnosticCategory.MICROPHONE
            }
        )
        serviceLocator.historyRepository.save(filtered)

        val notificationsEnabled = serviceLocator.settingsRepository.settings.first().notificationsEnabled
        if (notificationsEnabled) {
            postNotification(filtered.healthScore)
        }
        return Result.success()
    }

    private fun postNotification(score: Int) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, PhoneDoctorApp.NOTIFICATION_CHANNEL_HEALTH_CHECKS)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle(applicationContext.getString(R.string.notification_auto_check_title))
            .setContentText(applicationContext.getString(R.string.notification_auto_check_text) + " ($score%)")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "auto_health_check"
        private const val NOTIFICATION_ID = 1001
    }
}
