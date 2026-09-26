package com.phonedoctor.app.domain.model

data class CpuCoreFrequency(
    val coreIndex: Int,
    val currentKHz: Long?,
    val maxKHz: Long?
)

data class CpuInfo(
    val coreCount: Int,
    val architecture: String,
    val supportedAbis: List<String>,
    val socManufacturer: String?,
    val socModel: String?,
    val board: String,
    val hardware: String,
    val frequencies: List<CpuCoreFrequency>
)

data class CpuBenchmarkResult(
    val singleThreadOpsPerSecond: Long,
    val multiThreadOpsPerSecond: Long,
    val workerCount: Int
)

data class CpuStressResult(
    val durationSeconds: Int,
    val firstWindowOpsPerSecond: Long,
    val lastWindowOpsPerSecond: Long,
    val sustainedPerformancePercent: Int,
    val initialThermalState: ThermalState,
    val finalThermalState: ThermalState,
    val initialThermalHeadroom: Float?,
    val finalThermalHeadroom: Float?
)
