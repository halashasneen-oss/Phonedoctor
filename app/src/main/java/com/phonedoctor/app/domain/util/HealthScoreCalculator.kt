package com.phonedoctor.app.domain.util

import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.DiagnosticConfidence
import com.phonedoctor.app.domain.model.ScoreImpact
import com.phonedoctor.app.domain.model.TestStatus
import kotlin.math.roundToInt

/**
 * Health Score 2.0.
 *
 * Only observations explicitly classified as HEALTH evidence can influence
 * the score. Capability checks and temporary states remain visible in reports
 * without falsely making the phone look healthy or broken.
 *
 * Confidence is used as a weight so a high-confidence measurement contributes
 * more than an estimate while preserving a simple 0-100 result.
 */
object HealthScoreCalculator {

    private const val SCORE_EXCELLENT = 100
    private const val SCORE_GOOD = 80
    private const val SCORE_FAIR = 50
    private const val SCORE_POOR = 15

    fun calculate(results: List<CategoryResult>): Int {
        val scorable = results.filter {
            it.scoreImpact == ScoreImpact.HEALTH &&
                it.status != TestStatus.UNAVAILABLE
        }
        if (scorable.isEmpty()) return 0

        val weightedScore = scorable.sumOf {
            statusScore(it.status) * confidenceWeight(it.confidence)
        }
        val totalWeight = scorable.sumOf { confidenceWeight(it.confidence) }
        if (totalWeight <= 0.0) return 0

        return (weightedScore / totalWeight)
            .roundToInt()
            .coerceIn(0, 100)
    }

    fun scoredCategoryCount(results: List<CategoryResult>): Int =
        results.count {
            it.scoreImpact == ScoreImpact.HEALTH &&
                it.status != TestStatus.UNAVAILABLE
        }

    fun statusScore(status: TestStatus): Int = when (status) {
        TestStatus.EXCELLENT -> SCORE_EXCELLENT
        TestStatus.GOOD -> SCORE_GOOD
        TestStatus.FAIR -> SCORE_FAIR
        TestStatus.POOR -> SCORE_POOR
        TestStatus.UNAVAILABLE -> SCORE_GOOD
    }

    fun scoreToStatus(score: Int): TestStatus = when {
        score >= 90 -> TestStatus.EXCELLENT
        score >= 70 -> TestStatus.GOOD
        score >= 40 -> TestStatus.FAIR
        else -> TestStatus.POOR
    }

    private fun confidenceWeight(confidence: DiagnosticConfidence): Double = when (confidence) {
        DiagnosticConfidence.HIGH -> 1.0
        DiagnosticConfidence.MEDIUM -> 0.75
        DiagnosticConfidence.LOW -> 0.5
    }
}
