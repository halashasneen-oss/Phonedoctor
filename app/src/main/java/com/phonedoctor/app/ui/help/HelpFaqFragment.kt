package com.phonedoctor.app.ui.help

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentHelpFaqBinding
import com.phonedoctor.app.ui.common.viewBinding

class HelpFaqFragment : Fragment(R.layout.fragment_help_faq) {

    private val binding by viewBinding(FragmentHelpFaqBinding::bind)

    private val faqItems = listOf(
        FaqItem(R.string.help_q1, R.string.help_a1),
        FaqItem(R.string.help_q2, R.string.help_a2),
        FaqItem(R.string.help_q3, R.string.help_a3),
        FaqItem(R.string.help_q4, R.string.help_a4),
        FaqItem(R.string.help_q5, R.string.help_a5)
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.recyclerFaq.adapter = FaqAdapter(faqItems)
    }
}
