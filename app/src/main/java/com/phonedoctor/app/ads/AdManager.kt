package com.phonedoctor.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.phonedoctor.app.BuildConfig

object AdManager {

    private var initialized = false
    private var interstitialLoading = false
    private var rewardedLoading = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    @Synchronized
    fun initialize(context: Context) {
        if (initialized || !BuildConfig.SHOW_ADS) return
        initialized = true
        MobileAds.initialize(context.applicationContext) {}
    }

    fun createAndLoadAdaptiveBanner(
        context: Context,
        availableWidthPx: Int,
        onLoaded: () -> Unit,
        onFailed: () -> Unit
    ): AdView? {
        if (!BuildConfig.SHOW_ADS || !AdConsentManager.canRequestAds.value) {
            onFailed()
            return null
        }

        val density = context.resources.displayMetrics.density
        val widthDp = (availableWidthPx / density).toInt().coerceAtLeast(1)
        val adView = AdView(context).apply {
            adUnitId = BuildConfig.BANNER_AD_UNIT_ID
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    widthDp
                )
            )
            adListener = object : AdListener() {
                override fun onAdLoaded() = onLoaded()
                override fun onAdFailedToLoad(adError: LoadAdError) = onFailed()
            }
        }
        adView.loadAd(AdRequest.Builder().build())
        return adView
    }

    fun loadInterstitialAd(context: Context) {
        if (
            !BuildConfig.SHOW_ADS ||
            !AdConsentManager.canRequestAds.value ||
            BuildConfig.INTERSTITIAL_AD_UNIT_ID.isBlank()
        ) {
            return
        }
        if (interstitialAd != null || interstitialLoading) return

        interstitialLoading = true
        InterstitialAd.load(
            context,
            BuildConfig.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onDismissed: () -> Unit) {
        if (!BuildConfig.SHOW_ADS || !AdConsentManager.canRequestAds.value) {
            interstitialAd = null
            onDismissed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onDismissed()
            }
        }
        ad.show(activity)
    }

    fun loadRewardedAd(context: Context, onLoaded: (Boolean) -> Unit) {
        if (
            !BuildConfig.SHOW_ADS ||
            !AdConsentManager.canRequestAds.value ||
            BuildConfig.REWARDED_AD_UNIT_ID.isBlank()
        ) {
            onLoaded(false)
            return
        }
        if (rewardedAd != null) {
            onLoaded(true)
            return
        }
        if (rewardedLoading) {
            onLoaded(false)
            return
        }

        rewardedLoading = true
        RewardedAd.load(
            context,
            BuildConfig.REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewardedAd = ad
                    onLoaded(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    rewardedAd = null
                    onLoaded(false)
                }
            }
        )
    }

    @Synchronized
    fun clearCachedAds() {
        interstitialAd = null
        rewardedAd = null
        interstitialLoading = false
        rewardedLoading = false
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissed: () -> Unit
    ) {
        if (!BuildConfig.SHOW_ADS || !AdConsentManager.canRequestAds.value) {
            rewardedAd = null
            onDismissed()
            return
        }

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
        ad.show(activity) {
            onRewardEarned()
        }
    }
}
