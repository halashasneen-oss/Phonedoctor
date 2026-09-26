package com.phonedoctor.app.domain.util

object CpuBenchmarkMath {

    fun workerCount(coreCount: Int): Int = coreCount.coerceIn(1, 8)

    fun sustainedPerformancePercent(
        firstOpsPerSecond: Long,
        lastOpsPerSecond: Long
    ): Int {
        if (firstOpsPerSecond <= 0L || lastOpsPerSecond < 0L) return 0
        return ((lastOpsPerSecond.toDouble() / firstOpsPerSecond.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 200)
    }
}
