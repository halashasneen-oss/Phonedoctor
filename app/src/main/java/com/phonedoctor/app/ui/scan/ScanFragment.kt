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
import com.phonedoctor.app.ads.AdConsentManager
import com.phonedoctor.app.ads.AdFreePolicy
import com.phonedoctor.app.ads.AdManager
import com.phonedoctor.app.databinding.FragmentScanBinding
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.ScanMode
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.flow.first
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
        binding.buttonQuickScan.setOnClickListener {
            viewModel.start(ScanMode.QUICK)
        }
        binding.buttonDeepScan.setOnClickListener {
            viewModel.start(ScanMode.DEEP)
        }
        binding.buttonPerformanceScan.setOnClickListener {
            viewModel.start(ScanMode.PERFORMANCE)
        }
        binding.buttonCancel.setOnClickListener {
            viewModel.cancel()
            findNavController().navigateUp()
        }
        binding.buttonInteractiveContinue.setOnClickListener {
            viewModel.respondToInteraction(true)
        }
        binding.buttonInteractiveSkip.setOnClickListener {
            viewModel.respondToInteraction(false)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> render(state) }
                }
                launch {
                    AdConsentManager.canRequestAds.collect { canRequestAds ->
                        if (!canRequestAds) return@collect
                        val settings = serviceLocator().settingsRepository.settings.first()
                        if (!AdFreePolicy.isAdFree(settings.isPremium, settings.adFreeUntilMillis)) {
                            AdManager.loadInterstitialAd(requireContext())
                        }
                    }
                }
            }
        }

    }

    private fun render(state: ScanUiState) {
        adapter.submitList(state.items)

        binding.cardModeSelector.visibility = if (state.started) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.progressIndicator.visibility = if (state.started) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.recyclerScanItems.visibility = if (state.started) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.buttonCancel.visibility = if (state.started) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.textTitle.setText(
            when (state.selectedMode) {
                ScanMode.QUICK -> R.string.scan_quick_title
                ScanMode.DEEP -> R.string.scan_deep_title
                ScanMode.PERFORMANCE -> R.string.scan_performance_title
                ScanMode.BACKGROUND -> R.string.scan_title
                null -> R.string.scan_choose_mode_title
            }
        )

        binding.progressIndicator.setProgressCompat(
            state.progressPercent,
            true
        )

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
            viewLifecycleOwner.lifecycleScope.launch {
                val settings = serviceLocator().settingsRepository.settings.first()
                val skipAds = AdFreePolicy.isAdFree(
                    settings.isPremium,
                    settings.adFreeUntilMillis
                ) || !AdConsentManager.canRequestAds.value

                if (skipAds) {
                    if (isAdded) findNavController().navigate(action)
                } else {
                    AdManager.showInterstitialAd(requireActivity()) {
                        if (isAdded) findNavController().navigate(action)
                    }
                }
            }
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
