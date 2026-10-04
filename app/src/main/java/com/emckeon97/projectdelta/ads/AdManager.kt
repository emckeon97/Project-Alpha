package com.emckeon97.projectdelta.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * AdMob singleton for Project Delta.
 *
 * Ships with Google's TEST ad unit IDs ([useTestIDs] = true). When the real
 * AdMob units are provisioned, fill in the REAL_* constants and flip
 * [useTestIDs] to false — no other code changes needed.
 */
object AdManager {

    const val useTestIDs = true

    // Google's official test ad units (safe to ship during development).
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    // TODO: replace with real AdMob ad units before release.
    const val REAL_BANNER = ""
    const val REAL_INTERSTITIAL = ""
    const val REAL_REWARDED = ""

    private val bannerID: String get() = if (useTestIDs) TEST_BANNER else REAL_BANNER
    private val interstitialID: String get() = if (useTestIDs) TEST_INTERSTITIAL else REAL_INTERSTITIAL
    private val rewardedID: String get() = if (useTestIDs) TEST_REWARDED else REAL_REWARDED

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady

    private var gameOverCount = 0
    private var lastInterstitialShownAt = 0L

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
    }

    /** Fresh banner view; host it in an AndroidView. */
    fun bannerView(context: Context): AdView =
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = bannerID
            loadAd(AdRequest.Builder().build())
        }

    fun loadInterstitial(context: Context) {
        if (interstitialAd != null) return
        InterstitialAd.load(
            context,
            interstitialID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun loadRewarded(context: Context) {
        if (rewardedAd != null) return
        RewardedAd.load(
            context,
            rewardedID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _isRewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _isRewardedReady.value = false
                }
            }
        )
    }

    /** Returns true if an interstitial was shown. Preloads the next one. */
    fun showInterstitial(activity: Activity): Boolean {
        val ad = interstitialAd ?: return false
        ad.show(activity)
        interstitialAd = null
        lastInterstitialShownAt = System.currentTimeMillis()
        loadInterstitial(activity)
        return true
    }

    /** Shows the rewarded ad; [onReward] fires only if the user earns the reward. */
    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        val ad = rewardedAd ?: return
        ad.show(activity) {
            _isRewardedReady.value = false
            rewardedAd = null
            loadRewarded(activity)
            onReward()
        }
    }

    /**
     * Call on every game over. Shows an interstitial on every 5th game over,
     * at most once per 60 seconds — frequent enough to earn, rare enough
     * to keep players.
     */
    fun gameOverOccurred(activity: Activity?) {
        gameOverCount++
        if (activity == null) return
        val now = System.currentTimeMillis()
        if (gameOverCount % 5 == 0 && now - lastInterstitialShownAt > 60_000) {
            showInterstitial(activity)
        }
    }
}
