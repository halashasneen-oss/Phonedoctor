package com.phonedoctor.app.data.repository

import android.content.Context
import android.os.StatFs
import com.phonedoctor.app.domain.model.StorageBenchmarkResult
import com.phonedoctor.app.domain.util.PerformanceBenchmarkMath
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageBenchmarkEngine(private val context: Context) {

    suspend fun run(): StorageBenchmarkResult = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val availableBytes = StatFs(cacheDir.absolutePath).availableBytes
        val testBytes = chooseTestSize(availableBytes)
        require(testBytes >= MIN_TEST_BYTES) {
            "Not enough free app cache space for a safe storage test"
        }

        val tempFile = File.createTempFile("phone_doctor_storage_", ".bin", cacheDir)
        val buffer = ByteArray(CHUNK_BYTES) { index -> (index and 0xFF).toByte() }

        try {
            val writeStart = System.nanoTime()
            FileOutputStream(tempFile).use { output ->
                var remaining = testBytes
                while (remaining > 0L) {
                    val count = min(buffer.size.toLong(), remaining).toInt()
                    output.write(buffer, 0, count)
                    remaining -= count.toLong()
                }
                output.flush()
                output.fd.sync()
            }
            val writeElapsed = (System.nanoTime() - writeStart).coerceAtLeast(1L)

            val readBuffer = ByteArray(CHUNK_BYTES)
            var totalRead = 0L
            val readStart = System.nanoTime()
            FileInputStream(tempFile).use { input ->
                while (true) {
                    val read = input.read(readBuffer)
                    if (read <= 0) break
                    totalRead += read.toLong()
                }
            }
            val readElapsed = (System.nanoTime() - readStart).coerceAtLeast(1L)

            StorageBenchmarkResult(
                testBytes = testBytes,
                writeBytesPerSecond = PerformanceBenchmarkMath.bytesPerSecond(
                    testBytes,
                    writeElapsed
                ),
                readBytesPerSecond = PerformanceBenchmarkMath.bytesPerSecond(
                    totalRead,
                    readElapsed
                ),
                writeDurationMillis = writeElapsed / 1_000_000L,
                readDurationMillis = readElapsed / 1_000_000L
            )
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private fun chooseTestSize(availableBytes: Long): Long = when {
        availableBytes >= 128L * MIB -> 16L * MIB
        availableBytes >= 32L * MIB -> 4L * MIB
        else -> (availableBytes / 8L).coerceAtMost(1L * MIB)
    }

    companion object {
        private const val MIB = 1024 * 1024
        private const val CHUNK_BYTES = 256 * 1024
        private const val MIN_TEST_BYTES = 256L * 1024L
    }
}
