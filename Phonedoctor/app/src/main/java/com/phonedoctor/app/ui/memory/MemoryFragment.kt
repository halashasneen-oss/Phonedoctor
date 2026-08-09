package com.phonedoctor.app.ui.memory

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
import com.phonedoctor.app.databinding.FragmentMemoryBinding
import com.phonedoctor.app.domain.model.MemoryInfo
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.ui.common.InfoRow
import com.phonedoctor.app.ui.common.InfoRowAdapter
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class MemoryFragment : Fragment(R.layout.fragment_memory) {

    private val binding by viewBinding(FragmentMemoryBinding::bind)

    private val viewModel: MemoryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return MemoryViewModel(serviceLocator()) as T
            }
        }
    }

    private val adapter = InfoRowAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerInfo.adapter = adapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.memoryInfo.collect { info -> info?.let { render(it) } }
            }
        }
    }

    private fun render(info: MemoryInfo) {
        val percent = if (info.totalBytes > 0) (info.usedBytes * 100 / info.totalBytes).toInt() else 0
        binding.progressMemory.setProgressCompat(percent, true)
        binding.textLowMemoryWarning.visibility = if (info.isLowMemory) View.VISIBLE else View.GONE

        val rows = listOf(
            InfoRow(getString(R.string.memory_total), FormatUtils.formatBytes(info.totalBytes)),
            InfoRow(getString(R.string.memory_available), FormatUtils.formatBytes(info.availableBytes)),
            InfoRow(getString(R.string.memory_used), "${FormatUtils.formatBytes(info.usedBytes)} ($percent%)")
        )
        adapter.submitList(rows)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
