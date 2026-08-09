package com.phonedoctor.app.ui.history

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentTestHistoryBinding
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class TestHistoryFragment : Fragment(R.layout.fragment_test_history) {

    private val binding by viewBinding(FragmentTestHistoryBinding::bind)

    private val viewModel: TestHistoryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return TestHistoryViewModel(serviceLocator()) as T
            }
        }
    }

    private val adapter = HistoryAdapter { report ->
        val action = com.phonedoctor.app.ui.history.TestHistoryFragmentDirections.actionHistoryToReport(report.id)
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerHistory.adapter = adapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonClear.setOnClickListener { confirmClear() }
        binding.buttonRunFromEmpty.setOnClickListener { findNavController().navigate(R.id.scanFragment) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.history.collect { list ->
                    adapter.submitList(list)
                    binding.recyclerHistory.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                    binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.history_clear_confirm_title)
            .setMessage(R.string.history_clear_confirm_message)
            .setPositiveButton(R.string.common_delete) { _, _ -> viewModel.clearHistory() }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }
}
