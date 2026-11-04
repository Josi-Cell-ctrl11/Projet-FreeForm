package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class DisplayProfilesTest {
    @Test fun requestedModeHasTrustedAndDecorations() {
        assertEquals(1803,DisplayProfiles.creationOrder.first())
        assertTrue(DisplayProfiles.trusted(DisplayProfiles.creationOrder.first()))
        assertTrue(DisplayProfiles.decorated(DisplayProfiles.creationOrder.first()))
    }
    @Test fun decorationRefusalCanKeepTrustedMode() {
        val fallback=DisplayProfiles.creationOrder[1]
        assertTrue(DisplayProfiles.trusted(fallback));assertFalse(DisplayProfiles.decorated(fallback))
    }
    @Test fun permissionFallbackNeverRequestsDecorationsOnUntrustedDisplay() {
        for(flags in DisplayProfiles.creationOrder) {
            assertTrue(!DisplayProfiles.decorated(flags)||DisplayProfiles.trusted(flags))
            assertEquals(DisplayProfiles.BASE,flags and DisplayProfiles.BASE)
        }
        assertEquals(DisplayProfiles.BASE,DisplayProfiles.creationOrder.last())
        assertFalse(DisplayProfiles.trusted(DisplayProfiles.creationOrder.last()))
    }
}
