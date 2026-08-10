package com.screenlens.app.ui.languagemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenlens.app.translate.LanguageCatalog
import com.screenlens.app.translate.ModelDownloadOutcome
import com.screenlens.app.translate.TranslateLanguageInfo
import com.screenlens.app.translate.TranslationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LanguageManagerState(
    val installed: List<TranslateLanguageInfo> = emptyList(),
    val available: List<TranslateLanguageInfo> = emptyList(),
    val pendingCode: String? = null,
    val message: Message? = null
)

sealed class Message {
    data object NoInternet : Message()
    data class Failed(val text: String?) : Message()
}

class LanguageManagerViewModel(private val translationEngine: TranslationEngine) : ViewModel() {

    private val _state = MutableStateFlow(LanguageManagerState())
    val state: StateFlow<LanguageManagerState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val installedCodes = translationEngine.downloadedModelCodes()
            val installed = LanguageCatalog.all.filter { it.code in installedCodes }
            val available = LanguageCatalog.all.filter { it.code !in installedCodes }
            _state.value = _state.value.copy(installed = installed, available = available)
        }
    }

    fun download(code: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(pendingCode = code, message = null)
            when (val result = translationEngine.downloadModel(code, requireWifi = false)) {
                ModelDownloadOutcome.Success -> {
                    _state.value = _state.value.copy(pendingCode = null)
                    refresh()
                }
                ModelDownloadOutcome.NoInternet ->
                    _state.value = _state.value.copy(pendingCode = null, message = Message.NoInternet)
                is ModelDownloadOutcome.Failed ->
                    _state.value = _state.value.copy(pendingCode = null, message = Message.Failed(result.message))
            }
        }
    }

    fun remove(code: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(pendingCode = code, message = null)
            translationEngine.deleteModel(code)
            _state.value = _state.value.copy(pendingCode = null)
            refresh()
        }
    }
}
