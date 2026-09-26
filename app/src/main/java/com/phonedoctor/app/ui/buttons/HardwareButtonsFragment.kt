package com.phonedoctor.app.ui.buttons

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentHardwareButtonsBinding
import com.phonedoctor.app.ui.common.viewBinding

class HardwareButtonsFragment : Fragment(R.layout.fragment_hardware_buttons) {

    private val binding by viewBinding(FragmentHardwareButtonsBinding::bind)

    private var volumeUpDetected = false
    private var volumeDownDetected = false
    private var cameraDetected = false
    private var focusDetected = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonReset.setOnClickListener {
            volumeUpDetected = false
            volumeDownDetected = false
            cameraDetected = false
            focusDetected = false
            render()
            binding.root.requestFocus()
        }

        binding.root.isFocusableInTouchMode = true
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener isTestKey(keyCode)
            }

            val handled = when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    volumeUpDetected = true
                    true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    volumeDownDetected = true
                    true
                }
                KeyEvent.KEYCODE_CAMERA -> {
                    cameraDetected = true
                    true
                }
                KeyEvent.KEYCODE_FOCUS -> {
                    focusDetected = true
                    true
                }
                else -> false
            }

            if (handled) render()
            handled
        }

        render()
        binding.root.post { binding.root.requestFocus() }
    }

    override fun onResume() {
        super.onResume()
        binding.root.requestFocus()
    }

    private fun isTestKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_CAMERA,
        KeyEvent.KEYCODE_FOCUS -> true
        else -> false
    }

    private fun render() {
        renderStatus(binding.textVolumeUpStatus, volumeUpDetected)
        renderStatus(binding.textVolumeDownStatus, volumeDownDetected)
        renderStatus(binding.textCameraStatus, cameraDetected)
        renderStatus(binding.textFocusStatus, focusDetected)

        val detectedCount = listOf(
            volumeUpDetected,
            volumeDownDetected,
            cameraDetected,
            focusDetected
        ).count { it }

        binding.textButtonsSummary.text = getString(
            R.string.buttons_detected_fmt,
            detectedCount,
            4
        )
    }

    private fun renderStatus(textView: android.widget.TextView, detected: Boolean) {
        textView.setText(
            if (detected) {
                R.string.buttons_detected
            } else {
                R.string.buttons_waiting
            }
        )
        textView.setTextColor(
            requireContext().getColor(
                if (detected) {
                    R.color.color_success
                } else {
                    R.color.color_text_secondary
                }
            )
        )
    }
}
