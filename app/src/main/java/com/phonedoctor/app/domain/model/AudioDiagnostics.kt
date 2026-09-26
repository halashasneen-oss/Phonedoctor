package com.phonedoctor.app.domain.model

data class AudioRouteInfo(
    val outputDevices: List<String>,
    val inputDevices: List<String>
)

data class MicrophoneLevelStats(
    val sampleCount: Int,
    val averagePercent: Int,
    val peakPercent: Int,
    val clippingSamples: Int,
    val silentSamples: Int
)
