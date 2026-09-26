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
    val vendor: String? = null,
    val version: Int? = null,
    val resolution: Float? = null,
    val maximumRange: Float? = null,
    val powerMilliAmps: Float? = null,
    val minDelayMicroseconds: Int? = null,
    val fifoReservedEventCount: Int? = null,
    val fifoMaxEventCount: Int? = null,
    val reportingMode: Int? = null,
    val wakeUpSensor: Boolean? = null
)

data class SensorReading(
    val values: FloatArray,
    val accuracy: Int,
    val timestampNanos: Long
)
