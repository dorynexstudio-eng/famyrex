package com.famyrex.app

enum class WebSafetyAction { ALLOW, BLOCK, WARN }

data class WebSafetySettings(
    val enabled: Boolean = true,
    val blockKnownThreats: Boolean = true,
    val blockedDomains: Set<String> = emptySet(),
    val allowedDomains: Set<String> = emptySet()
)

data class WebSafetyDecision(
    val action: WebSafetyAction,
    val reason: String
)
