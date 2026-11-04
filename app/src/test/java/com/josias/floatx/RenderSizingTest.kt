package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class RenderSizingTest {
    @Test fun smallAndWideWindowsKeepACompletePhoneLayout() {
        for((w,h) in listOf(180 to 116,220 to 400,720 to 140,400 to 600,1080 to 1600)) {
            val size=RenderSizing.forWindow(w,h,320)
            assertEquals(w*2,size.width);assertEquals(h*2,size.height)
            assertTrue(size.width*160/size.dpi>=320)
            assertTrue(size.height*160/size.dpi>=240)
            assertTrue(size.dpi<=640)
        }
    }
    @Test fun zeroLayoutDuringAttachmentIsSafe() {
        val size=RenderSizing.forWindow(0,0,320)
        assertTrue(size.width>0 && size.height>0 && size.dpi>0)
    }
    @Test fun normalWindowRetainsDeviceScale() {
        assertEquals(RenderSize(1440,2400,640),RenderSizing.forWindow(720,1200,320))
    }
}
