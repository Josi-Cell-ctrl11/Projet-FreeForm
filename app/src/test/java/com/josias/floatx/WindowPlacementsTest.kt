package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class WindowPlacementsTest {
    private val corners=listOf(WindowPlacement.TOP_LEFT,WindowPlacement.TOP_RIGHT,
        WindowPlacement.BOTTOM_LEFT,WindowPlacement.BOTTOM_RIGHT)
    @Test fun fourWindowsFitWithoutOverlapInPortraitAndLandscape() {
        for((w,h) in listOf(360 to 640,640 to 256,721 to 1221)) {
            val boxes=corners.map { WindowPlacements.box(it,w,h,6) }
            for(box in boxes) {
                assertTrue(box.width>0 && box.height>0)
                assertTrue(box.x>=0 && box.y>=0 && box.x+box.width<=w && box.y+box.height<=h)
            }
            for(i in boxes.indices) for(j in i+1 until boxes.size) {
                val a=boxes[i];val b=boxes[j]
                assertTrue(a.x+a.width<=b.x || b.x+b.width<=a.x || a.y+a.height<=b.y || b.y+b.height<=a.y)
            }
        }
    }
    @Test fun halvesHaveEqualHeightAndDoNotOverlap() {
        val left=WindowPlacements.box(WindowPlacement.LEFT,721,1200,6)
        val right=WindowPlacements.box(WindowPlacement.RIGHT,721,1200,6)
        assertEquals(left.y,right.y);assertEquals(left.height,right.height)
        assertTrue(left.x+left.width<right.x)
        assertTrue(kotlin.math.abs(left.width-right.width)<=1)
    }
    @Test fun centerRemainsCenteredAndInsideScreen() {
        val box=WindowPlacements.box(WindowPlacement.CENTER,360,640,6)
        assertTrue(kotlin.math.abs(box.x*2+box.width-360)<=1)
        assertTrue(kotlin.math.abs(box.y*2+box.height-640)<=1)
        assertTrue(box.width<360 && box.height<640)
    }
}
