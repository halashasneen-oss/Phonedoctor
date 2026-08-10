package com.screenlens.app.ui.scan

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.screenlens.app.R
import com.screenlens.app.capture.CaptureEvent
import com.screenlens.app.capture.CaptureResultBus
import com.screenlens.app.capture.MediaProjectionPermissionActivity
import com.screenlens.app.databinding.FragmentScanBinding
import com.screenlens.app.domain.HistoryType
import com.screenlens.app.ui.crop.CropActivity
import com.screenlens.app.util.ImageCacheStore
import kotlinx.coroutines.launch

class ScanFragment : Fragment(R.layout.fragment_scan) {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val text = result.data!!.getStringExtra(CropActivity.EXTRA_RESULT_TEXT)
            val language = result.data!!.getStringExtra(CropActivity.EXTRA_RESULT_LANGUAGE)
            if (!text.isNullOrBlank()) {
                findNavController().navigate(
                    ScanFragmentDirections.actionScanToOcrResult(
                        originalText = text,
                        detectedLanguage = language,
                        sourceType = HistoryType.SCREEN.name
                    )
                )
            }
            showExplainer()
        } else {
            showExplainer()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScanBinding.bind(view)

        binding.allowCaptureButton.setOnClickListener {
            MediaProjectionPermissionActivity.start(requireContext())
        }
        binding.retryButton.setOnClickListener { showExplainer() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CaptureResultBus.events.collect { event -> handleEvent(event) }
            }
        }
    }

    private fun handleEvent(event: CaptureEvent) {
        when (event) {
            CaptureEvent.PermissionDenied -> showError(getString(R.string.error_no_capture_permission))
            CaptureEvent.Capturing -> showProgress(getString(R.string.scan_status_capturing))
            is CaptureEvent.Captured -> onCaptured(event.bitmap)
            is CaptureEvent.Failed -> showError(event.message ?: getString(R.string.error_ocr_failed))
        }
    }

    private fun onCaptured(bitmap: Bitmap) {
        showProgress(getString(R.string.scan_status_analyzing))
        val uri = ImageCacheStore.save(requireContext(), bitmap)
        bitmap.recycle()
        cropLauncher.launch(CropActivity.createIntent(requireContext(), uri))
    }

    private fun showExplainer() {
        _binding?.let {
            it.explainerContainer.visibility = View.VISIBLE
            it.progressContainer.visibility = View.GONE
            it.errorContainer.visibility = View.GONE
        }
    }

    private fun showProgress(status: String) {
        _binding?.let {
            it.explainerContainer.visibility = View.GONE
            it.progressContainer.visibility = View.VISIBLE
            it.errorContainer.visibility = View.GONE
            it.progressStatus.text = status
        }
    }

    private fun showError(message: String) {
        _binding?.let {
            it.explainerContainer.visibility = View.GONE
            it.progressContainer.visibility = View.GONE
            it.errorContainer.visibility = View.VISIBLE
            it.errorMessage.text = message
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
