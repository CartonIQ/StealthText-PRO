package com.cartoniq.stealthtext

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.lang.ref.WeakReference

/**
 * Ads are optional and online-only. The app never waits for them:
 * nothing starts while offline, and every core feature works without ads or internet.
 * Order: UMP consent first, then the ads SDK, then banner and interstitial.
 */
object Ads {
    // Google sample unit IDs. Replace both with your real AdMob unit IDs before release.
    const val BANNER_ID = "ca-app-pub-9771662959477951/3668277990"
    const val INTERSTITIAL_ID = "ca-app-pub-9771662959477951/6569773503"
    private const val COOLDOWN_MS = 120_000L

    var ready by mutableStateOf(false)
    var online by mutableStateOf(false)

    private var flowRunning = false
    private var sdkStarted = false
    private var netWatching = false
    private var interstitial: InterstitialAd? = null
    private var loading = false
    private var act: WeakReference<Activity>? = null
    private val main = Handler(Looper.getMainLooper())

    fun isOnline(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(n) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    /** Safe to call often (onCreate, onResume). Does nothing while offline. */
    fun start(activity: Activity) {
        act = WeakReference(activity)
        watchNetwork(activity.applicationContext)
        online = isOnline(activity)
        if (!online || sdkStarted || flowRunning) return
        flowRunning = true
        try {
            val info = UserMessagingPlatform.getConsentInformation(activity)
            val params = ConsentRequestParameters.Builder().build()
            info.requestConsentInfoUpdate(activity, params, {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    flowRunning = false
                    if (info.canRequestAds()) initSdk(activity)
                }
            }, { error ->
                flowRunning = false
                if (info.canRequestAds()) initSdk(activity)
            })
        } catch (e: Exception) {
            flowRunning = false
        }
    }

    private fun initSdk(activity: Activity) {
        if (sdkStarted) return
        sdkStarted = true
        val app = activity.applicationContext
        Thread {
            try {
                MobileAds.initialize(app) { }
                main.post {
                    ready = true
                    loadInterstitial(app)
                }
            } catch (e: Exception) {
                sdkStarted = false
            }
        }.start()
    }

    private fun watchNetwork(ctx: Context) {
        if (netWatching) return
        netWatching = true
        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val ok = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    main.post {
                        online = ok
                        val a = act?.get()
                        if (ok && a != null) start(a)
                    }
                }

                override fun onLost(network: Network) {
                    main.post { online = false }
                }
            })
        } catch (e: Exception) {
            netWatching = false
        }
    }

    private fun loadInterstitial(ctx: Context) {
        if (!ready || loading || interstitial != null || !isOnline(ctx)) return
        loading = true
        InterstitialAd.load(ctx, INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                loading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
                loading = false
            }
        })
    }

    /** Call after a secret was hidden or revealed. Shows at most one ad every 2 minutes. */
    fun maybeShow(activity: Activity) {
        if (!ready || !isOnline(activity)) return
        val now = System.currentTimeMillis()
        val last = Prefs.lastAd(activity)
        if (last != 0L && now - last in 0 until COOLDOWN_MS) return
        val ad = interstitial
        if (ad == null) {
            loadInterstitial(activity.applicationContext)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadInterstitial(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadInterstitial(activity.applicationContext)
            }
        }
        interstitial = null
        Prefs.putLastAd(activity, now)
        ad.show(activity)
    }

    fun privacyOptionsRequired(ctx: Context): Boolean {
        return try {
            UserMessagingPlatform.getConsentInformation(ctx).privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (e: Exception) {
            false
        }
    }

    fun showPrivacyOptions(activity: Activity) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
        } catch (e: Exception) {
        }
    }
}
