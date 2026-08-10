package com.screenlens.app.ocr

/** Result of running on-device text recognition on a bitmap. */
sealed class OcrOutcome {
    data class Success(val text: String) : OcrOutcome()
    data object NoTextFound : OcrOutcome()
    data class Failed(val message: String?) : OcrOutcome()
}
