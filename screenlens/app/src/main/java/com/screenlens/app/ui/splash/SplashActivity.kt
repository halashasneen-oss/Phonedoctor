package com.screenlens.app.ui.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.screenlens.app.ServiceLocator
import com.screenlens.app.databinding.ActivitySplashBinding
import com.screenlens.app.ui.MainActivity
import com.screenlens.app.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Two-stage splash: the OS-level SplashScreen (icon + fade, ~450ms, themed via
 * Theme.ScreenLens.Splash) hands off to this Activity, which briefly shows the
 * full brand mark (logo + name + tagline) before routing to Onboarding or Home.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isReduceMotionEnabled()) {
            animateBrandIn()
        }

        lifecycleScope.launch {
            delay(BRAND_DISPLAY_MS)
            val onboardingCompleted = ServiceLocator.settingsDataStore.settings.first().onboardingCompleted
            val next = if (onboardingCompleted) MainActivity::class.java else OnboardingActivity::class.java
            startActivity(Intent(this@SplashActivity, next))
            finish()
        }
    }

    private fun animateBrandIn() {
        listOf(binding.logo, binding.appName, binding.tagline).forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 24f
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                duration = 320
                startDelay = index * 90L
                start()
            }
            ObjectAnimator.ofFloat(view, "translationY", 24f, 0f).apply {
                duration = 320
                startDelay = index * 90L
                start()
            }
        }
    }

    private fun isReduceMotionEnabled(): Boolean {
        val scale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return scale == 0f
    }

    private companion object {
        const val BRAND_DISPLAY_MS = 650L
    }
}
