package com.phonedoctor.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
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
        val levelPercent = if (level >= 0 && scale > 0) {
            level * 100 / scale
        } else {
            null
        }

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

        val rawTemperature = batteryIntent
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        val temperature = rawTemperature
            .takeIf { it != Int.MIN_VALUE }
            ?.div(10f)

        val voltage = batteryIntent
            ?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            ?.takeIf { it > 0 }

        fun intProperty(id: Int): Long? {
            val value = batteryManager?.getIntProperty(id) ?: return null
            return value.takeIf { it != Int.MIN_VALUE }?.toLong()
        }

        val currentMicroAmps = intProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentAverageMicroAmps =
            intProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val chargeCounterMicroAh =
            intProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val energyCounterNanoWh = batteryManager
            ?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            ?.takeIf { it != Long.MIN_VALUE }

        val cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            batteryIntent
                ?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
                ?.takeIf { it >= 0 }
        } else {
            null
        }

        val chargeTimeRemainingMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            batteryManager
                ?.computeChargeTimeRemaining()
                ?.takeIf { it >= 0L }
        } else {
            null
        }

        val batteryLow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            batteryIntent?.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)
        } else {
            null
        }

        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

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
            currentAverageMicroAmps = currentAverageMicroAmps,
            chargeCounterMicroAh = chargeCounterMicroAh,
            energyCounterNanoWh = energyCounterNanoWh,
            cycleCount = cycleCount,
            chargeTimeRemainingMillis = chargeTimeRemainingMillis,
            batteryLow = batteryLow,
            technology = technology,
            healthDescription = healthDescription
        )
    }
}
