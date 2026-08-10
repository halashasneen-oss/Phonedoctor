package com.screenlens.app

import android.content.Context
import com.screenlens.app.ads.AdManager
import com.screenlens.app.billing.BillingRepository
import com.screenlens.app.data.db.AppDatabase
import com.screenlens.app.data.repository.HistoryRepository
import com.screenlens.app.data.settings.SettingsDataStore
import com.screenlens.app.ocr.LanguageDetectionEngine
import com.screenlens.app.ocr.TextRecognitionEngine
import com.screenlens.app.translate.TranslationEngine

/**
 * Minimal manual dependency container. ScreenLens has a small, single dependency
 * graph, so a full DI framework (Hilt/Dagger) would add build complexity without
 * a matching benefit — every dependency below is a cheap, lazily-created singleton.
 */
object ServiceLocator {

    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context =
        appContext ?: throw IllegalStateException("ServiceLocator.init() was not called")

    val database: AppDatabase by lazy { AppDatabase.getInstance(requireContext()) }

    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(requireContext()) }

    val historyRepository: HistoryRepository by lazy { HistoryRepository(database.historyDao()) }

    val textRecognitionEngine: TextRecognitionEngine by lazy { TextRecognitionEngine() }

    val languageDetectionEngine: LanguageDetectionEngine by lazy { LanguageDetectionEngine() }

    val translationEngine: TranslationEngine by lazy { TranslationEngine(requireContext()) }

    val adManager: AdManager by lazy { AdManager(requireContext()) }

    val billingRepository: BillingRepository by lazy { BillingRepository(requireContext()) }
}
