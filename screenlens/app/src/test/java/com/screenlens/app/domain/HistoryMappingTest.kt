package com.screenlens.app.domain

import com.screenlens.app.data.db.HistoryEntity
import com.screenlens.app.data.db.toDomain
import com.screenlens.app.data.db.toEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryMappingTest {

    @Test
    fun `entity to domain to entity round trips without data loss`() {
        val original = HistoryEntity(
            id = 42,
            createdAt = 123456789L,
            type = "TRANSLATION",
            originalText = "Hello",
            translatedText = "Bonjour",
            sourceLanguage = "en",
            targetLanguage = "fr",
            detectedLanguage = "en"
        )

        val roundTripped = original.toDomain().toEntity()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `unknown type string falls back to SCREEN instead of crashing`() {
        val entity = HistoryEntity(
            createdAt = 1,
            type = "NOT_A_REAL_TYPE",
            originalText = "text",
            translatedText = null,
            sourceLanguage = null,
            targetLanguage = null,
            detectedLanguage = null
        )

        assertEquals(HistoryType.SCREEN, entity.toDomain().type)
    }
}
