package com.screenlens.app.ui.translator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenlens.app.data.repository.HistoryRepository
import com.screenlens.app.data.settings.SettingsDataStore
import com.screenlens.app.domain.HistoryItem
import com.screenlens.app.domain.HistoryType
import com.screenlens.app.translate.LanguageCatalog
import com.screenlens.app.translate.ModelDownloadOutcome
import com.screenlens.app.translate.TranslateLanguageInfo
import com.screenlens.app.translate.TranslationEngine
import com.screenlens.app.translate.TranslationOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TranslatorViewModel(
    initialText: String?,
    initialSourceLanguage: String?,
    private val translationEngine: TranslationEngine,
    private val historyRepository: HistoryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _sourceLanguage = MutableStateFlow(
        initialSourceLanguage?.let { LanguageCatalog.byCode(it) } ?: LanguageCatalog.byCode("en")
    )
    val sourceLanguage: StateFlow<TranslateLanguageInfo?> = _sourceLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow(LanguageCatalog.byCode("ar"))
    val targetLanguage: StateFlow<TranslateLanguageInfo?> = _targetLanguage.asStateFlow()

    private val _inputText = MutableStateFlow(initialText.orEmpty())
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _uiState = MutableStateFlow<TranslateUiState>(TranslateUiState.Idle)
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<ModelDownloadUiState>(ModelDownloadUiState.Idle)
    val downloadState: StateFlow<ModelDownloadUiState> = _downloadState.asStateFlow()

    private var pendingTranslate: Job? = null

    init {
        if (_inputText.value.isNotBlank()) requestTranslate()
    }

    fun setSourceLanguage(language: TranslateLanguageInfo) {
        _sourceLanguage.value = language
        requestTranslate()
    }

    fun setTargetLanguage(language: TranslateLanguageInfo) {
        _targetLanguage.value = language
        requestTranslate()
    }

    fun swapLanguages() {
        val source = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = source
        requestTranslate()
    }

    fun setInputText(text: String) {
        _inputText.value = text
        requestTranslate(debounce = true)
    }

    private fun requestTranslate(debounce: Boolean = false) {
        pendingTranslate?.cancel()
        val text = _inputText.value
        if (text.isBlank()) {
            _uiState.value = TranslateUiState.Idle
            return
        }
        val source = _sourceLanguage.value
        val target = _targetLanguage.value
        if (source == null || target == null) return

        pendingTranslate = viewModelScope.launch {
            if (debounce) delay(400)
            _uiState.value = TranslateUiState.Translating
            when (val outcome = translationEngine.translate(text, source.code, target.code)) {
                is TranslationOutcome.Success -> {
                    _uiState.value = TranslateUiState.Success(outcome.translatedText)
                    maybeAutoSave(text, outcome.translatedText, source.code, target.code)
                }
                is TranslationOutcome.ModelsNotInstalled ->
                    _uiState.value = TranslateUiState.ModelsNotInstalled(outcome.missingLanguageCodes)
                TranslationOutcome.NoInternet -> _uiState.value = TranslateUiState.NoInternet
                is TranslationOutcome.Failed -> _uiState.value = TranslateUiState.Error(outcome.message)
            }
        }
    }

    private suspend fun maybeAutoSave(original: String, translated: String, source: String, target: String) {
        if (!settingsDataStore.settings.first().autoSaveHistory) return
        saveTranslation(original, translated, source, target)
    }

    fun saveManually() {
        val state = _uiState.value
        if (state !is TranslateUiState.Success) return
        val source = _sourceLanguage.value?.code ?: return
        val target = _targetLanguage.value?.code ?: return
        viewModelScope.launch { saveTranslation(_inputText.value, state.text, source, target) }
    }

    private suspend fun saveTranslation(original: String, translated: String, source: String, target: String) {
        val limit = settingsDataStore.settings.first().historyLimit
        historyRepository.save(
            HistoryItem(
                createdAt = System.currentTimeMillis(),
                type = HistoryType.TRANSLATION,
                originalText = original,
                translatedText = translated,
                sourceLanguage = source,
                targetLanguage = target,
                detectedLanguage = source
            ),
            limit
        )
    }

    fun downloadModel(code: String, requireWifi: Boolean) {
        viewModelScope.launch {
            _downloadState.value = ModelDownloadUiState.Downloading
            when (val result = translationEngine.downloadModel(code, requireWifi)) {
                ModelDownloadOutcome.Success -> {
                    _downloadState.value = ModelDownloadUiState.Done
                    requestTranslate()
                }
                ModelDownloadOutcome.NoInternet -> _downloadState.value = ModelDownloadUiState.NoInternet
                is ModelDownloadOutcome.Failed -> _downloadState.value = ModelDownloadUiState.Failed(result.message)
            }
        }
    }
}
