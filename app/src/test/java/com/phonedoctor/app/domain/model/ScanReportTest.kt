package com.phonedoctor.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanReportTest {

    @Test
    fun `legacy-compatible reports default to deep mode`() {
        val report = ScanReport(
            timestampMillis = 1L,
            healthScore = 100,
            results = emptyList()
        )

        assertEquals(ScanMode.DEEP, report.scanMode)
    }

    @Test
    fun `performance reports preserve their explicit mode`() {
        val report = ScanReport(
            timestampMillis = 1L,
            healthScore = 0,
            results = emptyList(),
            scanMode = ScanMode.PERFORMANCE
        )

        assertEquals(ScanMode.PERFORMANCE, report.scanMode)
    }
}
