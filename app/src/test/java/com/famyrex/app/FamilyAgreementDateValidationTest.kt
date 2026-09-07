package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyAgreementDateValidationTest {
    @Test fun acceptsRealCalendarDate() {
        assertTrue(isValidFamilyAgreementDate("2026-09-30"))
    }

    @Test fun rejectsImpossibleCalendarDate() {
        assertFalse(isValidFamilyAgreementDate("2026-02-30"))
    }

    @Test fun rejectsMalformedDate() {
        assertFalse(isValidFamilyAgreementDate("30-09-2026"))
    }
}
