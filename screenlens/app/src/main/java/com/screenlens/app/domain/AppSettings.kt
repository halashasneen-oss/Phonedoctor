package com.screenlens.app.domain

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    ARABIC("ar")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val floatingLensEnabled: Boolean = false,
    val autoSaveHistory: Boolean = true,
    /** 0 means unlimited. */
    val historyLimit: Int = 200,
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false
)
