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
 * A helper class to manage loading and showing interstitial ads.
 * This makes the ad logic reusable and keeps the UI code cleaner.
 */
class InterstitialAdManager(private val context: Context) {

    private var mInterstitialAd: InterstitialAd? = null

    // This is your Ad Unit ID. You can replace it if needed.
    private val adUnitId: String = "ca-app-pub-1527833190869655/8094999825"

    /**
     * Loads an interstitial ad. It's best to call this ahead of time
     * so the ad is ready when you need to show it.
     */
    fun loadAd() {
        if (mInterstitialAd != null) return // Don't load if one is already loaded

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("AdManager", "Interstitial ad failed to load: ${adError.message}")
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("AdManager", "Interstitial ad loaded successfully.")
                mInterstitialAd = interstitialAd
            }
        })
    }

    /**
     * Shows the loaded interstitial ad. If an ad is not ready, it will
     * immediately call the onAdDismissed callback.
     * @param activity The activity to show the ad in.
     * @param onAdDismissed A callback function to be executed after the ad is dismissed or fails to show.
     */
    fun showAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when the ad is dismissed.
                    // Preload the next ad for the next time.
                    mInterstitialAd = null
                    loadAd()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called if the ad fails to show.
                    Log.d("AdManager", "Ad failed to show: ${adError.message}")
                    mInterstitialAd = null
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when the ad is shown successfully.
                    Log.d("AdManager", "Ad shown successfully.")
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            Log.d("AdManager", "Ad was not loaded yet. Proceeding with action.")
            // If the ad isn't loaded, just execute the action.
            onAdDismissed()
        }
    }
}
