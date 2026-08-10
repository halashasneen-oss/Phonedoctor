package com.screenlens.app.ui.imageocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentImageOcrBinding
import com.screenlens.app.domain.HistoryType
import com.screenlens.app.ocr.OcrOutcome
import kotlinx.coroutines.launch

class ImageOcrFragment : Fragment(R.layout.fragment_image_ocr) {

    private var _binding: FragmentImageOcrBinding? = null
    private val binding get() = _binding!!
    private var selectedBitmap: Bitmap? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) loadBitmap(uri)
    }

    private var pendingPhotoUri: Uri? = null
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) loadBitmap(uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImageOcrBinding.bind(view)

        binding.chooseImageButton.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.takePhotoButton.setOnClickListener { launchCamera() }
        binding.detectTextButton.setOnClickListener { runOcr() }
    }

    private fun launchCamera() {
        val photoFile = java.io.File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", photoFile
        )
        pendingPhotoUri = uri
        takePhoto.launch(uri)
    }

    private fun loadBitmap(uri: Uri) {
        val bitmap = decodeUri(requireContext(), uri) ?: run {
            Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
            return
        }
        selectedBitmap = bitmap
        binding.previewImage.setImageBitmap(bitmap)
        binding.previewImage.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE
        binding.detectTextButton.visibility = View.VISIBLE
    }

    private fun decodeUri(context: Context, uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (t: Throwable) {
        null
    }

    private fun runOcr() {
        val bitmap = selectedBitmap ?: return
        binding.progress.visibility = View.VISIBLE
        binding.detectTextButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            when (val outcome = ServiceLocator.textRecognitionEngine.recognize(bitmap)) {
                is OcrOutcome.Success -> {
                    val language = ServiceLocator.languageDetectionEngine.detect(outcome.text)
                    findNavController().navigate(
                        ImageOcrFragmentDirections.actionImageOcrToOcrResult(
                            outcome.text, language, HistoryType.IMAGE.name
                        )
                    )
                }
                OcrOutcome.NoTextFound -> {
                    binding.progress.visibility = View.GONE
                    binding.detectTextButton.isEnabled = true
                    Toast.makeText(requireContext(), R.string.error_no_text_found, Toast.LENGTH_SHORT).show()
                }
                is OcrOutcome.Failed -> {
                    binding.progress.visibility = View.GONE
                    binding.detectTextButton.isEnabled = true
                    Toast.makeText(requireContext(), R.string.error_ocr_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        selectedBitmap?.recycle()
        selectedBitmap = null
        _binding = null
    }
}
