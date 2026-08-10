package com.screenlens.app.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.screenlens.app.R

data class OnboardingPage(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

val onboardingPages = listOf(
    OnboardingPage(R.drawable.ic_onboarding_read, R.string.onboarding_title_1, R.string.onboarding_desc_1),
    OnboardingPage(R.drawable.ic_onboarding_translate, R.string.onboarding_title_2, R.string.onboarding_desc_2),
    OnboardingPage(R.drawable.ic_onboarding_private, R.string.onboarding_title_3, R.string.onboarding_desc_3),
    OnboardingPage(R.drawable.ic_onboarding_lens, R.string.onboarding_title_4, R.string.onboarding_desc_4)
)
