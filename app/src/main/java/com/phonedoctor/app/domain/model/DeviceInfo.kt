package com.phonedoctor.app.domain.model

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val androidVersion: String,
    val sdkInt: Int,
    val cpuAbi: String,
    val supportedAbis: List<String>,
    val cpuCoreCount: Int,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val screenDensityDpi: Int,
    val refreshRateHz: Float?,
    val totalRamBytes: Long,
    val board: String,
    val hardware: String
)
