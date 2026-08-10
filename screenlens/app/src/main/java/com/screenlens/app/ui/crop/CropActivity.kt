package com.screenlens.app.ui.crop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.ActivityCropBinding
import com.screenlens.app.ocr.OcrOutcome
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropBinding
    private var sourceBitmap: Bitmap? = null
    private var displayScale = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val uri = uriString?.let { Uri.parse(it) }
        val bitmap = uri?.let { decodeAndCleanup(it) }
        if (bitmap == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        sourceBitmap = bitmap
        binding.capturedImage.setImageBitmap(bitmap)
        binding.capturedImage.doOnLaidOut { computeImageBounds(bitmap) }

        binding.cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        binding.resetButton.setOnClickListener { binding.cropOverlay.reset() }
        binding.analyzeButton.setOnClickListener { analyzeSelection() }
    }

    private fun decodeAndCleanup(uri: Uri): Bitmap? {
        val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        if (uri.scheme == "file") {
            File(uri.path.orEmpty()).delete()
        } else {
            runCatching { contentResolver.delete(uri, null, null) }
        }
        return bitmap
    }

    private fun View.doOnLaidOut(action: () -> Unit) {
        if (width > 0 && height > 0) {
            action()
        } else {
            viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    action()
                }
            })
        }
    }

    private fun computeImageBounds(bitmap: Bitmap) {
        val viewW = binding.capturedImage.width.toFloat()
        val viewH = binding.capturedImage.height.toFloat()
        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()
        if (viewW <= 0 || viewH <= 0 || bmpW <= 0 || bmpH <= 0) return

        val scale = min(viewW / bmpW, viewH / bmpH)
        displayScale = scale
        val displayW = bmpW * scale
        val displayH = bmpH * scale
        val left = (viewW - displayW) / 2f
        val top = (viewH - displayH) / 2f
        binding.cropOverlay.imageBounds = RectF(left, top, left + displayW, top + displayH)
    }

    private fun analyzeSelection() {
        val bitmap = sourceBitmap ?: return
        val bounds = binding.cropOverlay.imageBounds
        val selection = binding.cropOverlay.selectionRect

        val x = ((selection.left - bounds.left) / displayScale).toInt().coerceIn(0, bitmap.width - 1)
        val y = ((selection.top - bounds.top) / displayScale).toInt().coerceIn(0, bitmap.height - 1)
        val w = ((selection.width()) / displayScale).toInt().coerceIn(1, bitmap.width - x)
        val h = ((selection.height()) / displayScale).toInt().coerceIn(1, bitmap.height - y)
        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)

        setLoading(true)
        lifecycleScope.launch {
            when (val outcome = ServiceLocator.textRecognitionEngine.recognize(cropped)) {
                is OcrOutcome.Success -> {
                    val language = ServiceLocator.languageDetectionEngine.detect(outcome.text)
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(EXTRA_RESULT_TEXT, outcome.text).putExtra(EXTRA_RESULT_LANGUAGE, language)
                    )
                    finish()
                }
                OcrOutcome.NoTextFound -> {
                    setLoading(false)
                    Toast.makeText(this@CropActivity, R.string.error_no_text_found, Toast.LENGTH_SHORT).show()
                }
                is OcrOutcome.Failed -> {
                    setLoading(false)
                    Toast.makeText(this@CropActivity, R.string.error_ocr_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.analyzeProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.analyzeButton.isEnabled = !loading
        binding.resetButton.isEnabled = !loading
        binding.cancelButton.isEnabled = !loading
    }

    companion object {
        const val EXTRA_IMAGE_URI = "image_uri"
        const val EXTRA_RESULT_TEXT = "result_text"
        const val EXTRA_RESULT_LANGUAGE = "result_language"

        fun createIntent(context: Context, imageUri: Uri): Intent =
            Intent(context, CropActivity::class.java).putExtra(EXTRA_IMAGE_URI, imageUri.toString())
    }
}
