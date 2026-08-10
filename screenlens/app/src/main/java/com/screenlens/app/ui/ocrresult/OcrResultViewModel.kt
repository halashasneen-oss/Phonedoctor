package com.screenlens.app.ui.ocrresult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenlens.app.data.repository.HistoryRepository
import com.screenlens.app.data.settings.SettingsDataStore
import com.screenlens.app.domain.HistoryItem
import com.screenlens.app.domain.HistoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OcrResultViewModel(
    initialText: String,
    val detectedLanguage: String?,
    val sourceType: HistoryType,
    private val historyRepository: HistoryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _text = MutableStateFlow(initialText)
    val text: StateFlow<String> = _text.asStateFlow()

    private val _savedId = MutableStateFlow<Long?>(null)
    val savedId: StateFlow<Long?> = _savedId.asStateFlow()

    init {
        viewModelScope.launch {
            if (settingsDataStore.settings.first().autoSaveHistory) {
                save()
            }
        }
    }

    fun updateText(newText: String) {
        _text.value = newText
    }

    fun save() {
        if (_savedId.value != null) return
        viewModelScope.launch {
            val limit = settingsDataStore.settings.first().historyLimit
            val id = historyRepository.save(
                HistoryItem(
                    createdAt = System.currentTimeMillis(),
                    type = sourceType,
                    originalText = _text.value,
                    translatedText = null,
                    sourceLanguage = detectedLanguage,
                    targetLanguage = null,
                    detectedLanguage = detectedLanguage
                ),
                limit
            )
            _savedId.value = id
        }
    }
}
