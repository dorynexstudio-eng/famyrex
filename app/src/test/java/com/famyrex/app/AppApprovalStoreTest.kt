package com.famyrex.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AppApprovalStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `approval can be granted and revoked`() {
        val store = AppApprovalStore(context)
        store.revoke("com.example.approval")

        assertTrue(store.approve("com.example.approval"))
        assertTrue(store.isApproved("com.example.approval"))
        assertTrue(store.revoke("com.example.approval"))
        assertFalse(store.isApproved("com.example.approval"))
    }

    @Test
    fun `invalid package names are never approved`() {
        val store = AppApprovalStore(context)

        assertFalse(store.approve("not a package"))
        assertFalse(store.isApproved("not a package"))
    }

    @Test
    fun `approval survives a new store instance`() {
        val packageName = "com.example.persisted"
        AppApprovalStore(context).revoke(packageName)
        assertTrue(AppApprovalStore(context).approve(packageName))

        assertTrue(AppApprovalStore(context).isApproved(packageName))
        AppApprovalStore(context).revoke(packageName)
    }
}
