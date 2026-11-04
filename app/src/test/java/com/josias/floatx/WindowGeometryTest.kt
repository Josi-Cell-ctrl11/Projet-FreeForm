package com.josias.floatx

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowGeometryTest {
    @Test fun windowCannotBeLostOutsideScreenAfterDragging() {
        assertEquals(WindowBox(620,0,400,600),WindowGeometry.clamp(WindowBox(2000,-100,400,600),1020,1800,260,300))
    }
    @Test fun resizeAfterRotationStillFitsScreen() {
        assertEquals(WindowBox(0,0,800,450),WindowGeometry.clamp(WindowBox(900,1000,1200,1700),800,450,260,300))
    }
    @Test fun minimumWindowSizeDoesNotExceedSmallScreen() {
        assertEquals(WindowBox(0,0,200,200),WindowGeometry.clamp(WindowBox(-2,10,5,5),200,200,260,300))
    }
}
