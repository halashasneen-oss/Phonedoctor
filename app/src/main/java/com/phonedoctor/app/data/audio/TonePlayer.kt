package com.phonedoctor.app.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

enum class ToneChannel { BOTH, LEFT, RIGHT }

class TonePlayer {

    private var track: AudioTrack? = null

    suspend fun playTone(
        channel: ToneChannel,
        frequencyHz: Double = 440.0,
        durationMs: Int = 1200
    ) = withContext(Dispatchers.Default) {
        playGenerated(
            channel = channel,
            durationMs = durationMs
        ) { _, _ -> frequencyHz }
    }

    suspend fun playSweep(
        channel: ToneChannel = ToneChannel.BOTH,
        startHz: Double = 200.0,
        endHz: Double = 4_000.0,
        durationMs: Int = 2_500
    ) = withContext(Dispatchers.Default) {
        playGenerated(
            channel = channel,
            durationMs = durationMs
        ) { index, sampleCount ->
            val fraction = index.toDouble() / sampleCount.coerceAtLeast(1).toDouble()
            startHz + (endHz - startHz) * fraction
        }
    }

    private fun playGenerated(
        channel: ToneChannel,
        durationMs: Int,
        frequencyProvider: (index: Int, sampleCount: Int) -> Double
    ) {
        stop()

        val sampleRate = 44_100
        val sampleCount = sampleRate * durationMs.coerceIn(250, 5_000) / 1_000
        val amplitude = 0.35
        val buffer = ShortArray(sampleCount * 2)
        val fadeSamples = (sampleCount / 20).coerceAtLeast(1)
        var phase = 0.0

        for (i in 0 until sampleCount) {
            val frequencyHz = frequencyProvider(i, sampleCount).coerceIn(80.0, 8_000.0)
            phase += 2.0 * PI * frequencyHz / sampleRate.toDouble()

            val envelope = when {
                i < fadeSamples -> i.toDouble() / fadeSamples.toDouble()
                i > sampleCount - fadeSamples ->
                    (sampleCount - i).toDouble() / fadeSamples.toDouble()
                else -> 1.0
            }

            val sample = (
                sin(phase) * amplitude * envelope * Short.MAX_VALUE
                ).toInt().toShort()

            val leftSample = if (channel == ToneChannel.RIGHT) 0 else sample
            val rightSample = if (channel == ToneChannel.LEFT) 0 else sample
            buffer[i * 2] = leftSample
            buffer[i * 2 + 1] = rightSample
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(buffer.size * 2)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        track = audioTrack
        audioTrack.play()
    }

    fun stop() {
        track?.let {
            runCatching {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop()
                it.release()
            }
        }
        track = null
    }
}
