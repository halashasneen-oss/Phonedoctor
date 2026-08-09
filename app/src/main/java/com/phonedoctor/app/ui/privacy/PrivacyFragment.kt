package com.phonedoctor.app.ui.privacy

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentPrivacyBinding
import com.phonedoctor.app.ui.common.viewBinding

class PrivacyFragment : Fragment(R.layout.fragment_privacy) {

    private val binding by viewBinding(FragmentPrivacyBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
    }
}
