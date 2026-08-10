package com.screenlens.app.ui.qrscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.barcode.common.Barcode
import com.screenlens.app.R
import com.screenlens.app.databinding.FragmentQrScannerBinding
import java.util.concurrent.Executors

class QrScannerFragment : Fragment(R.layout.fragment_qr_scanner) {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var analyzer: BarcodeAnalyzer? = null

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showPermissionDenied()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQrScannerBinding.bind(view)

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
        binding.scanFrame.visibility = View.GONE
        binding.hintText.visibility = View.GONE
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
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            val barcodeAnalyzer = BarcodeAnalyzer { barcode -> onBarcodeDetected(barcode) }
            analyzer = barcodeAnalyzer
            analysis.setAnalyzer(analysisExecutor, barcodeAnalyzer)

            try {
                provider.unbindAll()
                provider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (t: Throwable) {
                android.widget.Toast.makeText(requireContext(), R.string.error_camera_unavailable, android.widget.Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun onBarcodeDetected(barcode: Barcode) {
        val rawValue = barcode.rawValue ?: return
        val isUrl = barcode.valueType == Barcode.TYPE_URL ||
            rawValue.startsWith("http://", ignoreCase = true) ||
            rawValue.startsWith("https://", ignoreCase = true)

        activity?.runOnUiThread {
            if (isAdded) {
                findNavController().navigate(
                    QrScannerFragmentDirections.actionQrScannerToQrResult(
                        rawValue, if (isUrl) "URL" else "TEXT"
                    )
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        analysisExecutor.shutdown()
        _binding = null
    }
}
