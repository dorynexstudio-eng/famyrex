package com.famyrex.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingCodeStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun currentRejectsPersistedCodeWithInvalidFormat() {
        val prefs = context.getSharedPreferences("famyrex_pairing", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("code", "1234567")
            .putLong("created", 1_000L)
            .putLong("expires", 10_000L)
            .commit()

        val store = PairingCodeStore(context)

        assertNull(store.current(now = 2_000L))
        assertEquals(null, prefs.getString("code", null))
    }
}
