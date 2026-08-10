package com.screenlens.app.ui.translator

sealed class TranslateUiState {
    data object Idle : TranslateUiState()
    data object Translating : TranslateUiState()
    data class Success(val text: String) : TranslateUiState()
    data class ModelsNotInstalled(val missingCodes: List<String>) : TranslateUiState()
    data object NoInternet : TranslateUiState()
    data class Error(val message: String?) : TranslateUiState()
}

sealed class ModelDownloadUiState {
    data object Idle : ModelDownloadUiState()
    data object Downloading : ModelDownloadUiState()
    data object Done : ModelDownloadUiState()
    data object NoInternet : ModelDownloadUiState()
    data class Failed(val message: String?) : ModelDownloadUiState()
}
