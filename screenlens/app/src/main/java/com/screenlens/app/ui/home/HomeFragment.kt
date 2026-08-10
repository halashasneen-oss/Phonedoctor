package com.screenlens.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentHomeBinding
import com.screenlens.app.databinding.ItemHistoryCardBinding
import com.screenlens.app.domain.HistoryItem
import com.screenlens.app.ui.common.iconRes
import com.screenlens.app.ui.common.labelRes
import com.screenlens.app.ui.common.formatHistoryDate
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory { initializer { HomeViewModel(ServiceLocator.historyRepository) } }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
        binding.scanScreenButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scan)
        }
        binding.startFirstScanButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scan)
        }

        binding.quickActionImageOcr.icon.setImageResource(R.drawable.ic_type_image)
        binding.quickActionImageOcr.label.setText(R.string.quick_action_image_ocr)
        binding.quickActionImageOcr.root.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_imageOcr)
        }

        binding.quickActionTranslate.icon.setImageResource(R.drawable.ic_onboarding_translate)
        binding.quickActionTranslate.label.setText(R.string.quick_action_translate)
        binding.quickActionTranslate.root.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_translator)
        }

        binding.quickActionClipboard.icon.setImageResource(R.drawable.ic_type_clipboard)
        binding.quickActionClipboard.label.setText(R.string.quick_action_clipboard)
        binding.quickActionClipboard.root.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_clipboard)
        }

        binding.quickActionQr.icon.setImageResource(R.drawable.ic_type_qr)
        binding.quickActionQr.label.setText(R.string.quick_action_qr)
        binding.quickActionQr.root.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_qrScanner)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentScans.collect { items -> renderRecentScans(items) }
            }
        }

        if (ServiceLocator.adManager.isEnabled) {
            binding.bannerAd.apply {
                setAdSize(AdSize.BANNER)
                adUnitId = ServiceLocator.adManager.bannerAdUnitId
                visibility = View.VISIBLE
                loadAd(AdRequest.Builder().build())
            }
        }
    }

    private fun renderRecentScans(items: List<HistoryItem>) {
        binding.recentScansContainer.removeAllViews()
        if (items.isEmpty()) {
            binding.recentScansContainer.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
            return
        }
        binding.recentScansContainer.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE

        val inflater = LayoutInflater.from(requireContext())
        items.forEach { item ->
            val itemBinding = ItemHistoryCardBinding.inflate(inflater, binding.recentScansContainer, false)
            itemBinding.typeIcon.setImageResource(item.type.iconRes())
            itemBinding.originalText.text = item.originalText
            itemBinding.subtitle.text = getString(item.type.labelRes()) + " · " + formatHistoryDate(item.createdAt)
            itemBinding.root.setOnClickListener {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToHistoryDetail(item.id)
                )
            }
            binding.recentScansContainer.addView(itemBinding.root)
        }
    }

    override fun onPause() {
        _binding?.bannerAd?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        _binding?.bannerAd?.resume()
    }

    override fun onDestroyView() {
        binding.bannerAd.destroy()
        super.onDestroyView()
        _binding = null
    }
}
