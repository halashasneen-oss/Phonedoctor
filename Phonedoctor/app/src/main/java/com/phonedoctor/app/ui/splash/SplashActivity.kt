package com.phonedoctor.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.phonedoctor.app.PhoneDoctorApp
import com.phonedoctor.app.ui.MainActivity
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return SplashViewModel((application as PhoneDoctorApp).serviceLocator) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var ready = false
        var onboardingCompleted = false
        splashScreen.setKeepOnScreenCondition { !ready }

        lifecycleScope.launch {
            onboardingCompleted = viewModel.isOnboardingCompleted()
            ready = true
            startActivity(
                Intent(this@SplashActivity, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_START_WITH_ONBOARDING, !onboardingCompleted)
                }
            )
            finish()
        }
    }
}
