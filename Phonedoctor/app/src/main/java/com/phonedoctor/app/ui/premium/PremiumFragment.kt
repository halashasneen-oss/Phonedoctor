package com.phonedoctor.app.ui.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.phonedoctor.app.R
import com.phonedoctor.app.ads.AdManager
import com.phonedoctor.app.databinding.FragmentPremiumBinding
import com.phonedoctor.app.databinding.ItemPremiumBenefitBinding
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class PremiumFragment : Fragment(R.layout.fragment_premium) {

    private val binding by viewBinding(FragmentPremiumBinding::bind)

    private val benefits = listOf(
        R.string.premium_benefit_remove_ads,
        R.string.premium_benefit_advanced_reports,
        R.string.premium_benefit_extra_diagnostics,
        R.string.premium_benefit_advanced_device_info,
        R.string.premium_benefit_custom_reports
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        val inflater = LayoutInflater.from(requireContext())
        benefits.forEach { res ->
            val itemBinding = ItemPremiumBenefitBinding.inflate(inflater, binding.containerBenefits, false)
            itemBinding.textBenefit.setText(res)
            binding.containerBenefits.addView(itemBinding.root)
        }

        binding.buttonUpgrade.setOnClickListener {
            Snackbar.make(binding.root, R.string.premium_billing_unavailable, Snackbar.LENGTH_LONG).show()
        }
        binding.buttonWatchAd.setOnClickListener { watchRewardedAd() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                serviceLocator().settingsRepository.settings.collect { settings ->
                    binding.textAlreadyPremium.visibility = if (settings.isPremium) View.VISIBLE else View.GONE
                    binding.buttonUpgrade.visibility = if (settings.isPremium) View.GONE else View.VISIBLE
                    binding.buttonWatchAd.visibility = if (settings.isPremium) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun watchRewardedAd() {
        binding.buttonWatchAd.isEnabled = false
        AdManager.loadRewardedAd(requireContext()) { loaded ->
            if (!isAdded) return@loadRewardedAd
            if (!loaded) {
                binding.buttonWatchAd.isEnabled = true
                Snackbar.make(binding.root, R.string.common_not_available, Snackbar.LENGTH_SHORT).show()
                return@loadRewardedAd
            }
            AdManager.showRewardedAd(
                requireActivity(),
                onRewardEarned = {
                    if (isAdded) Snackbar.make(binding.root, R.string.premium_benefit_advanced_reports, Snackbar.LENGTH_SHORT).show()
                },
                onDismissed = {
                    if (isAdded) binding.buttonWatchAd.isEnabled = true
                }
            )
        }
    }
}
