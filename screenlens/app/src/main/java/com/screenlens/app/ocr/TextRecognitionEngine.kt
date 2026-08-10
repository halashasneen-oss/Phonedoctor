package com.screenlens.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Wraps Google ML Kit's on-device Text Recognition (v2, Latin script model).
 *
 * Known limitation: ML Kit's on-device recognizer only decodes Latin-alphabet
 * script (English, French, Spanish, German, Italian, Portuguese, Turkish, etc.).
 * It cannot read Arabic, Chinese, Japanese, Korean or Devanagari glyphs — there is
 * no on-device ML Kit model for those scripts. That limitation is surfaced to the
 * user explicitly (see error_language_not_supported) rather than silently failing.
 */
class TextRecognitionEngine {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun recognize(bitmap: Bitmap): OcrOutcome = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            val text = result.text.trim()
            if (text.isEmpty()) OcrOutcome.NoTextFound else OcrOutcome.Success(text)
        } catch (t: Throwable) {
            OcrOutcome.Failed(t.message)
        }
    }
}
