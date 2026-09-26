package com.phonedoctor.app.data.repository

import com.phonedoctor.app.domain.model.CpuBenchmarkResult
import com.phonedoctor.app.domain.util.CpuBenchmarkMath
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class CpuBenchmarkEngine {

    suspend fun runBenchmark(coreCount: Int): CpuBenchmarkResult = withContext(Dispatchers.Default) {
        // Short warm-up to reduce cold-start/JIT bias.
        runSingleWindow(250L)

        val single = runSingleWindow(750L)
        val workers = CpuBenchmarkMath.workerCount(coreCount)
        val multi = runMultiWindow(1_000L, workers)

        CpuBenchmarkResult(
            singleThreadOpsPerSecond = normalize(single, 750L),
            multiThreadOpsPerSecond = normalize(multi, 1_000L),
            workerCount = workers
        )
    }

    suspend fun runStress(
        coreCount: Int,
        durationSeconds: Int = DEFAULT_STRESS_SECONDS,
        onProgress: suspend (Int) -> Unit = {}
    ): List<Long> = withContext(Dispatchers.Default) {
        val safeDuration = durationSeconds.coerceIn(3, MAX_STRESS_SECONDS)
        val workers = CpuBenchmarkMath.workerCount(coreCount)
        val samples = ArrayList<Long>(safeDuration)

        repeat(safeDuration) { second ->
            val ops = runMultiWindow(1_000L, workers)
            samples += ops
            onProgress(((second + 1) * 100) / safeDuration)
        }

        samples
    }

    private suspend fun runMultiWindow(durationMillis: Long, workers: Int): Long = coroutineScope {
        List(workers) {
            async(Dispatchers.Default) { runSingleWindow(durationMillis) }
        }.awaitAll().sum()
    }

    private fun runSingleWindow(durationMillis: Long): Long {
        val deadline = System.nanoTime() + durationMillis * 1_000_000L
        var operations = 0L
        var state = 0x9E3779B97F4A7C15UL.toLong()

        while (System.nanoTime() < deadline) {
            repeat(WORK_BATCH) {
                state = state xor (state shl 13)
                state = state xor (state ushr 7)
                state = state xor (state shl 17)
                state *= -7046029254386353131L
            }
            operations += WORK_BATCH
        }

        blackHole = blackHole xor state
        return operations
    }

    private fun normalize(operations: Long, durationMillis: Long): Long {
        return max(0L, operations * 1_000L / durationMillis)
    }

    companion object {
        const val DEFAULT_STRESS_SECONDS = 10
        const val MAX_STRESS_SECONDS = 15
        private const val WORK_BATCH = 256L

        @Volatile
        private var blackHole: Long = 0L
    }
}
