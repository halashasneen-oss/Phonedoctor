package com.phonedoctor.app.domain.model

data class MemoryBenchmarkResult(
    val bufferBytes: Int,
    val copyBytesPerSecond: Long,
    val allocationTestBytes: Int,
    val allocationSucceeded: Boolean
)

data class StorageBenchmarkResult(
    val testBytes: Long,
    val writeBytesPerSecond: Long,
    val readBytesPerSecond: Long,
    val writeDurationMillis: Long,
    val readDurationMillis: Long
)
