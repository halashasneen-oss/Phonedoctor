package com.screenlens.app.translate

sealed class TranslationOutcome {
    data class Success(val translatedText: String) : TranslationOutcome()
    data class ModelsNotInstalled(val missingLanguageCodes: List<String>) : TranslationOutcome()
    data object NoInternet : TranslationOutcome()
    data class Failed(val message: String?) : TranslationOutcome()
}

sealed class ModelDownloadOutcome {
    data object Success : ModelDownloadOutcome()
    data object NoInternet : ModelDownloadOutcome()
    data class Failed(val message: String?) : ModelDownloadOutcome()
}
