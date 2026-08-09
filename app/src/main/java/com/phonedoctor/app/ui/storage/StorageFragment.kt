package com.phonedoctor.app.ui.storage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.phonedoctor.app.databinding.FragmentStorageBinding
import com.phonedoctor.app.domain.model.StorageBreakdown
import com.phonedoctor.app.domain.model.StorageInfo
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.ui.common.InfoRow
import com.phonedoctor.app.ui.common.InfoRowAdapter
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class StorageFragment : Fragment(R.layout.fragment_storage) {

    private val binding by viewBinding(FragmentStorageBinding::bind)

    private val viewModel: StorageViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return StorageViewModel(serviceLocator()) as T
            }
        }
    }

    private val breakdownAdapter = InfoRowAdapter()

    private val mediaPermissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.any { it }) {
            showBreakdownAndLoad()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerBreakdown.adapter = breakdownAdapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonViewBreakdown.setOnClickListener { requestBreakdownPermission() }

        if (hasMediaPermission()) {
            showBreakdownAndLoad()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.storageInfo.collect { info -> info?.let { renderStorage(it) } }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.breakdown.collect { breakdown -> breakdown?.let { renderBreakdown(it) } }
            }
        }
    }

    private fun hasMediaPermission(): Boolean =
        mediaPermissions.any { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }

    private fun requestBreakdownPermission() {
        if (hasMediaPermission()) {
            showBreakdownAndLoad()
        } else {
            permissionLauncher.launch(mediaPermissions)
        }
    }

    private fun showBreakdownAndLoad() {
        binding.containerRequestBreakdown.visibility = View.GONE
        binding.recyclerBreakdown.visibility = View.VISIBLE
        viewModel.loadBreakdown()
    }

    private fun renderStorage(info: StorageInfo) {
        binding.textUsedFree.text = getString(
            R.string.storage_used
        ) + ": " + FormatUtils.formatBytes(info.usedBytes) + " / " + FormatUtils.formatBytes(info.totalBytes)
        val percent = if (info.totalBytes > 0) (info.usedBytes * 100 / info.totalBytes).toInt() else 0
        binding.progressStorage.setProgressCompat(percent, true)
        binding.textUsed.text = "${getString(R.string.storage_used)}: ${FormatUtils.formatBytes(info.usedBytes)}"
        binding.textFree.text = "${getString(R.string.storage_free)}: ${FormatUtils.formatBytes(info.freeBytes)}"
    }

    private fun renderBreakdown(breakdown: StorageBreakdown) {
        val rows = listOf(
            InfoRow(getString(R.string.storage_category_images), FormatUtils.formatBytes(breakdown.imagesBytes)),
            InfoRow(getString(R.string.storage_category_videos), FormatUtils.formatBytes(breakdown.videosBytes)),
            InfoRow(getString(R.string.storage_category_audio), FormatUtils.formatBytes(breakdown.audioBytes)),
            InfoRow(getString(R.string.storage_category_documents), FormatUtils.formatBytes(breakdown.documentsBytes))
        )
        breakdownAdapter.submitList(rows)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
