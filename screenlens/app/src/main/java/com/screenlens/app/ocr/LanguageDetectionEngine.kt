package com.screenlens.app.ocr

import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Wraps Google ML Kit's on-device Language Identification model. */
class LanguageDetectionEngine {

    private val identifier by lazy { LanguageIdentification.getClient() }

    /** Returns a BCP-47 language tag (e.g. "en"), or null if the language could not be determined. */
    suspend fun detect(text: String): String? = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext null
        val tag = runCatching { identifier.identifyLanguage(text).await() }.getOrNull()
        if (tag.isNullOrEmpty() || tag == "und") null else tag
    }
}
