package com.phonedoctor.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MicrophoneStatsCalculatorTest {

    @Test
    fun `empty samples return zero stats`() {
        val stats = MicrophoneStatsCalculator.calculate(emptyList())
        assertEquals(0, stats.sampleCount)
        assertEquals(0, stats.averagePercent)
        assertEquals(0, stats.peakPercent)
    }

    @Test
    fun `amplitude converts into bounded percent`() {
        assertEquals(0, MicrophoneStatsCalculator.toPercent(-1))
        assertEquals(50, MicrophoneStatsCalculator.toPercent(16384))
        assertEquals(100, MicrophoneStatsCalculator.toPercent(40000))
    }

    @Test
    fun `clipping and silent samples are counted`() {
        val stats = MicrophoneStatsCalculator.calculate(
            listOf(0, 100, 16_000, 32_767, 32_767)
        )

        assertEquals(5, stats.sampleCount)
        assertEquals(2, stats.clippingSamples)
        assertEquals(2, stats.silentSamples)
        assertEquals(100, stats.peakPercent)
    }
}
