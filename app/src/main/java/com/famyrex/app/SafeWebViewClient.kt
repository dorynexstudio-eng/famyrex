package com.famyrex.app

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.Toast
import androidx.webkit.SafeBrowsingResponseCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature

class SafeWebViewClient(
    private val settingsProvider: () -> WebSafetySettings
) : WebViewClientCompat() {

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val decision = WebSafetyEngine.decide(request.url.toString(), settingsProvider())
        return when (decision.action) {
            WebSafetyAction.BLOCK -> {
                Toast.makeText(view.context, "Sitio bloqueado por Famyrex", Toast.LENGTH_LONG).show()
                true
            }
            WebSafetyAction.WARN -> {
                Toast.makeText(view.context, decision.reason, Toast.LENGTH_LONG).show()
                true
            }
            WebSafetyAction.ALLOW -> false
        }
    }

    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponseCompat
    ) {
        if (WebViewFeature.isFeatureSupported(
                WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY
            )
        ) {
            callback.backToSafety(true)
            Toast.makeText(
                view.context,
                "Famyrex ha bloqueado una página identificada como potencialmente peligrosa.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            super.onSafeBrowsingHit(view, request, threatType, callback)
        }
    }
}
