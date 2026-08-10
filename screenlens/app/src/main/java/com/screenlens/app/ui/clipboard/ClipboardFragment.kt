package com.screenlens.app.ui.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.screenlens.app.R
import com.screenlens.app.databinding.FragmentClipboardBinding

/**
 * Reads the clipboard only when the user explicitly taps "Check Clipboard" — never
 * on a timer or in the background, respecting Android's clipboard-access
 * restrictions and the user's expectation of privacy.
 */
class ClipboardFragment : Fragment(R.layout.fragment_clipboard) {

    private var _binding: FragmentClipboardBinding? = null
    private val binding get() = _binding!!
    private var currentText: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentClipboardBinding.bind(view)

        binding.checkClipboardButton.setOnClickListener { checkClipboard() }
        binding.translateButton.setOnClickListener {
            findNavController().navigate(
                ClipboardFragmentDirections.actionClipboardToTranslator(currentText, null)
            )
        }

        checkClipboard()
    }

    private fun checkClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = if (clipboard.hasPrimaryClip() && (clipboard.primaryClip?.itemCount ?: 0) > 0) {
            clipboard.primaryClip?.getItemAt(0)?.coerceToText(requireContext())?.toString()
        } else {
            null
        }

        currentText = text?.takeIf { it.isNotBlank() }
        binding.clipboardText.text = currentText ?: getString(R.string.clipboard_empty)
        binding.translateButton.isEnabled = currentText != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
