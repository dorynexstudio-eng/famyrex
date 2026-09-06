package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyAgreementTest {
    private val agreement = FamilyAgreement(
        childProfileId = "child-1",
        dailyMinutes = 120,
        goal = "Mantener un uso equilibrado",
        consequence = "Hablarlo juntos y aplicar lo pactado",
        reviewDate = "2026-09-30"
    )

    @Test fun noAgreementMeansInsufficientData() {
        assertEquals(AgreementState.INSUFFICIENT_DATA, FamilyAgreementEngine.evaluate(null, 30).state)
    }

    @Test fun missingUsageMeansInsufficientData() {
        assertEquals(AgreementState.INSUFFICIENT_DATA, FamilyAgreementEngine.evaluate(agreement, null).state)
    }

    @Test fun normalUsageIsOnTrack() {
        val result = FamilyAgreementEngine.evaluate(agreement, 60)
        assertEquals(AgreementState.ON_TRACK, result.state)
        assertEquals(60L, result.remainingMinutes)
    }

    @Test fun approachingLimitNeedsAttention() {
        assertEquals(AgreementState.ATTENTION, FamilyAgreementEngine.evaluate(agreement, 105).state)
    }

    @Test fun exceededLimitIsExplicit() {
        val result = FamilyAgreementEngine.evaluate(agreement, 121)
        assertEquals(AgreementState.EXCEEDED, result.state)
        assertEquals(0L, result.remainingMinutes)
    }
}
