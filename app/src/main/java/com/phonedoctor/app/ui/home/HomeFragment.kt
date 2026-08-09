package com.phonedoctor.app.ui.home

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
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.phonedoctor.app.BuildConfig
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentHomeBinding
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)

    private val viewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(serviceLocator(), requireContext().applicationContext) as T
            }
        }
    }

    private lateinit var quickTestAdapter: QuickTestAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quickTestAdapter = QuickTestAdapter { item -> findNavController().navigate(item.navActionId) }
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

        if (BuildConfig.SHOW_ADS) {
            val adView = AdView(requireContext()).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.BANNER_AD_UNIT_ID
            }
            binding.containerBannerAd.addView(adView)
            binding.containerBannerAd.visibility = View.VISIBLE
            adView.loadAd(AdRequest.Builder().build())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.healthRing.progress = state.healthScore
                    binding.textHealthScore.text = "${state.healthScore}%"
                    binding.textHealthStatus.setText(state.healthStatusRes)
                    binding.textLastCheck.text = state.lastCheckText
                    quickTestAdapter.submitList(state.quickTests)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
