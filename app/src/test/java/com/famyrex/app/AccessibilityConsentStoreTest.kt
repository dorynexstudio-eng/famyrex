package com.famyrex.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityConsentStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun consentIsNotAcceptedByDefault() {
        val store = AccessibilityConsentStore(context)
        store.clear()

        assertFalse(store.isAccepted())
    }

    @Test
    fun acceptingConsentPersistsIt() {
        val store = AccessibilityConsentStore(context)
        store.clear()

        store.accept()

        assertTrue(store.isAccepted())
    }

    @Test
    fun clearingConsentRemovesIt() {
        val store = AccessibilityConsentStore(context)
        store.accept()

        store.clear()

        assertFalse(store.isAccepted())
    }
}
