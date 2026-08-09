package com.phonedoctor.app.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Records a short local-only sample to the app cache dir. Never uploaded anywhere. */
class MicRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    var currentFile: File? = null
        private set

    fun startRecording(): File {
        val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
        val file = File(dir, "mic_test_${System.currentTimeMillis()}.m4a")

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        currentFile = file
        return file
    }

    /** 0-32767 raw amplitude from the recorder, or 0 if not recording. */
    fun getCurrentAmplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun stopRecording() {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
    }

    fun playRecording(onCompletion: () -> Unit) {
        val file = currentFile ?: return
        stopPlayback()
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { onCompletion() }
            prepare()
            start()
        }
        player = mediaPlayer
    }

    fun stopPlayback() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }

    fun deleteRecording() {
        stopPlayback()
        currentFile?.delete()
        currentFile = null
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}
