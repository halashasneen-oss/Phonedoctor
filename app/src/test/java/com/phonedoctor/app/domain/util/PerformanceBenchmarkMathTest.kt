package com.phonedoctor.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceBenchmarkMathTest {

    @Test
    fun `bytes per second normalizes elapsed nanoseconds`() {
        assertEquals(
            1000L,
            PerformanceBenchmarkMath.bytesPerSecond(
                bytes = 500L,
                durationNanos = 500_000_000L
            )
        )
    }

    @Test
    fun `invalid elapsed time returns zero`() {
        assertEquals(0L, PerformanceBenchmarkMath.bytesPerSecond(100L, 0L))
    }

    @Test
    fun `mebibytes per second uses binary megabytes`() {
        assertEquals(
            1.0,
            PerformanceBenchmarkMath.mebibytesPerSecond(1024L * 1024L),
            0.0001
        )
    }
}
