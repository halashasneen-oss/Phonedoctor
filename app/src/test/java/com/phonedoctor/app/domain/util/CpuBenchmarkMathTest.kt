package com.phonedoctor.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CpuBenchmarkMathTest {

    @Test
    fun `worker count is bounded for safe stress tests`() {
        assertEquals(1, CpuBenchmarkMath.workerCount(0))
        assertEquals(4, CpuBenchmarkMath.workerCount(4))
        assertEquals(8, CpuBenchmarkMath.workerCount(24))
    }

    @Test
    fun `sustained performance reports relative throughput`() {
        assertEquals(100, CpuBenchmarkMath.sustainedPerformancePercent(1_000L, 1_000L))
        assertEquals(80, CpuBenchmarkMath.sustainedPerformancePercent(1_000L, 800L))
        assertEquals(120, CpuBenchmarkMath.sustainedPerformancePercent(1_000L, 1_200L))
    }

    @Test
    fun `invalid baseline returns zero instead of fabricated performance`() {
        assertEquals(0, CpuBenchmarkMath.sustainedPerformancePercent(0L, 500L))
    }
}
