package com.screenlens.app.ui.translator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentTranslatorBinding
import com.screenlens.app.translate.LanguageCatalog
import kotlinx.coroutines.launch

class TranslatorFragment : Fragment(R.layout.fragment_translator) {

    private var _binding: FragmentTranslatorBinding? = null
    private val binding get() = _binding!!
    private val args: TranslatorFragmentArgs by navArgs()

    private val viewModel: TranslatorViewModel by viewModels {
        viewModelFactory {
            initializer {
                TranslatorViewModel(
                    initialText = args.initialText,
                    initialSourceLanguage = args.initialSourceLanguage,
                    translationEngine = ServiceLocator.translationEngine,
                    historyRepository = ServiceLocator.historyRepository,
                    settingsDataStore = ServiceLocator.settingsDataStore
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTranslatorBinding.bind(view)

        if (!args.initialText.isNullOrEmpty()) {
            binding.inputText.setText(args.initialText)
        }

        binding.inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setInputText(s?.toString().orEmpty())
            }
        })

        binding.sourceLanguageButton.setOnClickListener {
            showLanguagePicker { viewModel.setSourceLanguage(it) }
        }
        binding.targetLanguageButton.setOnClickListener {
            showLanguagePicker { viewModel.setTargetLanguage(it) }
        }
        binding.swapButton.setOnClickListener { viewModel.swapLanguages() }
        binding.copyButton.setOnClickListener { copyOutput() }
        binding.shareButton.setOnClickListener { shareOutput() }
        binding.saveButton.setOnClickListener {
            viewModel.saveManually()
            Toast.makeText(requireContext(), R.string.toast_saved_to_history, Toast.LENGTH_SHORT).show()
        }
        binding.downloadModelButton.setOnClickListener { downloadMissingModels() }

        observeState()
    }

    private var missingCodes: List<String> = emptyList()

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.sourceLanguage.collect {
                        binding.sourceLanguageButton.text = it?.englishName ?: "—"
                    }
                }
                launch {
                    viewModel.targetLanguage.collect {
                        binding.targetLanguageButton.text = it?.englishName ?: "—"
                    }
                }
                launch {
                    viewModel.uiState.collect { state -> render(state) }
                }
                launch {
                    viewModel.downloadState.collect { state -> renderDownload(state) }
                }
            }
        }
    }

    private fun render(state: TranslateUiState) {
        binding.translatingProgress.visibility = if (state is TranslateUiState.Translating) View.VISIBLE else View.GONE
        binding.modelMissingContainer.visibility = if (state is TranslateUiState.ModelsNotInstalled) View.VISIBLE else View.GONE

        when (state) {
            is TranslateUiState.Success -> {
                binding.outputText.visibility = View.VISIBLE
                binding.outputText.text = state.text
            }
            is TranslateUiState.ModelsNotInstalled -> {
                binding.outputText.visibility = View.GONE
                missingCodes = state.missingCodes
                val names = state.missingCodes.mapNotNull { LanguageCatalog.byCode(it)?.englishName }.joinToString(", ")
                binding.modelMissingText.text = getString(R.string.error_model_missing) +
                    if (names.isNotEmpty()) " ($names)" else ""
            }
            TranslateUiState.NoInternet -> {
                binding.outputText.visibility = View.VISIBLE
                binding.outputText.text = getString(R.string.error_no_internet)
            }
            is TranslateUiState.Error -> {
                binding.outputText.visibility = View.VISIBLE
                binding.outputText.text = getString(R.string.error_translation_failed)
            }
            TranslateUiState.Translating, TranslateUiState.Idle -> {
                binding.outputText.visibility = View.VISIBLE
                if (state is TranslateUiState.Idle) binding.outputText.text = ""
            }
        }
    }

    private fun renderDownload(state: ModelDownloadUiState) {
        binding.downloadProgress.visibility = if (state is ModelDownloadUiState.Downloading) View.VISIBLE else View.GONE
        binding.downloadModelButton.isEnabled = state !is ModelDownloadUiState.Downloading
        when (state) {
            ModelDownloadUiState.NoInternet ->
                Toast.makeText(requireContext(), R.string.error_no_internet_download, Toast.LENGTH_SHORT).show()
            is ModelDownloadUiState.Failed ->
                Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
            else -> Unit
        }
    }

    private fun downloadMissingModels() {
        missingCodes.forEach { viewModel.downloadModel(it, requireWifi = false) }
    }

    private fun showLanguagePicker(onPicked: (com.screenlens.app.translate.TranslateLanguageInfo) -> Unit) {
        val languages = LanguageCatalog.all
        val names = languages.map { "${it.englishName} (${it.nativeName})" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_target_language)
            .setItems(names) { _, index -> onPicked(languages[index]) }
            .show()
    }

    private fun copyOutput() {
        val text = binding.outputText.text.toString()
        if (text.isBlank()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ScreenLens", text))
        Toast.makeText(requireContext(), R.string.toast_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareOutput() {
        val text = binding.outputText.text.toString()
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
