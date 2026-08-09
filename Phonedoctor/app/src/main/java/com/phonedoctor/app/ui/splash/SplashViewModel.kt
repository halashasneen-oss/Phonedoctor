package com.phonedoctor.app.ui.splash

import androidx.lifecycle.ViewModel
import com.phonedoctor.app.ServiceLocator
import kotlinx.coroutines.flow.first

class SplashViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    suspend fun isOnboardingCompleted(): Boolean {
        return serviceLocator.settingsRepository.settings.first().onboardingCompleted
    }
}
