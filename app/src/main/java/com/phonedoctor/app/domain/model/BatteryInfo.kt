package com.phonedoctor.app.domain.model

enum class ChargingState { CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN }
enum class ChargePlug { AC, USB, WIRELESS, NONE, UNKNOWN }

data class BatteryInfo(
    val levelPercent: Int?,
    val chargingState: ChargingState,
    val chargePlug: ChargePlug,
    val temperatureCelsius: Float?,
    val voltageMillivolts: Int?,
    val currentMicroAmps: Long?,
    val technology: String?,
    val healthDescription: String?
)
