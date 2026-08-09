package com.phonedoctor.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.phonedoctor.app.domain.model.BatteryInfo
import com.phonedoctor.app.domain.model.ChargePlug
import com.phonedoctor.app.domain.model.ChargingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BatteryRepository(private val context: Context) {

    suspend fun getBatteryInfo(): BatteryInfo = withContext(Dispatchers.IO) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, filter)
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val levelPercent = if (level >= 0 && scale > 0) (level * 100 / scale) else null

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val chargingState = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingState.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> ChargingState.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingState.NOT_CHARGING
            else -> ChargingState.UNKNOWN
        }

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargePlug = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargePlug.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargePlug.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargePlug.WIRELESS
            0 -> ChargePlug.NONE
            else -> ChargePlug.UNKNOWN
        }

        val tenthsOfCelsius = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        val temperature = if (tenthsOfCelsius != Int.MIN_VALUE) tenthsOfCelsius / 10f else null

        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }

        val currentMicroAmps = batteryManager
            ?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.takeIf { it != Long.MIN_VALUE && it != 0L }

        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

        // Android exposes no public API for a manufacturer-verified "battery health"
        // percentage; only a coarse EXTRA_HEALTH enum on some OEM builds.
        val healthExtra = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthDescription = when (healthExtra) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
            else -> null
        }

        BatteryInfo(
            levelPercent = levelPercent,
            chargingState = chargingState,
            chargePlug = chargePlug,
            temperatureCelsius = temperature,
            voltageMillivolts = voltage,
            currentMicroAmps = currentMicroAmps,
            technology = technology,
            healthDescription = healthDescription
        )
    }
}
