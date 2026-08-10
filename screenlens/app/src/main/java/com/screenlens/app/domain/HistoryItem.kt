package com.screenlens.app.domain

/** Where a saved History entry originated from. Mirrors the source tool used to produce it. */
enum class HistoryType {
    SCREEN,
    IMAGE,
    CAMERA,
    CLIPBOARD,
    QR,
    /** A translation saved directly from the Translator tool, not tied to an OCR/QR capture. */
    TRANSLATION
}

/**
 * A single saved entry: either an OCR read (optionally translated) or a QR/barcode scan.
 * Only text is persisted — screen captures and photos are never written to History,
 * matching ScreenLens's local-only, no-persistent-screenshots privacy model.
 */
data class HistoryItem(
    val id: Long = 0,
    val createdAt: Long,
    val type: HistoryType,
    val originalText: String,
    val translatedText: String?,
    val sourceLanguage: String?,
    val targetLanguage: String?,
    val detectedLanguage: String?
)
