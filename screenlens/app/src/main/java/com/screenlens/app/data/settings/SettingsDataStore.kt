package com.screenlens.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.screenlens.app.domain.AppLanguage
import com.screenlens.app.domain.AppSettings
import com.screenlens.app.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "screenlens_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val FLOATING_LENS_ENABLED = booleanPreferencesKey("floating_lens_enabled")
        val AUTO_SAVE_HISTORY = booleanPreferencesKey("auto_save_history")
        val HISTORY_LIMIT = intPreferencesKey("history_limit")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            appLanguage = prefs[Keys.APP_LANGUAGE]?.let { tag -> AppLanguage.entries.find { it.tag == tag } }
                ?: AppLanguage.SYSTEM,
            floatingLensEnabled = prefs[Keys.FLOATING_LENS_ENABLED] ?: false,
            autoSaveHistory = prefs[Keys.AUTO_SAVE_HISTORY] ?: true,
            historyLimit = prefs[Keys.HISTORY_LIMIT] ?: 200,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = language.tag }
    }

    suspend fun setFloatingLensEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FLOATING_LENS_ENABLED] = enabled }
    }

    suspend fun setAutoSaveHistory(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SAVE_HISTORY] = enabled }
    }

    suspend fun setHistoryLimit(limit: Int) {
        context.dataStore.edit { it[Keys.HISTORY_LIMIT] = limit }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}
