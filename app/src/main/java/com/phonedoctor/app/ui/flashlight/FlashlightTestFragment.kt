package com.phonedoctor.app.ui.flashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentFlashlightTestBinding
import com.phonedoctor.app.ui.common.viewBinding
import kotlin.math.roundToInt

class FlashlightTestFragment : Fragment(R.layout.fragment_flashlight_test) {

    private val binding by viewBinding(FragmentFlashlightTestBinding::bind)
    private var isOn = false
    private var cameraId: String? = null
    private var maxStrengthLevel = 1
    private var defaultStrengthLevel = 1

    private val cameraManager: CameraManager by lazy {
        requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        cameraId = findTorchCameraId()
        val id = cameraId
        if (id == null) {
            binding.buttonToggle.isEnabled = false
            binding.textInstructions.setText(R.string.common_not_available)
            binding.textTorchCapabilities.setText(R.string.common_not_supported)
            binding.sliderStrength.isVisible = false
            binding.textStrengthValue.isVisible = false
            return
        }

        readTorchCapabilities(id)
        binding.buttonToggle.setOnClickListener { toggleTorch() }
        binding.sliderStrength.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val level = value.roundToInt().coerceIn(1, maxStrengthLevel)
            binding.textStrengthValue.text = getString(
                R.string.flashlight_strength_value_fmt,
                level,
                maxStrengthLevel
            )
            if (isOn) applyTorch(true, level)
        }
    }

    private fun findTorchCameraId(): String? {
        return runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private fun readTorchCapabilities(id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            maxStrengthLevel = characteristics[
                CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL
            ] ?: 1
            defaultStrengthLevel = characteristics[
                CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL
            ] ?: 1
        }

        maxStrengthLevel = maxStrengthLevel.coerceAtLeast(1)
        defaultStrengthLevel = defaultStrengthLevel.coerceIn(1, maxStrengthLevel)

        val supportsStrength = maxStrengthLevel > 1 &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        binding.sliderStrength.isVisible = supportsStrength
        binding.textStrengthValue.isVisible = supportsStrength

        if (supportsStrength) {
            binding.sliderStrength.valueFrom = 1f
            binding.sliderStrength.valueTo = maxStrengthLevel.toFloat()
            binding.sliderStrength.stepSize = 1f
            binding.sliderStrength.value = defaultStrengthLevel.toFloat()
            binding.textStrengthValue.text = getString(
                R.string.flashlight_strength_value_fmt,
                defaultStrengthLevel,
                maxStrengthLevel
            )
            binding.textTorchCapabilities.text = getString(
                R.string.flashlight_strength_supported_fmt,
                defaultStrengthLevel,
                maxStrengthLevel
            )
        } else {
            binding.textTorchCapabilities.setText(
                R.string.flashlight_strength_not_supported
            )
        }
    }

    private fun toggleTorch() {
        val newState = !isOn
        val level = binding.sliderStrength.value
            .roundToInt()
            .coerceIn(1, maxStrengthLevel)
        if (applyTorch(newState, level)) {
            isOn = newState
            binding.buttonToggle.setText(
                if (isOn) {
                    R.string.flashlight_turn_off
                } else {
                    R.string.flashlight_turn_on
                }
            )
            binding.imageFlashlight.alpha = if (isOn) 1f else 0.5f
        }
    }

    private fun applyTorch(enabled: Boolean, strengthLevel: Int): Boolean {
        val id = cameraId ?: return false
        val success = runCatching {
            if (
                enabled &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                maxStrengthLevel > 1
            ) {
                cameraManager.turnOnTorchWithStrengthLevel(
                    id,
                    strengthLevel.coerceIn(1, maxStrengthLevel)
                )
            } else {
                cameraManager.setTorchMode(id, enabled)
            }
        }.isSuccess

        if (!success && isAdded) {
            Snackbar.make(
                binding.root,
                R.string.common_not_available,
                Snackbar.LENGTH_SHORT
            ).show()
        }
        return success
    }

    override fun onDestroyView() {
        if (isOn) {
            cameraId?.let { id ->
                runCatching { cameraManager.setTorchMode(id, false) }
            }
        }
        super.onDestroyView()
    }
}
