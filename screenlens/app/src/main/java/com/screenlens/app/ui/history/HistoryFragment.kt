package com.screenlens.app.ui.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentHistoryBinding
import com.screenlens.app.domain.HistoryType
import com.screenlens.app.ui.common.HistoryItemAdapter
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels {
        viewModelFactory { initializer { HistoryViewModel(ServiceLocator.historyRepository) } }
    }

    private val adapter = HistoryItemAdapter { item ->
        findNavController().navigate(HistoryFragmentDirections.actionHistoryToDetail(item.id))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setQuery(s?.toString().orEmpty())
            }
        })

        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: binding.chipAll.id
            viewModel.setFilter(
                when (id) {
                    binding.chipScreen.id -> HistoryType.SCREEN
                    binding.chipImage.id -> HistoryType.IMAGE
                    binding.chipCamera.id -> HistoryType.CAMERA
                    binding.chipClipboard.id -> HistoryType.CLIPBOARD
                    binding.chipTranslation.id -> HistoryType.TRANSLATION
                    binding.chipQr.id -> HistoryType.QR
                    else -> null
                }
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submitList(items)
                    binding.emptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    binding.historyList.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                    val query = viewModel.query.value
                    binding.emptyText.text = if (query.isNotBlank()) {
                        getString(R.string.history_empty_search, query)
                    } else {
                        getString(R.string.history_empty)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
