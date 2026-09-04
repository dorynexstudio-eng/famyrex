package com.famyrex.app

enum class WebSafetyAction { ALLOW, BLOCK, WARN }

enum class WebCategory {
    ADULT,
    GAMBLING,
    MALWARE,
    PHISHING,
    VIOLENCE,
    SOCIAL,
    UNKNOWN
}

data class WebSafetySettings(
    val enabled: Boolean = true,
    val blockAdult: Boolean = true,
    val blockGambling: Boolean = true,
    val blockKnownThreats: Boolean = true,
    val blockedDomains: Set<String> = emptySet(),
    val allowedDomains: Set<String> = emptySet()
)

data class WebSafetyDecision(
    val action: WebSafetyAction,
    val reason: String
)
