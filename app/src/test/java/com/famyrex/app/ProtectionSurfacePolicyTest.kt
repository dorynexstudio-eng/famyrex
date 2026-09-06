package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionSurfacePolicyTest {
    @Test
    fun `famyrex ui is never evaluated`() {
        assertFalse(ProtectionSurfacePolicy.shouldEvaluate("com.famyrex.app", "com.famyrex.app", "com.launcher"))
    }

    @Test
    fun `launcher is never evaluated`() {
        assertFalse(ProtectionSurfacePolicy.shouldEvaluate("com.launcher", "com.famyrex.app", "com.launcher"))
    }

    @Test
    fun `android settings are never evaluated`() {
        assertFalse(ProtectionSurfacePolicy.shouldEvaluate("com.android.settings", "com.famyrex.app", "com.launcher"))
        assertFalse(ProtectionSurfacePolicy.shouldEvaluate("com.google.android.settings", "com.famyrex.app", "com.launcher"))
    }

    @Test
    fun `ordinary app is evaluated`() {
        assertTrue(ProtectionSurfacePolicy.shouldEvaluate("com.example.childapp", "com.famyrex.app", "com.launcher"))
    }

    @Test
    fun `unknown launcher does not disable ordinary app evaluation`() {
        assertTrue(ProtectionSurfacePolicy.shouldEvaluate("com.example.childapp", "com.famyrex.app", null))
    }

    @Test
    fun `blank package is ignored`() {
        assertFalse(ProtectionSurfacePolicy.shouldEvaluate("", "com.famyrex.app", "com.launcher"))
    }
}
