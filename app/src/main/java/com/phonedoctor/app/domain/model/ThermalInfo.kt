package com.phonedoctor.app.domain.model

enum class ThermalState {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
    UNAVAILABLE
}

data class ThermalInfo(
    val state: ThermalState,
    val currentHeadroom: Float?,
    val forecastHeadroom10s: Float?,
    val severeThreshold: Float?
)
