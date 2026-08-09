package com.phonedoctor.app.ui.sensors

import androidx.annotation.StringRes
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.SensorKind

fun SensorKind.labelRes(): Int = when (this) {
    SensorKind.ACCELEROMETER -> R.string.sensor_accelerometer
    SensorKind.GYROSCOPE -> R.string.sensor_gyroscope
    SensorKind.MAGNETOMETER -> R.string.sensor_magnetometer
    SensorKind.PROXIMITY -> R.string.sensor_proximity
    SensorKind.LIGHT -> R.string.sensor_light
    SensorKind.PRESSURE -> R.string.sensor_pressure
    SensorKind.ROTATION_VECTOR -> R.string.sensor_rotation_vector
    SensorKind.STEP_COUNTER -> R.string.sensor_step_counter
}
