package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WebSafetyEngineTest {
    private val base = WebSafetySettings(
        enabled = true,
        blockedDomains = setOf("example.com"),
        allowedDomains = emptySet()
    )

    @Test
    fun blockedDomainAlsoBlocksSubdomains() {
        val result = WebSafetyEngine.decide("https://sub.example.com/path", base)
        assertEquals(WebSafetyAction.BLOCK, result.action)
    }

    @Test
    fun similarLookingDomainIsNotBlockedBySuffixCollision() {
        val result = WebSafetyEngine.decide("https://example.com.evil.test", base)
        assertEquals(WebSafetyAction.ALLOW, result.action)
    }

    @Test
    fun allowedDomainWinsWhenNotBlocked() {
        val settings = base.copy(
            blockedDomains = emptySet(),
            allowedDomains = setOf("example.com")
        )
        val result = WebSafetyEngine.decide("https://www.example.com", settings)
        assertEquals(WebSafetyAction.ALLOW, result.action)
    }

    @Test
    fun disabledProtectionAllowsWithoutDomainParsing() {
        val result = WebSafetyEngine.decide("not-a-url", base.copy(enabled = false))
        assertEquals(WebSafetyAction.ALLOW, result.action)
    }

    @Test
    fun malformedUrlProducesWarningInsteadOfAllowing() {
        val result = WebSafetyEngine.decide("https://", base)
        assertEquals(WebSafetyAction.WARN, result.action)
    }

    @Test
    fun emptyHostProducesWarning() {
        val result = WebSafetyEngine.decide("file:///tmp/page.html", base)
        assertEquals(WebSafetyAction.WARN, result.action)
    }
}
