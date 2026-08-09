package com.phonedoctor.app.ui.about

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.BuildConfig
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentAboutBinding
import com.phonedoctor.app.ui.common.viewBinding

class AboutFragment : Fragment(R.layout.fragment_about) {

    private val binding by viewBinding(FragmentAboutBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.textVersion.text = getString(R.string.about_version_fmt, BuildConfig.VERSION_NAME)
    }
}
