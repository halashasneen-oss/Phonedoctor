package com.phonedoctor.app.ui.flashlight

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentFlashlightTestBinding
import com.phonedoctor.app.ui.common.viewBinding

class FlashlightTestFragment : Fragment(R.layout.fragment_flashlight_test) {

    private val binding by viewBinding(FragmentFlashlightTestBinding::bind)
    private var isOn = false
    private var cameraId: String? = null

    private val cameraManager: CameraManager by lazy {
        requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        cameraId = findTorchCameraId()
        if (cameraId == null) {
            binding.buttonToggle.isEnabled = false
            binding.textInstructions.text = getString(R.string.common_not_available)
            return
        }

        binding.buttonToggle.setOnClickListener { toggleTorch() }
    }

    private fun findTorchCameraId(): String? {
        return runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private fun toggleTorch() {
        val id = cameraId ?: return
        val newState = !isOn
        val success = runCatching { cameraManager.setTorchMode(id, newState) }.isSuccess
        if (success) {
            isOn = newState
            binding.buttonToggle.setText(if (isOn) R.string.flashlight_turn_off else R.string.flashlight_turn_on)
            binding.imageFlashlight.alpha = if (isOn) 1f else 0.5f
        } else {
            Snackbar.make(binding.root, R.string.common_not_available, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        if (isOn) {
            cameraId?.let { runCatching { cameraManager.setTorchMode(it, false) } }
        }
        super.onDestroyView()
    }
}
