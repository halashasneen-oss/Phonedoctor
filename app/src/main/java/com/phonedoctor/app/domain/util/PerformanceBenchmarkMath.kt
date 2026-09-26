package com.phonedoctor.app.domain.util

object PerformanceBenchmarkMath {

    fun bytesPerSecond(bytes: Long, durationNanos: Long): Long {
        if (bytes <= 0L || durationNanos <= 0L) return 0L
        return ((bytes.toDouble() * 1_000_000_000.0) / durationNanos.toDouble())
            .toLong()
            .coerceAtLeast(0L)
    }

    fun mebibytesPerSecond(bytesPerSecond: Long): Double =
        bytesPerSecond.toDouble() / (1024.0 * 1024.0)
}
