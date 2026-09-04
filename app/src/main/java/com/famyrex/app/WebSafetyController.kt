package com.famyrex.app

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

object WebSafetyController {
    fun configure(webView: WebView, settings: WebSafetySettings) {
        webView.settings.apply {
            javaScriptEnabled = false
            domStorageEnabled = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                WebSettingsCompatBridge.enableSafeBrowsing(this, settings.blockKnownThreats)
            }
        }
        webView.webViewClient = SafeWebViewClient { settings }
    }

    fun isSafeBrowsingSupported(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_HIT)
}

/**
 * Small compatibility bridge so the project can keep WebView configuration isolated.
 */
private object WebSettingsCompatBridge {
    fun enableSafeBrowsing(settings: WebSettings, enabled: Boolean) {
        androidx.webkit.WebSettingsCompat.setSafeBrowsingEnabled(settings, enabled)
    }
}
