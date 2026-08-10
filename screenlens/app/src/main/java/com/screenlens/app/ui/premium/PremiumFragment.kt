package com.screenlens.app.ui.premium

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.billing.PremiumState
import com.screenlens.app.databinding.FragmentPremiumBinding
import kotlinx.coroutines.launch

class PremiumFragment : Fragment(R.layout.fragment_premium) {

    private var _binding: FragmentPremiumBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPremiumBinding.bind(view)

        ServiceLocator.billingRepository.connect()

        binding.upgradeButton.setOnClickListener {
            val state = ServiceLocator.billingRepository.state.value
            if (state is PremiumState.Available) {
                ServiceLocator.billingRepository.launchPurchaseFlow(requireActivity(), state.productDetails)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ServiceLocator.billingRepository.state.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: PremiumState) {
        when (state) {
            PremiumState.Connecting -> {
                binding.upgradeButton.isEnabled = false
                binding.statusMessage.text = ""
            }
            is PremiumState.Available -> {
                binding.upgradeButton.isEnabled = true
                binding.upgradeButton.text = getString(
                    R.string.action_upgrade
                ) + " · " + (state.productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: "")
                binding.statusMessage.text = ""
            }
            PremiumState.NotConfigured -> {
                binding.upgradeButton.isEnabled = false
                binding.statusMessage.text = getString(R.string.premium_not_configured)
            }
            PremiumState.Purchased -> {
                binding.upgradeButton.isEnabled = false
                binding.upgradeButton.text = "✓"
                binding.statusMessage.text = ""
            }
            is PremiumState.Unavailable -> {
                binding.upgradeButton.isEnabled = false
                binding.statusMessage.text = state.reason
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
