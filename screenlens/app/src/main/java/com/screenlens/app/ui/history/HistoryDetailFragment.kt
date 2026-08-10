package com.screenlens.app.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentHistoryDetailBinding
import com.screenlens.app.domain.HistoryItem
import com.screenlens.app.translate.LanguageCatalog
import com.screenlens.app.ui.common.formatHistoryDate
import com.screenlens.app.ui.common.labelRes
import kotlinx.coroutines.launch

class HistoryDetailFragment : Fragment(R.layout.fragment_history_detail) {

    private var _binding: FragmentHistoryDetailBinding? = null
    private val binding get() = _binding!!
    private val args: HistoryDetailFragmentArgs by navArgs()
    private var currentItem: HistoryItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryDetailBinding.bind(view)

        binding.copyButton.setOnClickListener { copyItem() }
        binding.shareButton.setOnClickListener { shareItem() }
        binding.deleteButton.setOnClickListener { confirmDelete() }

        lifecycleScope.launch {
            val item = ServiceLocator.historyRepository.getById(args.historyId)
            if (item == null) {
                findNavController().popBackStack()
                return@launch
            }
            currentItem = item
            render(item)
        }
    }

    private fun render(item: HistoryItem) {
        binding.dateText.text = formatHistoryDate(item.createdAt)
        binding.typeText.text = getString(item.type.labelRes())
        binding.originalText.text = item.originalText

        if (item.translatedText.isNullOrBlank()) {
            binding.translationLabel.visibility = View.GONE
            binding.translationText.visibility = View.GONE
        } else {
            binding.translationText.text = item.translatedText
        }

        val source = item.sourceLanguage?.let { LanguageCatalog.byCode(it)?.englishName ?: it }
        val target = item.targetLanguage?.let { LanguageCatalog.byCode(it)?.englishName ?: it }
        if (source == null && target == null) {
            binding.languagesLabel.visibility = View.GONE
            binding.languagesText.visibility = View.GONE
        } else {
            binding.languagesText.text = listOfNotNull(source, target).joinToString(" → ")
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                currentItem?.let { item ->
                    lifecycleScope.launch {
                        ServiceLocator.historyRepository.delete(item)
                        findNavController().popBackStack()
                    }
                }
            }
            .show()
    }

    private fun combinedShareText(item: HistoryItem): String =
        if (item.translatedText.isNullOrBlank()) item.originalText else "${item.originalText}\n\n${item.translatedText}"

    private fun copyItem() {
        val item = currentItem ?: return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ScreenLens", combinedShareText(item)))
        android.widget.Toast.makeText(requireContext(), R.string.toast_copied, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun shareItem() {
        val item = currentItem ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, combinedShareText(item))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
