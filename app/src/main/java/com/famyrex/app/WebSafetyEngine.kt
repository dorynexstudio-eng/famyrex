package com.famyrex.app

import java.net.URI

object WebSafetyEngine {
    fun decide(url: String, settings: WebSafetySettings): WebSafetyDecision {
        if (!settings.enabled) return WebSafetyDecision(WebSafetyAction.ALLOW, "Protección web desactivada.")

        val parsed = runCatching { URI(url.trim()) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase()
        val host = parsed?.host?.lowercase()?.removeSuffix(".")
        if (parsed == null || scheme !in setOf("http", "https") || host.isNullOrBlank()) {
            return WebSafetyDecision(WebSafetyAction.WARN, "No se ha podido identificar un dominio web válido.")
        }

        fun normalizeRule(rule: String): String = rule.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .removePrefix("www.")
            .removeSuffix(".")

        fun matches(rule: String): Boolean {
            val normalized = normalizeRule(rule)
            return normalized.isNotBlank() && (host == normalized || host.endsWith(".$normalized"))
        }

        // An explicit block is the stronger local rule: an allow entry must not
        // accidentally bypass a blocked child domain.
        if (settings.blockedDomains.any(::matches)) {
            return WebSafetyDecision(WebSafetyAction.BLOCK, "Dominio incluido en la lista bloqueada.")
        }

        if (settings.allowedDomains.any(::matches)) {
            return WebSafetyDecision(WebSafetyAction.ALLOW, "Dominio incluido en la lista permitida.")
        }

        // Famyrex does not pretend to classify the entire web locally.
        // Known malware/phishing threats are delegated to WebView Safe Browsing.
        return WebSafetyDecision(WebSafetyAction.ALLOW, "Sin coincidencia en las listas locales.")
    }
}
