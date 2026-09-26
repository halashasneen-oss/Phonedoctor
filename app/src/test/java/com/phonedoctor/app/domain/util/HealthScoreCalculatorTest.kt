package com.phonedoctor.app.domain.util

import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.DiagnosticConfidence
import com.phonedoctor.app.domain.model.DiagnosticEvidenceType
import com.phonedoctor.app.domain.model.ScoreImpact
import com.phonedoctor.app.domain.model.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthScoreCalculatorTest {

    @Test
    fun `all excellent health results score 100`() {
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
    fun `unavailable health categories are excluded from average`() {
        val results = listOf(
            CategoryResult(DiagnosticCategory.BATTERY, TestStatus.EXCELLENT, "ok"),
            CategoryResult(DiagnosticCategory.CAMERA, TestStatus.UNAVAILABLE, "no camera")
        )
        assertEquals(100, HealthScoreCalculator.calculate(results))
    }

    @Test
    fun `informational warnings do not reduce health score`() {
        val results = listOf(
            CategoryResult(
                category = DiagnosticCategory.BATTERY,
                status = TestStatus.EXCELLENT,
                summary = "healthy",
                evidenceType = DiagnosticEvidenceType.MEASURED,
                scoreImpact = ScoreImpact.HEALTH
            ),
            CategoryResult(
                category = DiagnosticCategory.STORAGE,
                status = TestStatus.POOR,
                summary = "98% used",
                evidenceType = DiagnosticEvidenceType.CURRENT_STATE,
                scoreImpact = ScoreImpact.INFORMATIONAL
            ),
            CategoryResult(
                category = DiagnosticCategory.CONNECTIVITY,
                status = TestStatus.FAIR,
                summary = "offline",
                evidenceType = DiagnosticEvidenceType.CURRENT_STATE,
                scoreImpact = ScoreImpact.INFORMATIONAL
            )
        )

        assertEquals(100, HealthScoreCalculator.calculate(results))
        assertEquals(1, HealthScoreCalculator.scoredCategoryCount(results))
    }

    @Test
    fun `confidence weights health evidence`() {
        val results = listOf(
            CategoryResult(
                DiagnosticCategory.BATTERY,
                TestStatus.EXCELLENT,
                "high confidence",
                confidence = DiagnosticConfidence.HIGH
            ),
            CategoryResult(
                DiagnosticCategory.DISPLAY,
                TestStatus.POOR,
                "low confidence",
                confidence = DiagnosticConfidence.LOW
            )
        )

        // (100*1.0 + 15*0.5) / 1.5 = 71.67 -> 72
        assertEquals(72, HealthScoreCalculator.calculate(results))
    }

    @Test
    fun `mixed statuses average correctly with equal confidence`() {
        val results = listOf(
            CategoryResult(DiagnosticCategory.BATTERY, TestStatus.EXCELLENT, "ok"),
            CategoryResult(DiagnosticCategory.STORAGE, TestStatus.GOOD, "ok"),
            CategoryResult(DiagnosticCategory.MEMORY, TestStatus.FAIR, "ok"),
            CategoryResult(DiagnosticCategory.CPU, TestStatus.POOR, "ok")
        )
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
