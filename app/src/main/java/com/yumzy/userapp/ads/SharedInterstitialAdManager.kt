package com.yumzy.userapp.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Shared singleton to manage a single interstitial ad instance across screens.
 * This allows pre-loading the ad on one screen and showing it on another.
 */
object SharedInterstitialAdManager {

    private var mInterstitialAd: InterstitialAd? = null
    private var isLoading: Boolean = false

    // Your Ad Unit ID
    private const val AD_UNIT_ID = "ca-app-pub-1527833190869655/6812055357"

    /**
     * Loads an interstitial ad if one is not already loaded or loading.
     * Call this from any screen where you want to pre-load the ad.
     */
    fun loadAd(context: Context) {
        // Don't load if already loaded or currently loading
        if (mInterstitialAd != null || isLoading) {
            Log.d("SharedAdManager", "Ad already loaded or loading, skipping...")
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context, AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("SharedAdManager", "Interstitial ad failed to load: ${adError.message}")
                mInterstitialAd = null
                isLoading = false
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("SharedAdManager", "Interstitial ad loaded successfully.")
                mInterstitialAd = interstitialAd
                isLoading = false
            }
        })
    }

    /**
     * Shows the loaded interstitial ad if available.
     * If no ad is ready, immediately calls onAdDismissed callback.
     * After showing, the ad is consumed and a new one is pre-loaded.
     *
     * @param activity The activity to show the ad in.
     * @param onAdDismissed Callback executed after ad is dismissed or if no ad is available.
     */
    fun showAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("SharedAdManager", "Ad dismissed by user")
                    mInterstitialAd = null
                    // Pre-load the next ad for future use
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("SharedAdManager", "Ad failed to show: ${adError.message}")
                    mInterstitialAd = null
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("SharedAdManager", "Ad shown successfully")
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            Log.d("SharedAdManager", "No ad available to show. Proceeding without ad.")
            onAdDismissed()
        }
    }

    /**
     * Check if an ad is currently loaded and ready to show
     */
    fun isAdLoaded(): Boolean = mInterstitialAd != null

    /**
     * Clear the loaded ad (use with caution, typically not needed)
     */
    fun clearAd() {
        mInterstitialAd = null
        isLoading = false
    }
}