package com.phonedoctor.app.data.repository

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.phonedoctor.app.domain.model.ThermalInfo
import com.phonedoctor.app.domain.model.ThermalState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThermalRepository(private val context: Context) {

    suspend fun getThermalInfo(): ThermalInfo = withContext(Dispatchers.Default) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext ThermalInfo(
                state = ThermalState.UNAVAILABLE,
                currentHeadroom = null,
                forecastHeadroom10s = null,
                severeThreshold = null
            )
        }

        val state = when (powerManager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState.NONE
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalState.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.SHUTDOWN
            else -> ThermalState.UNAVAILABLE
        }

        val currentHeadroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { powerManager.getThermalHeadroom(0) }
                .getOrNull()
                ?.takeIf { it.isFinite() }
        } else {
            null
        }

        val forecastHeadroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { powerManager.getThermalHeadroom(10) }
                .getOrNull()
                ?.takeIf { it.isFinite() }
        } else {
            null
        }

        val severeThreshold = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runCatching {
                powerManager.thermalHeadroomThresholds[PowerManager.THERMAL_STATUS_SEVERE]
            }.getOrNull()
        } else {
            null
        }

        ThermalInfo(
            state = state,
            currentHeadroom = currentHeadroom,
            forecastHeadroom10s = forecastHeadroom,
            severeThreshold = severeThreshold
        )
    }
}
