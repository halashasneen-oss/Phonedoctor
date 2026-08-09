package com.phonedoctor.app.domain.util

import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.TestStatus
import kotlin.math.roundToInt

/**
 * Converts a set of category results into a single 0-100 health score.
 * Categories with [TestStatus.UNAVAILABLE] are excluded entirely rather than
 * penalized, since unavailability reflects a platform/API limitation, not a
 * problem with the device.
 */
object HealthScoreCalculator {

    private const val SCORE_EXCELLENT = 100
    private const val SCORE_GOOD = 80
    private const val SCORE_FAIR = 50
    private const val SCORE_POOR = 15

    fun calculate(results: List<CategoryResult>): Int {
        val scorable = results.filter { it.status != TestStatus.UNAVAILABLE }
        if (scorable.isEmpty()) return 0
        val total = scorable.sumOf { statusScore(it.status) }
        return (total.toDouble() / scorable.size).roundToInt().coerceIn(0, 100)
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
}
