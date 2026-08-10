package com.screenlens.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.screenlens.app.BuildConfig

private const val TAG = "AdManager"

/**
 * Thin wrapper around the Google Mobile Ads SDK (AdMob). Ads are only shown in
 * release builds ([BuildConfig.SHOW_ADS]); debug builds always use Google's public
 * test ad unit IDs. Callers are responsible for never invoking [showInterstitial]
 * while a scan, capture, OCR or translation is in progress.
 */
class AdManager(private val context: Context) {

    val isEnabled: Boolean get() = BuildConfig.SHOW_ADS
    val bannerAdUnitId: String get() = BuildConfig.BANNER_AD_UNIT_ID

    private var initialized = false
    private var interstitialAd: InterstitialAd? = null

    fun initialize() {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context) { }
    }

    fun preloadInterstitial() {
        if (!isEnabled) return
        InterstitialAd.load(
            context,
            BuildConfig.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: AdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    /** Shows a preloaded interstitial if one is ready. Never call this mid-scan/OCR/translation. */
    fun showInterstitialIfAvailable(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (!isEnabled || ad == null) {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
