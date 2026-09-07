package com.famyrex.app

import java.net.URI

object WebSafetyEngine {
    /**
     * Conservative built-in protection for common adult-content domains.
     * This is an additional local safeguard, not a claim that it classifies
     * the entire web. Parents can still maintain their own block/allow rules.
     */
    private val DEFAULT_ADULT_BLOCKED_DOMAINS = setOf(
        "pornhub.com",
        "xvideos.com",
        "xnxx.com",
        "xhamster.com",
        "redtube.com",
        "youporn.com",
        "spankbang.com",
        "chaturbate.com",
        "onlyfans.com",
        "rule34.xxx",
        "nhentai.net"
    )

    fun decide(url: String, settings: WebSafetySettings): WebSafetyDecision {
        val parsed = runCatching { URI(url.trim()) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase()
        val host = parsed?.host?.lowercase()?.removeSuffix(".")
        if (parsed == null || scheme !in setOf("http", "https") || host.isNullOrBlank()) {
            return WebSafetyDecision(WebSafetyAction.WARN, "No se ha podido identificar un dominio web válido.")
        }

        if (!settings.enabled) {
            return WebSafetyDecision(WebSafetyAction.ALLOW, "Protección web desactivada.")
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

        if (settings.blockedDomains.any(::matches)) {
            return WebSafetyDecision(WebSafetyAction.BLOCK, "Dominio incluido en la lista bloqueada.")
        }

        if (settings.blockAdultContent && DEFAULT_ADULT_BLOCKED_DOMAINS.any(::matches)) {
            return WebSafetyDecision(WebSafetyAction.BLOCK, "Contenido para adultos bloqueado por la protección web de Famyrex.")
        }

        if (settings.allowedDomains.any(::matches)) {
            return WebSafetyDecision(WebSafetyAction.ALLOW, "Dominio incluido en la lista permitida.")
        }

        return WebSafetyDecision(WebSafetyAction.ALLOW, "Sin coincidencia en las listas locales.")
    }
}
