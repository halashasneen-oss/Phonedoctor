package com.phonedoctor.app.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonedoctor.app.ServiceLocator
import com.phonedoctor.app.data.datastore.AppLanguage
import com.phonedoctor.app.data.datastore.AppSettings
import com.phonedoctor.app.data.datastore.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    val settings: StateFlow<AppSettings> = serviceLocator.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { serviceLocator.settingsRepository.setThemeMode(mode) }
        val nightMode = when (mode) {
            AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { serviceLocator.settingsRepository.setLanguage(language) }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { serviceLocator.settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun setAutoHealthCheckEnabled(enabled: Boolean) {
        viewModelScope.launch { serviceLocator.settingsRepository.setAutoHealthCheckEnabled(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch { serviceLocator.historyRepository.clearAll() }
    }
}
