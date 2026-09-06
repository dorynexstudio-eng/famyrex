package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityConsentStoreTest {
    private class FakePreferences : AccessibilityConsentPreferences {
        private val values = mutableMapOf<String, Boolean>()

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] ?: defaultValue
        override fun setBoolean(key: String, value: Boolean) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }

    @Test
    fun consentIsNotAcceptedByDefault() {
        val store = AccessibilityConsentStore(FakePreferences())

        assertFalse(store.isAccepted())
    }

    @Test
    fun acceptingConsentPersistsIt() {
        val store = AccessibilityConsentStore(FakePreferences())

        store.accept()

        assertTrue(store.isAccepted())
    }

    @Test
    fun clearingConsentRemovesIt() {
        val store = AccessibilityConsentStore(FakePreferences())
        store.accept()

        store.clear()

        assertFalse(store.isAccepted())
    }
}
