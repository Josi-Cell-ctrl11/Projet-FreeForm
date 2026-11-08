package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class DragMotionTest {
    @Test fun slowSubpixelMovesKeepTheirFullDistanceInBothDirections() {
        for(direction in listOf(-1,1)) {
            val motion=DragMotion(0);motion.begin(100f,200f)
            var x=0;var y=0
            for(step in 1..100) motion.sample(100f+direction*step*.2f,200f-direction*step*.3f)?.let {
                x+=it.first;y+=it.second
            }
            assertEquals(direction*20,x);assertEquals(-direction*30,y)
        }
    }
    @Test fun microMovementStaysATapUntilTheTouchSlopIsCrossed() {
        val motion=DragMotion(8);motion.begin(100f,200f)
        assertNull(motion.sample(104f,197f));assertFalse(motion.dragging)
        assertEquals(10 to -5,motion.sample(110f,195f));assertTrue(motion.dragging)
    }
    @Test fun finalReleasePositionAndReversalDoNotLoseDistance() {
        val motion=DragMotion(0);motion.begin(10f,10f)
        assertEquals(10 to 10,motion.sample(20f,20f))
        // The same sample method consumes the final ACTION_UP coordinates.
        assertEquals(3 to -6,motion.sample(23f,14f))
        assertEquals(-13 to -4,motion.sample(10f,10f))
        assertNull(motion.sample(10f,10f))
    }
    @Test fun aNewGestureResetsThePreviousGesture() {
        val motion=DragMotion(8);motion.begin(0f,0f);motion.sample(30f,20f)
        motion.begin(400f,500f)
        assertFalse(motion.dragging);assertNull(motion.sample(400.2f,500.1f))
    }
}
