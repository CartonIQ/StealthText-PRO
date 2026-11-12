package com.cartoniq.stealthtext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Banner that only exists while ads are ready and the phone is online. Offline, nothing is shown and no space is used. */
@Composable
fun AdBanner() {
    if (!Ads.ready || !Ads.online) return
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { c ->
                AdView(c).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = Ads.BANNER_ID
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() }
        )
    }
}
