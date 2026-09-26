package com.phonedoctor.app.data.repository

import com.phonedoctor.app.domain.model.NetworkProbeResult
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkDiagnosticsEngine {

    suspend fun runProbe(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS
    ): NetworkProbeResult = withContext(Dispatchers.IO) {
        val safeHost = host.trim().ifBlank { DEFAULT_HOST }
        try {
            val dnsStart = System.nanoTime()
            val addresses = InetAddress.getAllByName(safeHost)
            val dnsElapsed = elapsedMillis(dnsStart)

            if (addresses.isEmpty()) {
                return@withContext NetworkProbeResult(
                    host = safeHost,
                    dnsLatencyMillis = dnsElapsed,
                    tcpLatencyMillis = null,
                    resolvedAddressCount = 0,
                    success = false,
                    errorMessage = "No DNS addresses returned"
                )
            }

            val socket = Socket()
            val tcpStart = System.nanoTime()
            try {
                socket.connect(
                    InetSocketAddress(addresses.first(), port),
                    timeoutMillis.coerceIn(500, 5_000)
                )
            } finally {
                runCatching { socket.close() }
            }
            val tcpElapsed = elapsedMillis(tcpStart)

            NetworkProbeResult(
                host = safeHost,
                dnsLatencyMillis = dnsElapsed,
                tcpLatencyMillis = tcpElapsed,
                resolvedAddressCount = addresses.size,
                success = true
            )
        } catch (t: Throwable) {
            NetworkProbeResult(
                host = safeHost,
                dnsLatencyMillis = null,
                tcpLatencyMillis = null,
                resolvedAddressCount = 0,
                success = false,
                errorMessage = t.message ?: t.javaClass.simpleName
            )
        }
    }

    private fun elapsedMillis(startNanos: Long): Long =
        ((System.nanoTime() - startNanos) / 1_000_000L).coerceAtLeast(0L)

    companion object {
        const val DEFAULT_HOST = "example.com"
        const val DEFAULT_PORT = 443
        const val DEFAULT_TIMEOUT_MILLIS = 1_500
    }
}
