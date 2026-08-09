package com.phonedoctor.app.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentOnboardingBinding
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private val binding by viewBinding(FragmentOnboardingBinding::bind)

    private val pages = listOf(
        OnboardingPage(R.drawable.ic_scan, R.string.onboarding_page1_title, R.string.onboarding_page1_desc),
        OnboardingPage(R.drawable.ic_sensors, R.string.onboarding_page2_title, R.string.onboarding_page2_desc),
        OnboardingPage(R.drawable.ic_privacy, R.string.onboarding_page3_title, R.string.onboarding_page3_desc)
    )

    private lateinit var dots: List<View>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter = OnboardingPagerAdapter(pages)
        setUpDots()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                val isLastPage = position == pages.size - 1
                binding.buttonNext.setText(if (isLastPage) R.string.common_get_started else R.string.common_next)
                binding.buttonSkip.visibility = if (isLastPage) View.INVISIBLE else View.VISIBLE
            }
        })

        binding.buttonNext.setOnClickListener {
            val next = binding.viewPager.currentItem + 1
            if (next < pages.size) {
                binding.viewPager.currentItem = next
            } else {
                completeOnboarding()
            }
        }

        binding.buttonSkip.setOnClickListener { completeOnboarding() }
    }

    private fun setUpDots() {
        val inflater = LayoutInflater.from(requireContext())
        dots = pages.indices.map { index ->
            val dot = inflater.inflate(R.layout.item_onboarding_dot, binding.dotsContainer, false)
            binding.dotsContainer.addView(dot)
            dot
        }
        updateDots(0)
    }

    private fun updateDots(selectedIndex: Int) {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(if (index == selectedIndex) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            serviceLocator().settingsRepository.setOnboardingCompleted(true)
            findNavController().navigate(R.id.action_onboarding_to_home)
        }
    }
}
