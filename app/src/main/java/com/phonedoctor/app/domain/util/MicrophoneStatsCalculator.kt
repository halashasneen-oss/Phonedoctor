package com.phonedoctor.app.domain.util

import com.phonedoctor.app.domain.model.MicrophoneLevelStats

object MicrophoneStatsCalculator {

    private const val MAX_AMPLITUDE = 32767
    private const val CLIPPING_THRESHOLD_PERCENT = 98
    private const val SILENT_THRESHOLD_PERCENT = 2

    fun calculate(amplitudes: List<Int>): MicrophoneLevelStats {
        if (amplitudes.isEmpty()) {
            return MicrophoneLevelStats(
                sampleCount = 0,
                averagePercent = 0,
                peakPercent = 0,
                clippingSamples = 0,
                silentSamples = 0
            )
        }

        val percents = amplitudes.map(::toPercent)
        return MicrophoneLevelStats(
            sampleCount = percents.size,
            averagePercent = percents.average().toInt().coerceIn(0, 100),
            peakPercent = percents.maxOrNull()?.coerceIn(0, 100) ?: 0,
            clippingSamples = percents.count { it >= CLIPPING_THRESHOLD_PERCENT },
            silentSamples = percents.count { it <= SILENT_THRESHOLD_PERCENT }
        )
    }

    fun toPercent(amplitude: Int): Int {
        return ((amplitude.coerceIn(0, MAX_AMPLITUDE).toLong() * 100L) / MAX_AMPLITUDE)
            .toInt()
            .coerceIn(0, 100)
    }
}
