package com.screenlens.app.ui.liveocr

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentLiveOcrBinding
import com.screenlens.app.domain.HistoryType
import com.screenlens.app.ocr.OcrOutcome
import kotlinx.coroutines.launch

class LiveOcrFragment : Fragment(R.layout.fragment_live_ocr) {

    private var _binding: FragmentLiveOcrBinding? = null
    private val binding get() = _binding!!
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var torchOn = false

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showPermissionDenied()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLiveOcrBinding.bind(view)

        binding.captureButton.setOnClickListener { capturePhoto() }
        binding.flashlightButton.setOnClickListener { toggleTorch() }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionDenied() {
        binding.previewView.visibility = View.GONE
        binding.hintText.visibility = View.GONE
        binding.captureButton.visibility = View.GONE
        binding.flashlightButton.visibility = View.GONE
        binding.permissionMessage.text = getString(R.string.permission_camera_denied)
        binding.permissionMessage.visibility = View.VISIBLE
        binding.permissionMessage.setOnClickListener {
            startActivity(
                android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData("package:${requireContext().packageName}".toUri())
            )
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture
                )
            } catch (t: Throwable) {
                Toast.makeText(requireContext(), R.string.error_camera_unavailable, Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun toggleTorch() {
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit()) {
            torchOn = !torchOn
            cam.cameraControl.enableTorch(torchOn)
        }
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return
        binding.captureProgress.visibility = View.VISIBLE
        binding.captureButton.isEnabled = false

        capture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    if (bitmap == null) {
                        onCaptureFailed(getString(R.string.error_ocr_failed))
                        return
                    }
                    runOcr(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    onCaptureFailed(exception.message)
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? = try {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val rotation = image.imageInfo.rotationDegrees
        if (decoded != null && rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else {
            decoded
        }
    } catch (t: Throwable) {
        null
    }

    private fun runOcr(bitmap: Bitmap) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val outcome = ServiceLocator.textRecognitionEngine.recognize(bitmap)) {
                is OcrOutcome.Success -> {
                    val language = ServiceLocator.languageDetectionEngine.detect(outcome.text)
                    findNavController().navigate(
                        LiveOcrFragmentDirections.actionLiveOcrToOcrResult(
                            outcome.text, language, HistoryType.CAMERA.name
                        )
                    )
                }
                OcrOutcome.NoTextFound -> onCaptureFailed(getString(R.string.error_no_text_found))
                is OcrOutcome.Failed -> onCaptureFailed(getString(R.string.error_ocr_failed))
            }
        }
    }

    private fun onCaptureFailed(message: String?) {
        _binding?.let {
            it.captureProgress.visibility = View.GONE
            it.captureButton.isEnabled = true
        }
        if (isAdded) {
            Toast.makeText(requireContext(), message ?: getString(R.string.error_ocr_failed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
