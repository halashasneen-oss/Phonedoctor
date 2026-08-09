package com.phonedoctor.app.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

enum class ToneChannel { BOTH, LEFT, RIGHT }

/**
 * Generates and plays a short, comfortable-volume sine wave test tone through
 * AudioTrack. Real DSP-generated audio, not a bundled asset, so it works
 * without any audio permission or extra APK weight.
 */
class TonePlayer {

    private var track: AudioTrack? = null

    suspend fun playTone(channel: ToneChannel, frequencyHz: Double = 440.0, durationMs: Int = 1200) =
        withContext(Dispatchers.Default) {
            stop()
            val sampleRate = 44100
            val sampleCount = sampleRate * durationMs / 1000
            val amplitude = 0.35 // comfortable, non-startling volume
            val buffer = ShortArray(sampleCount * 2) // stereo interleaved

            for (i in 0 until sampleCount) {
                val angle = 2.0 * PI * i / (sampleRate / frequencyHz)
                // Fade in/out 5% to avoid clicks
                val fadeSamples = sampleCount / 20
                val envelope = when {
                    i < fadeSamples -> i.toDouble() / fadeSamples
                    i > sampleCount - fadeSamples -> (sampleCount - i).toDouble() / fadeSamples
                    else -> 1.0
                }
                val sample = (sin(angle) * amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
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
