package com.phonedoctor.app.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentCameraTestBinding
import com.phonedoctor.app.ui.common.viewBinding

class CameraTestFragment : Fragment(R.layout.fragment_camera_test) {

    private val binding by viewBinding(FragmentCameraTestBinding::bind)
    private var cameraProvider: ProcessCameraProvider? = null
    private var usingFrontCamera = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showUnavailable(showGrantButton = true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonClose.setOnClickListener { findNavController().navigateUp() }
        binding.buttonSwitchCamera.setOnClickListener {
            usingFrontCamera = !usingFrontCamera
            startCamera()
        }
        binding.buttonGrantPermission.setOnClickListener { permissionLauncher.launch(Manifest.permission.CAMERA) }

        val hasCamera = requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (!hasCamera) {
            showUnavailable(showGrantButton = false)
            return
        }

        val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) startCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider

            val selector = if (usingFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            if (!provider.hasCamera(selector)) {
                usingFrontCamera = !usingFrontCamera
            }
            val finalSelector = if (usingFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(viewLifecycleOwner, finalSelector, preview)
                binding.containerUnavailable.visibility = View.GONE
                binding.previewView.visibility = View.VISIBLE
                binding.textActiveCamera.setText(
                    if (usingFrontCamera) R.string.camera_front else R.string.camera_rear
                )
            }.onFailure {
                showUnavailable(showGrantButton = false)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun showUnavailable(showGrantButton: Boolean) {
        binding.previewView.visibility = View.GONE
        binding.containerUnavailable.visibility = View.VISIBLE
        binding.buttonGrantPermission.visibility = if (showGrantButton) View.VISIBLE else View.GONE
        binding.buttonSwitchCamera.isEnabled = false
    }

    override fun onDestroyView() {
        cameraProvider?.unbindAll()
        super.onDestroyView()
    }
}
