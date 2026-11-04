package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class CornerResizeTest {
    private val start=WindowBox(100,120,400,500)
    private fun resize(c:ResizeCorner,dx:Int,dy:Int)=WindowGeometry.resize(start,c,dx,dy,1000,1200,180,200)
    @Test fun allFourCornersKeepOppositeCornerFixed() {
        for(c in ResizeCorner.entries) {
            val next=resize(c,30,40)
            assertEquals(if(c.left) start.x+start.width else start.x,
                if(c.left) next.x+next.width else next.x)
            assertEquals(if(c.top) start.y+start.height else start.y,
                if(c.top) next.y+next.height else next.y)
            assertEquals(if(c.left) 370 else 430,next.width)
            assertEquals(if(c.top) 460 else 540,next.height)
        }
    }
    @Test fun draggingPastMinimumDoesNotMoveTheOppositeEdge() {
        assertEquals(WindowBox(320,420,180,200),resize(ResizeCorner.TOP_LEFT,2000,2000))
        assertEquals(WindowBox(100,120,180,200),resize(ResizeCorner.BOTTOM_RIGHT,-2000,-2000))
    }
    @Test fun draggingPastScreenBoundsDoesNotShiftTheWindow() {
        assertEquals(WindowBox(0,0,500,620),resize(ResizeCorner.TOP_LEFT,-2000,-2000))
        assertEquals(WindowBox(100,120,900,1080),resize(ResizeCorner.BOTTOM_RIGHT,2000,2000))
    }
}
