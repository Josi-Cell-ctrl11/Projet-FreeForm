package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class NativeWindowBoundsTest {
    @Test fun portraitWindowFitsBetweenSystemBars() {
        val box=NativeWindowBounds.initial(720,1600,0,60,0,80,2f,false)
        assertTrue(box.x>=0);assertTrue(box.y>=60)
        assertTrue(box.x+box.width<=720);assertTrue(box.y+box.height<=1520)
        assertTrue(box.width<720);assertTrue(box.height<1460)
    }
    @Test fun largeModeProvidesLargerInitialAppWindow() {
        val normal=NativeWindowBounds.initial(1080,2400,0,80,0,100,3f,false)
        val large=NativeWindowBounds.initial(1080,2400,0,80,0,100,3f,true)
        assertTrue(large.width>normal.width);assertTrue(large.height>normal.height)
    }
    @Test fun landscapeCutoutAndMinimumSizeStayOnScreen() {
        val box=NativeWindowBounds.initial(1600,720,80,0,0,60,3f,false)
        assertTrue(box.x>=80);assertTrue(box.y>=0)
        assertTrue(box.x+box.width<=1600);assertTrue(box.y+box.height<=660)
    }
    @Test fun tinyDisplayStillHasPositiveBounds() {
        val box=NativeWindowBounds.initial(100,80,0,10,0,10,4f,true)
        assertEquals(100,box.width);assertEquals(60,box.height)
        assertEquals(0,box.x);assertEquals(10,box.y)
    }
}
