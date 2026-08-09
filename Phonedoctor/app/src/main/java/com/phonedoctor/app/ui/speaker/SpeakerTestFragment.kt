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
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class SpeakerTestFragment : Fragment(R.layout.fragment_speaker_test) {

    private val binding by viewBinding(FragmentSpeakerTestBinding::bind)
    private val tonePlayer = TonePlayer()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonPlay.setOnClickListener { play(ToneChannel.BOTH) }
        binding.buttonPlayLeft.setOnClickListener { play(ToneChannel.LEFT) }
        binding.buttonPlayRight.setOnClickListener { play(ToneChannel.RIGHT) }
    }

    private fun play(channel: ToneChannel) {
        viewLifecycleOwner.lifecycleScope.launch {
            tonePlayer.playTone(channel)
        }
    }

    override fun onDestroyView() {
        tonePlayer.stop()
        super.onDestroyView()
    }
}
