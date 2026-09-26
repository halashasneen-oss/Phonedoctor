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
            val manager = requireContext()
                .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
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
            binding.textAmplitudeSupport.setText(R.string.common_not_supported)
            binding.textVibrationCapabilities.setText(R.string.common_not_supported)
            binding.buttonShort.isEnabled = false
            binding.buttonLong.isEnabled = false
            binding.buttonPattern.isEnabled = false
            binding.buttonClickEffect.isEnabled = false
            return
        }

        val amplitudeSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.hasAmplitudeControl()
        } else {
            false
        }
        binding.textAmplitudeSupport.text = getString(
            R.string.vibration_amplitude_fmt,
            getString(
                if (amplitudeSupported) {
                    R.string.sensor_available
                } else {
                    R.string.sensor_not_available
                }
            )
        )
        binding.textVibrationCapabilities.text = buildCapabilities()

        binding.buttonShort.setOnClickListener { vibrateOneShot(80) }
        binding.buttonLong.setOnClickListener { vibrateOneShot(800) }
        binding.buttonPattern.setOnClickListener { vibratePattern() }
        binding.buttonClickEffect.setOnClickListener { vibrateClickEffect() }
    }

    private fun buildCapabilities(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return getString(R.string.vibration_capabilities_legacy)
        }

        val effectLabels = listOf("Tick", "Click", "Heavy click", "Double click")
        val effectSupport = vibrator.areEffectsSupported(
            VibrationEffect.EFFECT_TICK,
            VibrationEffect.EFFECT_CLICK,
            VibrationEffect.EFFECT_HEAVY_CLICK,
            VibrationEffect.EFFECT_DOUBLE_CLICK
        )
        val effectsText = effectLabels.indices.joinToString(", ") { index ->
            val status = when (effectSupport[index]) {
                Vibrator.VIBRATION_EFFECT_SUPPORT_YES -> "✓"
                Vibrator.VIBRATION_EFFECT_SUPPORT_NO -> "✕"
                else -> "?"
            }
            "${effectLabels[index]} $status"
        }

        val primitiveLabels = listOf("Click", "Thud", "Tick")
        val primitiveSupport = vibrator.arePrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_THUD,
            VibrationEffect.Composition.PRIMITIVE_TICK
        )
        val durations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator.getPrimitiveDurations(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_TICK
            )
        } else {
            IntArray(primitiveLabels.size)
        }

        val primitivesText = primitiveLabels.indices.joinToString(", ") { index ->
            if (!primitiveSupport[index]) {
                "${primitiveLabels[index]} ✕"
            } else {
                val duration = durations.getOrNull(index)?.takeIf { it > 0 }
                if (duration != null) {
                    "${primitiveLabels[index]} ✓ (${duration}ms)"
                } else {
                    "${primitiveLabels[index]} ✓"
                }
            }
        }

        val idText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator.id.toString()
        } else {
            getString(R.string.common_not_available)
        }

        return getString(
            R.string.vibration_capabilities_fmt,
            idText,
            effectsText,
            primitivesText
        )
    }

    private fun vibrateOneShot(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
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

    private fun vibrateClickEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(
                    VibrationEffect.EFFECT_CLICK
                )
            )
        } else {
            vibrateOneShot(50)
        }
    }

    override fun onDestroyView() {
        vibrator.cancel()
        super.onDestroyView()
    }
}
