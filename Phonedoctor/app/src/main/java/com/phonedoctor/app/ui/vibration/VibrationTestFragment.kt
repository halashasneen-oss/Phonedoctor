package com.phonedoctor.app.ui.vibration

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentVibrationTestBinding
import com.phonedoctor.app.ui.common.viewBinding

class VibrationTestFragment : Fragment(R.layout.fragment_vibration_test) {

    private val binding by viewBinding(FragmentVibrationTestBinding::bind)

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        if (!vibrator.hasVibrator()) {
            binding.textAmplitudeSupport.text = getString(R.string.common_not_supported)
            binding.buttonShort.isEnabled = false
            binding.buttonLong.isEnabled = false
            binding.buttonPattern.isEnabled = false
            return
        }

        val amplitudeSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.hasAmplitudeControl()
        } else {
            false
        }
        binding.textAmplitudeSupport.text =
            "${getString(R.string.vibration_has_amplitude_control)}: " +
            getString(if (amplitudeSupported) R.string.sensor_available else R.string.sensor_not_available)

        binding.buttonShort.setOnClickListener { vibrateOneShot(80) }
        binding.buttonLong.setOnClickListener { vibrateOneShot(800) }
        binding.buttonPattern.setOnClickListener { vibratePattern() }
    }

    private fun vibrateOneShot(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun vibratePattern() {
        val timings = longArrayOf(0, 100, 100, 100, 100, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
}
