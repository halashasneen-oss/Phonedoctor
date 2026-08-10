package com.screenlens.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.screenlens.app.R
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.ActivityOnboardingBinding
import com.screenlens.app.ui.MainActivity
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val dots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pager.adapter = OnboardingPagerAdapter(onboardingPages)
        setupDots()

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                binding.nextButton.setText(
                    if (position == onboardingPages.lastIndex) R.string.action_get_started else R.string.action_next
                )
                binding.skipButton.visibility = if (position == onboardingPages.lastIndex) View.INVISIBLE else View.VISIBLE
            }
        })

        binding.skipButton.setOnClickListener { finishOnboarding() }
        binding.nextButton.setOnClickListener {
            val current = binding.pager.currentItem
            if (current == onboardingPages.lastIndex) {
                finishOnboarding()
            } else {
                binding.pager.currentItem = current + 1
            }
        }
    }

    private fun setupDots() {
        val dotSize = resources.getDimensionPixelSize(R.dimen.space_sm)
        onboardingPages.indices.forEach { index ->
            val dot = View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginStart = dotSize / 2
                    marginEnd = dotSize / 2
                }
                setBackgroundResource(
                    if (index == 0) R.drawable.dot_indicator_selected else R.drawable.dot_indicator_unselected
                )
            }
            dots.add(dot)
            binding.dotsContainer.addView(dot)
        }
    }

    private fun updateDots(selectedIndex: Int) {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index == selectedIndex) R.drawable.dot_indicator_selected else R.drawable.dot_indicator_unselected
            )
        }
    }

    private fun finishOnboarding() {
        lifecycleScope.launch {
            ServiceLocator.settingsDataStore.setOnboardingCompleted(true)
            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
            finish()
        }
    }
}
