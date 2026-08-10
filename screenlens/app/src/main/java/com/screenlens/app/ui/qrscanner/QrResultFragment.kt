package com.screenlens.app.ui.qrscanner

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentQrResultBinding
import com.screenlens.app.domain.HistoryItem
import com.screenlens.app.domain.HistoryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QrResultFragment : Fragment(R.layout.fragment_qr_result) {

    private var _binding: FragmentQrResultBinding? = null
    private val binding get() = _binding!!
    private val args: QrResultFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQrResultBinding.bind(view)

        val isUrl = args.valueType == "URL"
        binding.resultTypeLabel.setText(if (isUrl) R.string.qr_result_url_type else R.string.qr_result_text_type)
        binding.rawValueText.text = args.rawValue
        binding.openLinkButton.visibility = if (isUrl) View.VISIBLE else View.GONE

        binding.openLinkButton.setOnClickListener { openLink() }
        binding.copyButton.setOnClickListener { copyValue() }
        binding.shareButton.setOnClickListener { shareValue() }

        autoSaveIfEnabled()
    }

    private fun autoSaveIfEnabled() {
        lifecycleScope.launch {
            val settings = ServiceLocator.settingsDataStore.settings.first()
            if (!settings.autoSaveHistory) return@launch
            ServiceLocator.historyRepository.save(
                HistoryItem(
                    createdAt = System.currentTimeMillis(),
                    type = HistoryType.QR,
                    originalText = args.rawValue,
                    translatedText = null,
                    sourceLanguage = null,
                    targetLanguage = null,
                    detectedLanguage = null
                ),
                settings.historyLimit
            )
        }
    }

    private fun openLink() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(args.rawValue)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyValue() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ScreenLens", args.rawValue))
        Toast.makeText(requireContext(), R.string.toast_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareValue() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, args.rawValue)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
