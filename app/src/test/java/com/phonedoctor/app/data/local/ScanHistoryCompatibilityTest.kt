package com.phonedoctor.app.data.local

import com.phonedoctor.app.data.local.entity.ScanHistoryEntity
import com.phonedoctor.app.domain.model.ScanMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanHistoryCompatibilityTest {

    @Test
    fun `legacy-compatible entity defaults to deep mode`() {
        val entity = ScanHistoryEntity(
            id = 1L,
            timestampMillis = 123L,
            healthScore = 88,
            results = emptyList()
        )

        assertEquals(ScanMode.DEEP.name, entity.scanMode)
    }

    @Test
    fun `database migration targets version two`() {
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)
    }
}
