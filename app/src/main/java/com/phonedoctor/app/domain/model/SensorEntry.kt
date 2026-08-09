package com.phonedoctor.app.domain.model

enum class SensorKind {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    PROXIMITY,
    LIGHT,
    PRESSURE,
    ROTATION_VECTOR,
    STEP_COUNTER
}

data class SensorEntry(
    val kind: SensorKind,
    val available: Boolean,
    val displayName: String? = null,
    val vendor: String? = null
)
