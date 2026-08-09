package com.phonedoctor.app.ui.microphone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.phonedoctor.app.R
import com.phonedoctor.app.data.audio.MicRecorder
import com.phonedoctor.app.databinding.FragmentMicrophoneTestBinding
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MicrophoneTestFragment : Fragment(R.layout.fragment_microphone_test) {

    private val binding by viewBinding(FragmentMicrophoneTestBinding::bind)
    private lateinit var recorder: MicRecorder
    private var isRecording = false
    private var levelJob: Job? = null

    private val maxRecordingMs = 10_000L

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginRecording() else Snackbar.make(binding.root, R.string.common_permission_required, Snackbar.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recorder = MicRecorder(requireContext().applicationContext)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonRecord.setOnClickListener { onRecordClicked() }
        binding.buttonPlay.setOnClickListener { onPlayClicked() }
        binding.buttonDelete.setOnClickListener { onDeleteClicked() }
    }

    private fun onRecordClicked() {
        if (isRecording) {
            stopRecording()
            return
        }
        val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) beginRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun beginRecording() {
        runCatching { recorder.startRecording() }.onFailure {
            Snackbar.make(binding.root, R.string.common_not_available, Snackbar.LENGTH_SHORT).show()
            return
        }
        isRecording = true
        binding.buttonRecord.setText(R.string.mic_stop_recording)
        binding.buttonPlay.isEnabled = false
        binding.buttonDelete.isEnabled = false

        levelJob = viewLifecycleOwner.lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            while (isRecording && System.currentTimeMillis() - startTime < maxRecordingMs) {
                val amplitude = recorder.getCurrentAmplitude()
                binding.levelMeter.setProgressCompat((amplitude * 100 / 32767).coerceIn(0, 100), true)
                delay(120)
            }
            if (isRecording) stopRecording()
        }
    }

    private fun stopRecording() {
        isRecording = false
        levelJob?.cancel()
        recorder.stopRecording()
        binding.buttonRecord.setText(R.string.mic_start_recording)
        binding.levelMeter.setProgressCompat(0, true)
        val hasRecording = recorder.currentFile != null
        binding.buttonPlay.isEnabled = hasRecording
        binding.buttonDelete.isEnabled = hasRecording
    }

    private fun onPlayClicked() {
        recorder.playRecording {}
    }

    private fun onDeleteClicked() {
        recorder.deleteRecording()
        binding.buttonPlay.isEnabled = false
        binding.buttonDelete.isEnabled = false
        Snackbar.make(binding.root, R.string.mic_recording_deleted, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        levelJob?.cancel()
        recorder.deleteRecording()
        recorder.release()
        super.onDestroyView()
    }
}
