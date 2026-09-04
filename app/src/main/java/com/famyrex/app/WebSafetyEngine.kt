package com.famyrex.app

import android.net.Uri

object WebSafetyEngine {
    fun decide(url: String, settings: WebSafetySettings): WebSafetyDecision {
        if (!settings.enabled) return WebSafetyDecision(WebSafetyAction.ALLOW, "Protección web desactivada.")

        val host = runCatching { Uri.parse(url).host?.lowercase()?.removePrefix("www.") }.getOrNull()
            ?: return WebSafetyDecision(WebSafetyAction.WARN, "No se ha podido identificar el dominio.")

        fun matches(rule: String): Boolean =
            host == rule || host.endsWith(".$rule")

        if (settings.allowedDomains.any(::matches)) {
            return WebSafetyDecision(WebSafetyAction.ALLOW, "Dominio incluido en la lista permitida.")
        }

        if (settings.blockedDomains.any(::matches)) {
            return WebSafetyDecision(WebSafetyAction.BLOCK, "Dominio incluido en la lista bloqueada.")
        }

        // Famyrex does not pretend to classify the entire web locally.
        // Known malware/phishing threats are delegated to WebView Safe Browsing.
        return WebSafetyDecision(WebSafetyAction.ALLOW, "Sin coincidencia en las listas locales.")
    }
}
