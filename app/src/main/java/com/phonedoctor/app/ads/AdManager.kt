package com.phonedoctor.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.phonedoctor.app.BuildConfig

/**
 * Thin wrapper around Google Mobile Ads. Ad unit IDs come from BuildConfig,
 * which resolves to Google's public test IDs unless real IDs are supplied via
 * secrets.properties / CI secrets for a release build (see app/build.gradle.kts).
 */
object AdManager {

    private var initialized = false
    private var rewardedAd: RewardedAd? = null

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context.applicationContext) {}
    }

    fun loadRewardedAd(context: Context, onLoaded: (Boolean) -> Unit) {
        RewardedAd.load(
            context,
            BuildConfig.REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    onLoaded(true)
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    rewardedAd = null
                    onLoaded(false)
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onDismissed: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                onDismissed()
            }
        }
        ad.show(activity) { onRewardEarned() }
    }
}
