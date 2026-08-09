package com.phonedoctor.app.ui.scan

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentScanBinding
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class ScanFragment : Fragment(R.layout.fragment_scan) {

    private val binding by viewBinding(FragmentScanBinding::bind)

    private val viewModel: ScanViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return ScanViewModel(serviceLocator(), requireContext().applicationContext) as T
            }
        }
    }

    private val adapter = ScanListAdapter()
    private var navigatedToResults = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerScanItems.adapter = adapter
        binding.buttonCancel.setOnClickListener { findNavController().navigateUp() }
        binding.buttonInteractiveContinue.setOnClickListener { viewModel.respondToInteraction(true) }
        binding.buttonInteractiveSkip.setOnClickListener { viewModel.respondToInteraction(false) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }

        viewModel.start()
    }

    private fun render(state: ScanUiState) {
        adapter.submitList(state.items)
        binding.progressIndicator.setProgressCompat(state.progressPercent, true)

        val interactive = state.pendingInteraction
        if (interactive != null) {
            binding.cardInteractive.visibility = View.VISIBLE
            binding.textInteractiveBody.setText(interactiveBodyRes(interactive))
        } else {
            binding.cardInteractive.visibility = View.GONE
        }

        val reportId = state.finishedReportId
        if (reportId != null && !navigatedToResults) {
            navigatedToResults = true
            val action = ScanFragmentDirections.actionScanToResults(reportId)
            findNavController().navigate(action)
        }
    }

    private fun interactiveBodyRes(category: DiagnosticCategory): Int = when (category) {
        DiagnosticCategory.DISPLAY -> R.string.scan_interactive_display
        DiagnosticCategory.TOUCH -> R.string.scan_interactive_touch
        DiagnosticCategory.AUDIO -> R.string.scan_interactive_audio
        DiagnosticCategory.MICROPHONE -> R.string.scan_interactive_microphone
        else -> R.string.scan_interactive_display
    }
}
