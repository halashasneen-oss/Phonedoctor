package com.phonedoctor.app.domain.util

import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthScoreCalculatorTest {

    @Test
    fun `all excellent results score 100`() {
        val results = DiagnosticCategory.entries.map {
            CategoryResult(it, TestStatus.EXCELLENT, "ok")
        }
        assertEquals(100, HealthScoreCalculator.calculate(results))
    }

    @Test
    fun `empty results score 0`() {
        assertEquals(0, HealthScoreCalculator.calculate(emptyList()))
    }

    @Test
    fun `unavailable categories are excluded from the average`() {
        val results = listOf(
            CategoryResult(DiagnosticCategory.BATTERY, TestStatus.EXCELLENT, "ok"),
            CategoryResult(DiagnosticCategory.CAMERA, TestStatus.UNAVAILABLE, "no camera")
        )
        // Only BATTERY (EXCELLENT=100) should count, CAMERA is skipped entirely.
        assertEquals(100, HealthScoreCalculator.calculate(results))
    }

    @Test
    fun `mixed statuses average correctly`() {
        val results = listOf(
            CategoryResult(DiagnosticCategory.BATTERY, TestStatus.EXCELLENT, "ok"), // 100
            CategoryResult(DiagnosticCategory.STORAGE, TestStatus.GOOD, "ok"), // 80
            CategoryResult(DiagnosticCategory.MEMORY, TestStatus.FAIR, "ok"), // 50
            CategoryResult(DiagnosticCategory.CPU, TestStatus.POOR, "ok") // 15
        )
        // (100 + 80 + 50 + 15) / 4 = 61.25 -> rounds to 61
        assertEquals(61, HealthScoreCalculator.calculate(results))
    }

    @Test
    fun `score to status mapping matches thresholds`() {
        assertEquals(TestStatus.EXCELLENT, HealthScoreCalculator.scoreToStatus(95))
        assertEquals(TestStatus.GOOD, HealthScoreCalculator.scoreToStatus(75))
        assertEquals(TestStatus.FAIR, HealthScoreCalculator.scoreToStatus(50))
        assertEquals(TestStatus.POOR, HealthScoreCalculator.scoreToStatus(20))
    }
}
