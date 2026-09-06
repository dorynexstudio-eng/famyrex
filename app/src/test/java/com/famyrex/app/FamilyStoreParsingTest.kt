package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyStoreParsingTest {
    @Test
    fun malformedProfilesAreSkippedWithoutLosingValidOnes() {
        val raw = """
            [
              {"id":"p1","displayName":"Ana","role":"OWNER","createdAtMs":1000,"guardianProfileIds":[]},
              {"id":"","displayName":"Corrupto","role":"OWNER","createdAtMs":1000},
              {"id":"p2","displayName":"Leo","role":"CHILD","createdAtMs":2000,"guardianProfileIds":["p1",""]}
            ]
        """.trimIndent()

        val profiles = FamilyStore.parseProfiles(raw)

        assertEquals(listOf("p1", "p2"), profiles.map { it.id })
        assertEquals(listOf("p1"), profiles[1].guardianProfileIds)
    }

    @Test
    fun malformedDevicesAreSkippedWithoutLosingValidOnes() {
        val raw = """
            [
              {"id":"d1","displayName":"Móvil","ownerProfileId":"p1","linkState":"LINKED","linkedAtMs":3000},
              {"id":"d2","displayName":"Roto","ownerProfileId":"","linkState":"LINKED"},
              {"id":"d3","displayName":"Tablet","ownerProfileId":"p1","linkState":"PENDING","linkedAtMs":0}
            ]
        """.trimIndent()

        val devices = FamilyStore.parseDevices(raw)

        assertEquals(listOf("d1", "d3"), devices.map { it.id })
        assertEquals(null, devices[1].linkedAtMs)
    }

    @Test
    fun malformedRootReturnsEmptyCollections() {
        assertTrue(FamilyStore.parseProfiles("not-json").isEmpty())
        assertTrue(FamilyStore.parseDevices("not-json").isEmpty())
        assertTrue(FamilyStore.parseProfiles(null).isEmpty())
        assertTrue(FamilyStore.parseDevices("").isEmpty())
    }

    @Test
    fun invalidAppModeFallsBackToParent() {
        assertEquals(FamyrexAppMode.PARENT, FamilyStore.parseAppMode("INVALID"))
        assertEquals(FamyrexAppMode.PARENT, FamilyStore.parseAppMode(null))
    }
}
