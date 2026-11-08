package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class DisplayProfilesTest {
    @Test fun preferredProfileIsTrustedWithoutDesktopDecorations() {
        assertEquals(1290,DisplayProfiles.creationOrder.first())
        assertTrue(DisplayProfiles.trusted(DisplayProfiles.creationOrder.first()))
    }
    @Test fun allProfilesExcludeInnerSystemBars() {
        for(flags in DisplayProfiles.creationOrder) {
            assertFalse(DisplayProfiles.decorated(flags))
            assertEquals(0,flags and 1) // Never PUBLIC: avoids forced external desktop mode.
            assertEquals(DisplayProfiles.BASE,flags and DisplayProfiles.BASE)
        }
    }
    @Test fun permissionRefusalRetainsStandardFallback() {
        assertEquals(DisplayProfiles.BASE,DisplayProfiles.creationOrder.last())
        assertFalse(DisplayProfiles.trusted(DisplayProfiles.creationOrder.last()))
    }
}
