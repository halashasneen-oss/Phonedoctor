package com.phonedoctor.app.domain.model

data class MemoryInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val isLowMemory: Boolean,
    val lowMemoryThresholdBytes: Long
)
