package com.screenlens.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrOutcomeTest {

    @Test
    fun `success carries the recognized text`() {
        val outcome = OcrOutcome.Success("Hello world")
        assertTrue(outcome is OcrOutcome.Success)
        assertEquals("Hello world", (outcome as OcrOutcome.Success).text)
    }

    @Test
    fun `no text found and success are distinct outcomes`() {
        val noText: OcrOutcome = OcrOutcome.NoTextFound
        val success: OcrOutcome = OcrOutcome.Success("text")
        assertTrue(noText != success)
    }
}
