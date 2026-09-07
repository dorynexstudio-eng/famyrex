package com.famyrex.app

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class FamilyAgreementDateValidationTest {
    private val store = FamilyAgreementStore(mock(Context::class.java))

    @Test fun acceptsRealCalendarDate() {
        assertTrue(store.isValidDate("2026-09-30"))
    }

    @Test fun rejectsImpossibleCalendarDate() {
        assertFalse(store.isValidDate("2026-02-30"))
    }

    @Test fun rejectsMalformedDate() {
        assertFalse(store.isValidDate("30-09-2026"))
    }
}
