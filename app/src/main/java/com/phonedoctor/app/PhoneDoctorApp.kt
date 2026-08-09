package com.phonedoctor.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.phonedoctor.app.ads.AdManager
import com.phonedoctor.app.data.datastore.AppLanguage
import com.phonedoctor.app.data.datastore.AppThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PhoneDoctorApp : Application() {

    val serviceLocator: ServiceLocator by lazy { ServiceLocator(this) }

    override fun onCreate() {
        super.onCreate()
        applySavedThemeAndLanguage()
        createNotificationChannel()
        AdManager.initialize(this)
    }

    private fun applySavedThemeAndLanguage() {
        // Applied once, synchronously, before any Activity/Window is created so
        // there is no theme flash. The settings file is tiny and local.
        val settings = runBlocking { serviceLocator.settingsRepository.settings.first() }

        val nightMode = when (settings.themeMode) {
            AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        settings.language?.let { language ->
            val tag = if (language == AppLanguage.ARABIC) "ar" else "en"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_HEALTH_CHECKS,
                getString(R.string.notification_channel_health_checks),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_health_checks_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_HEALTH_CHECKS = "health_checks"
    }
}
