package com.phonedoctor.app.ui.speaker

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.data.audio.ToneChannel
import com.phonedoctor.app.data.audio.TonePlayer
import com.phonedoctor.app.databinding.FragmentSpeakerTestBinding
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class SpeakerTestFragment : Fragment(R.layout.fragment_speaker_test) {

    private val binding by viewBinding(FragmentSpeakerTestBinding::bind)
    private val tonePlayer = TonePlayer()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonPlay.setOnClickListener { playTone(ToneChannel.BOTH, 1_000.0) }
        binding.buttonPlayLeft.setOnClickListener { playTone(ToneChannel.LEFT, 1_000.0) }
        binding.buttonPlayRight.setOnClickListener { playTone(ToneChannel.RIGHT, 1_000.0) }
        binding.buttonToneLow.setOnClickListener { playTone(ToneChannel.BOTH, 250.0) }
        binding.buttonToneMid.setOnClickListener { playTone(ToneChannel.BOTH, 1_000.0) }
        binding.buttonToneHigh.setOnClickListener { playTone(ToneChannel.BOTH, 4_000.0) }
        binding.buttonSweep.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                tonePlayer.playSweep()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val routes = serviceLocator().audioDiagnosticsRepository.getRoutes()
            binding.textOutputRoutes.text = if (routes.outputDevices.isEmpty()) {
                getString(R.string.common_not_available)
            } else {
                routes.outputDevices.joinToString("\n")
            }
        }
    }

    private fun playTone(channel: ToneChannel, frequencyHz: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            tonePlayer.playTone(channel, frequencyHz)
        }
    }

    override fun onDestroyView() {
        tonePlayer.stop()
        super.onDestroyView()
    }
}
