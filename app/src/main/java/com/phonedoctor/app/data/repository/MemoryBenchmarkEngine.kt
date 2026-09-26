package com.phonedoctor.app.data.repository

import com.phonedoctor.app.domain.model.MemoryBenchmarkResult
import com.phonedoctor.app.domain.util.PerformanceBenchmarkMath
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemoryBenchmarkEngine {

    suspend fun run(): MemoryBenchmarkResult = withContext(Dispatchers.Default) {
        val maxHeap = Runtime.getRuntime().maxMemory().coerceAtLeast(1L)
        val bufferBytes = min(
            MAX_COPY_BUFFER_BYTES.toLong(),
            (maxHeap / 64L).coerceAtLeast(MIN_BUFFER_BYTES.toLong())
        ).toInt()

        val source = ByteArray(bufferBytes) { index -> (index and 0x7F).toByte() }
        val destination = ByteArray(bufferBytes)

        // Warm up System.arraycopy/JIT before measuring.
        System.arraycopy(source, 0, destination, 0, bufferBytes)

        val start = System.nanoTime()
        val deadline = start + COPY_WINDOW_NANOS
        var copiedBytes = 0L
        while (System.nanoTime() < deadline) {
            System.arraycopy(source, 0, destination, 0, bufferBytes)
            copiedBytes += bufferBytes.toLong()
        }
        val elapsed = (System.nanoTime() - start).coerceAtLeast(1L)

        val allocationBytes = min(
            MAX_ALLOCATION_TEST_BYTES.toLong(),
            (maxHeap / 64L).coerceAtLeast(MIN_BUFFER_BYTES.toLong())
        ).toInt()

        val allocationSucceeded = try {
            val probe = ByteArray(allocationBytes)
            var index = 0
            while (index < probe.size) {
                probe[index] = 1
                index += PAGE_TOUCH_BYTES
            }
            probe[probe.lastIndex] = 1
            true
        } catch (_: OutOfMemoryError) {
            false
        }

        MemoryBenchmarkResult(
            bufferBytes = bufferBytes,
            copyBytesPerSecond = PerformanceBenchmarkMath.bytesPerSecond(
                copiedBytes,
                elapsed
            ),
            allocationTestBytes = allocationBytes,
            allocationSucceeded = allocationSucceeded
        )
    }

    companion object {
        private const val MIN_BUFFER_BYTES = 512 * 1024
        private const val MAX_COPY_BUFFER_BYTES = 8 * 1024 * 1024
        private const val MAX_ALLOCATION_TEST_BYTES = 8 * 1024 * 1024
        private const val PAGE_TOUCH_BYTES = 4 * 1024
        private const val COPY_WINDOW_NANOS = 500_000_000L
    }
}
