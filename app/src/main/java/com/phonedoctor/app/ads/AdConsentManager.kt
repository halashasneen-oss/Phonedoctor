package com.phonedoctor.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdConsentManager {

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    fun requestConsent(activity: Activity, onComplete: () -> Unit = {}) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    complete(activity.applicationContext, consentInformation)
                    onComplete()
                }
            },
            {
                complete(activity.applicationContext, consentInformation)
                onComplete()
            }
        )
    }

    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return UserMessagingPlatform.getConsentInformation(context)
            .privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptions(activity: Activity, onComplete: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            complete(activity.applicationContext, consentInformation)
            onComplete()
        }
    }

    private fun complete(context: Context, consentInformation: ConsentInformation) {
        val allowed = consentInformation.canRequestAds()
        _canRequestAds.value = allowed
        if (allowed) AdManager.initialize(context)
    }
}
