package com.screenlens.app.ui.ocrresult

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentOcrResultBinding
import com.screenlens.app.domain.HistoryType
import com.screenlens.app.translate.LanguageCatalog
import kotlinx.coroutines.launch

class OcrResultFragment : Fragment(R.layout.fragment_ocr_result) {

    private var _binding: FragmentOcrResultBinding? = null
    private val binding get() = _binding!!
    private val args: OcrResultFragmentArgs by navArgs()
    private var isEditing = false

    private val viewModel: OcrResultViewModel by viewModels {
        viewModelFactory {
            initializer {
                OcrResultViewModel(
                    initialText = args.originalText,
                    detectedLanguage = args.detectedLanguage,
                    sourceType = runCatching { HistoryType.valueOf(args.sourceType) }.getOrDefault(HistoryType.SCREEN),
                    historyRepository = ServiceLocator.historyRepository,
                    settingsDataStore = ServiceLocator.settingsDataStore
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOcrResultBinding.bind(view)

        binding.detectedLanguageView.text = args.detectedLanguage
            ?.let { LanguageCatalog.byCode(it)?.englishName ?: it }
            ?: getString(R.string.ocr_language_unknown)

        binding.copyButton.setOnClickListener { copyText() }
        binding.shareButton.setOnClickListener { shareText() }
        binding.saveButton.setOnClickListener {
            viewModel.save()
            Toast.makeText(requireContext(), R.string.toast_saved_to_history, Toast.LENGTH_SHORT).show()
        }
        binding.editButton.setOnClickListener { toggleEdit() }
        binding.translateButton.setOnClickListener {
            findNavController().navigate(
                OcrResultFragmentDirections.actionOcrResultToTranslator(viewModel.text.value, args.detectedLanguage)
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.text.collect { text ->
                    binding.originalTextView.text = text
                    if (binding.originalTextEdit.text.toString() != text) {
                        binding.originalTextEdit.setText(text)
                    }
                }
            }
        }
    }

    private fun toggleEdit() {
        isEditing = !isEditing
        if (isEditing) {
            binding.originalTextView.visibility = View.GONE
            binding.originalTextEdit.visibility = View.VISIBLE
            binding.originalTextEdit.requestFocus()
            binding.editButton.setText(R.string.action_done_editing)
        } else {
            viewModel.updateText(binding.originalTextEdit.text.toString())
            binding.originalTextView.visibility = View.VISIBLE
            binding.originalTextEdit.visibility = View.GONE
            binding.editButton.setText(R.string.action_edit)
        }
    }

    private fun copyText() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ScreenLens", viewModel.text.value))
        Toast.makeText(requireContext(), R.string.toast_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareText() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, viewModel.text.value)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
