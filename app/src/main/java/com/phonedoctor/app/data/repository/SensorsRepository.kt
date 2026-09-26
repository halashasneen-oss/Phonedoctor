package com.phonedoctor.app.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.phonedoctor.app.domain.model.SensorEntry
import com.phonedoctor.app.domain.model.SensorKind
import com.phonedoctor.app.domain.model.SensorReading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorsRepository(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private fun androidTypeFor(kind: SensorKind): Int = when (kind) {
        SensorKind.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
        SensorKind.GYROSCOPE -> Sensor.TYPE_GYROSCOPE
        SensorKind.MAGNETOMETER -> Sensor.TYPE_MAGNETIC_FIELD
        SensorKind.PROXIMITY -> Sensor.TYPE_PROXIMITY
        SensorKind.LIGHT -> Sensor.TYPE_LIGHT
        SensorKind.PRESSURE -> Sensor.TYPE_PRESSURE
        SensorKind.ROTATION_VECTOR -> Sensor.TYPE_ROTATION_VECTOR
        SensorKind.STEP_COUNTER -> Sensor.TYPE_STEP_COUNTER
    }

    fun getAllSensors(): List<SensorEntry> = SensorKind.entries.map { kind ->
        val sensor = sensorManager.getDefaultSensor(androidTypeFor(kind))
        SensorEntry(
            kind = kind,
            available = sensor != null,
            displayName = sensor?.name,
            vendor = sensor?.vendor,
            version = sensor?.version,
            resolution = sensor?.resolution,
            maximumRange = sensor?.maximumRange,
            powerMilliAmps = sensor?.power,
            minDelayMicroseconds = sensor?.minDelay,
            fifoReservedEventCount = sensor?.fifoReservedEventCount,
            fifoMaxEventCount = sensor?.fifoMaxEventCount,
            reportingMode = sensor?.reportingMode,
            wakeUpSensor = sensor?.isWakeUpSensor
        )
    }

    fun observeReadings(kind: SensorKind): Flow<SensorReading> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(androidTypeFor(kind))
        if (sensor == null) {
            close()
            return@callbackFlow
        }

        var currentAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(
                    SensorReading(
                        values = event.values.copyOf(),
                        accuracy = currentAccuracy,
                        timestampNanos = event.timestamp
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                currentAccuracy = accuracy
            }
        }

        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_UI
        )
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
