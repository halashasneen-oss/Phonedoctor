package com.phonedoctor.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "phone_doctor_settings")

enum class AppThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage(val tag: String) { ENGLISH("en"), ARABIC("ar") }

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val language: AppLanguage? = null, // null = follow system locale
    val notificationsEnabled: Boolean = true,
    val autoHealthCheckEnabled: Boolean = false,
    val isPremium: Boolean = false,
    val onboardingCompleted: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTO_HEALTH_CHECK_ENABLED = booleanPreferencesKey("auto_health_check_enabled")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() },
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            autoHealthCheckEnabled = prefs[Keys.AUTO_HEALTH_CHECK_ENABLED] ?: false,
            isPremium = prefs[Keys.IS_PREMIUM] ?: false,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAutoHealthCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_HEALTH_CHECK_ENABLED] = enabled }
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { it[Keys.IS_PREMIUM] = isPremium }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}
