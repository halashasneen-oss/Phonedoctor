package com.screenlens.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.screenlens.app.util.AppearanceManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ScreenLensApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)

        // Applied synchronously (fast local DataStore read) so the very first Activity
        // inflates with the correct theme/locale instead of flashing the default one.
        val settings = runBlocking { ServiceLocator.settingsDataStore.settings.first() }
        AppearanceManager.applyThemeMode(settings.themeMode)
        AppearanceManager.applyAppLanguage(settings.appLanguage)

        createNotificationChannels()
        ServiceLocator.adManager.initialize()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SCREEN_CAPTURE,
                getString(R.string.notif_capture_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FLOATING_LENS,
                getString(R.string.notif_lens_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
        )
    }

    companion object {
        const val CHANNEL_SCREEN_CAPTURE = "screen_capture"
        const val CHANNEL_FLOATING_LENS = "floating_lens"
    }
}
