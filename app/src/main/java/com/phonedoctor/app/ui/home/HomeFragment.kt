package com.phonedoctor.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar
import com.phonedoctor.app.BuildConfig
import com.phonedoctor.app.R
import com.phonedoctor.app.ads.AdConsentManager
import com.phonedoctor.app.ads.AdFreePolicy
import com.phonedoctor.app.ads.AdManager
import com.phonedoctor.app.data.datastore.AppSettings
import com.phonedoctor.app.databinding.FragmentHomeBinding
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)

    private val viewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(
                    serviceLocator(),
                    requireContext().applicationContext
                ) as T
            }
        }
    }

    private lateinit var quickTestAdapter: QuickTestAdapter
    private var bannerAdView: AdView? = null
    private var countdownJob: Job? = null
    private var latestSettings: AppSettings? = null

    private val shareLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isAdded) showRewardedAdForAdFreeHour()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quickTestAdapter = QuickTestAdapter { item ->
            findNavController().navigate(item.navActionId)
        }
        binding.recyclerQuickTests.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerQuickTests.adapter = quickTestAdapter
        binding.recyclerQuickTests.isNestedScrollingEnabled = false

        val moreToolAdapter = MoreToolAdapter(viewModel.uiState.value.moreTools) { tool ->
            findNavController().navigate(tool.navActionId)
        }
        binding.recyclerMoreTools.adapter = moreToolAdapter

        binding.buttonSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
        binding.buttonHistory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }
        binding.buttonRunFullCheck.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scan)
        }
        binding.buttonAdFreeReward.setOnClickListener {
            launchShareFlow()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.healthRing.progress = state.healthScore
                        binding.textHealthScore.text = "${state.healthScore}%"
                        binding.textHealthStatus.setText(state.healthStatusRes)
                        binding.textLastCheck.text = state.lastCheckText
                        quickTestAdapter.submitList(state.quickTests)
                    }
                }

                launch {
                    combine(
                        serviceLocator().settingsRepository.settings,
                        AdConsentManager.canRequestAds
                    ) { settings, canRequestAds ->
                        settings to canRequestAds
                    }.collect { (settings, canRequestAds) ->
                        latestSettings = settings
                        renderAdState(settings, canRequestAds)
                    }
                }
            }
        }
    }

    private fun renderAdState(settings: AppSettings, canRequestAds: Boolean) {
        if (!BuildConfig.SHOW_ADS || settings.isPremium) {
            binding.cardAdFreeReward.visibility = View.GONE
            destroyBanner()
            return
        }

        binding.cardAdFreeReward.visibility = if (
            BuildConfig.REWARDED_ADS_ENABLED
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val adFree = AdFreePolicy.isAdFree(
            isPremium = settings.isPremium,
            adFreeUntilMillis = settings.adFreeUntilMillis
        )
        binding.buttonAdFreeReward.isEnabled = !adFree

        countdownJob?.cancel()
        if (adFree) {
            destroyBanner()
            startCountdown(settings.adFreeUntilMillis)
        } else {
            binding.textAdFreeRewardStatus.setText(R.string.home_adfree_desc)
            binding.buttonAdFreeReward.setText(R.string.home_adfree_button)
            if (canRequestAds) loadBannerIfNeeded()
        }
    }

    private fun startCountdown(expiresAt: Long) {
        countdownJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                val remaining = AdFreePolicy.remainingMillis(expiresAt)
                if (remaining <= 0L) {
                    binding.textAdFreeRewardStatus.setText(R.string.home_adfree_desc)
                    binding.buttonAdFreeReward.isEnabled = true
                    binding.buttonAdFreeReward.setText(R.string.home_adfree_button)
                    if (AdConsentManager.canRequestAds.value) loadBannerIfNeeded()
                    break
                }

                val totalSeconds = remaining / 1000L
                val minutes = totalSeconds / 60L
                val seconds = totalSeconds % 60L
                binding.textAdFreeRewardStatus.text =
                    getString(R.string.home_adfree_remaining, minutes, seconds)
                binding.buttonAdFreeReward.setText(R.string.home_adfree_active)
                delay(1000L)
            }
        }
    }

    private fun loadBannerIfNeeded() {
        if (bannerAdView != null || !BuildConfig.SHOW_ADS) return

        binding.containerBannerAd.post {
            if (!isAdded || bannerAdView != null) return@post
            val settings = latestSettings ?: return@post
            if (AdFreePolicy.isAdFree(settings.isPremium, settings.adFreeUntilMillis)) {
                return@post
            }

            val availableWidth = binding.containerBannerAd.width
                .takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels

            val adView = AdManager.createAndLoadAdaptiveBanner(
                context = requireContext(),
                availableWidthPx = availableWidth,
                onLoaded = {
                    if (isAdded) binding.containerBannerAd.visibility = View.VISIBLE
                },
                onFailed = {
                    if (isAdded) binding.containerBannerAd.visibility = View.GONE
                }
            )
            if (adView != null) {
                bannerAdView = adView
                binding.containerBannerAd.removeAllViews()
                binding.containerBannerAd.addView(adView)
            }
        }
    }

    private fun destroyBanner() {
        binding.containerBannerAd.visibility = View.GONE
        bannerAdView?.destroy()
        bannerAdView = null
        binding.containerBannerAd.removeAllViews()
    }

    private fun launchShareFlow() {
        val shareText = buildString {
            append(getString(R.string.home_share_text))
            append("\n")
            append("https://play.google.com/store/apps/details?id=com.phonedoctor.app")
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        shareLauncher.launch(
            Intent.createChooser(sendIntent, getString(R.string.home_share_chooser))
        )
    }

    private fun showRewardedAdForAdFreeHour() {
        if (
            !BuildConfig.REWARDED_ADS_ENABLED ||
            BuildConfig.REWARDED_AD_UNIT_ID.isBlank() ||
            !AdConsentManager.canRequestAds.value
        ) {
            Snackbar.make(
                binding.root,
                R.string.home_adfree_reward_unavailable,
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        binding.buttonAdFreeReward.isEnabled = false
        binding.textAdFreeRewardStatus.setText(R.string.home_adfree_reward_loading)

        AdManager.loadRewardedAd(requireContext()) { loaded ->
            if (!isAdded) return@loadRewardedAd
            if (!loaded) {
                binding.buttonAdFreeReward.isEnabled = true
                binding.textAdFreeRewardStatus.setText(R.string.home_adfree_desc)
                Snackbar.make(
                    binding.root,
                    R.string.home_adfree_reward_unavailable,
                    Snackbar.LENGTH_LONG
                ).show()
                return@loadRewardedAd
            }

            AdManager.showRewardedAd(
                requireActivity(),
                onRewardEarned = {
                    if (isAdded) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            serviceLocator().settingsRepository.grantOneHourAdFree()
                            Snackbar.make(
                                binding.root,
                                R.string.home_adfree_reward_granted,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onDismissed = {
                    if (isAdded) binding.buttonAdFreeReward.isEnabled = true
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        countdownJob?.cancel()
        destroyBanner()
        super.onDestroyView()
    }
}
