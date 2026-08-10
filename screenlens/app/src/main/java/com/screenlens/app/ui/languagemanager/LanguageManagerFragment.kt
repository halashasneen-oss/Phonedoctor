package com.screenlens.app.ui.languagemanager

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.FragmentLanguageManagerBinding
import kotlinx.coroutines.launch

class LanguageManagerFragment : Fragment(R.layout.fragment_language_manager) {

    private var _binding: FragmentLanguageManagerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LanguageManagerViewModel by viewModels {
        viewModelFactory { initializer { LanguageManagerViewModel(ServiceLocator.translationEngine) } }
    }

    private val installedAdapter = LanguageRowAdapter(installed = true) { viewModel.remove(it.code) }
    private val availableAdapter = LanguageRowAdapter(installed = false) { viewModel.download(it.code) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLanguageManagerBinding.bind(view)

        binding.installedList.layoutManager = LinearLayoutManager(requireContext())
        binding.installedList.adapter = installedAdapter
        binding.availableList.layoutManager = LinearLayoutManager(requireContext())
        binding.availableList.adapter = availableAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    installedAdapter.submit(state.installed, state.pendingCode)
                    availableAdapter.submit(state.available, state.pendingCode)
                    binding.emptyInstalledText.visibility = if (state.installed.isEmpty()) View.VISIBLE else View.GONE

                    state.message?.let { message ->
                        val text = when (message) {
                            Message.NoInternet -> getString(R.string.error_no_internet_download)
                            is Message.Failed -> getString(R.string.error_generic)
                        }
                        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
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
