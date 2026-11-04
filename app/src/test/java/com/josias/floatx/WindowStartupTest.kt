package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class WindowStartupTest {
    @Test fun hostWithoutGpuSurfaceDoesNotStartApp() {
        val startup = WindowStartup()
        startup.hostConnected()
        assertFalse(startup.beginOpening())
        assertEquals(WindowStartup.Phase.WAITING_SURFACE, startup.timeout())
    }
    @Test fun gpuWithoutHostHasDifferentDiagnostic() {
        val startup = WindowStartup()
        startup.surfaceAvailable()
        assertFalse(startup.beginOpening())
        assertEquals(WindowStartup.Phase.WAITING_HOST, startup.timeout())
    }
    @Test fun eitherCallbackOrderLaunchesExactlyOnce() {
        for (hostFirst in listOf(true, false)) {
            val startup = WindowStartup()
            if (hostFirst) startup.hostConnected() else startup.surfaceAvailable()
            assertFalse(startup.beginOpening())
            if (hostFirst) startup.surfaceAvailable() else startup.hostConnected()
            assertTrue(startup.beginOpening())
            assertFalse(startup.beginOpening())
            startup.complete()
            assertEquals(WindowStartup.Phase.READY, startup.phase)
            assertNull(startup.timeout())
        }
    }
    @Test fun timeoutPreventsLateBinderFromOpeningHiddenApp() {
        val startup = WindowStartup()
        startup.surfaceAvailable()
        startup.timeout()
        startup.hostConnected()
        assertFalse(startup.beginOpening())
        startup.complete()
        assertEquals(WindowStartup.Phase.FAILED, startup.phase)
    }
    @Test fun stoppedSessionIgnoresBothLateCallbacks() {
        val startup = WindowStartup()
        startup.fail()
        startup.hostConnected(); startup.surfaceAvailable()
        assertFalse(startup.beginOpening())
        assertNull(startup.timeout())
    }
}
