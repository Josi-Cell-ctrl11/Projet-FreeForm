package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class LatestRenderResizeTest {
    private val a=RenderSize(600,800,320)
    private val b=RenderSize(700,900,320)
    private val c=RenderSize(800,1000,320)
    @Test fun rapidChangesKeepOnlyTheLatestSizeBehindTheRunningResize() {
        val queue=LatestRenderResize()
        assertEquals(a,queue.offer(a))
        assertNull(queue.offer(b));assertNull(queue.offer(c))
        assertEquals(c,queue.complete(true))
        assertNull(queue.complete(true))
    }
    @Test fun unchangedSizeDoesNotCauseAnotherBinderResize() {
        val queue=LatestRenderResize();queue.seed(a)
        repeat(100) { assertNull(queue.offer(a)) }
        assertEquals(b,queue.offer(b));assertNull(queue.offer(b))
        assertNull(queue.complete(true));assertNull(queue.offer(b))
    }
    @Test fun returningToTheRunningSizeDropsTheIntermediateRequest() {
        val queue=LatestRenderResize()
        assertEquals(a,queue.offer(a));assertNull(queue.offer(b));assertNull(queue.offer(a))
        assertNull(queue.complete(true))
    }
    @Test fun startingANewTouchResizeDiscardsOutdatedPendingWork() {
        val queue=LatestRenderResize();queue.offer(a);queue.offer(b)
        queue.discardPending()
        assertNull(queue.complete(true))
        assertEquals(c,queue.offer(c))
    }
    @Test fun failedResizeDoesNotCountAsAppliedOrLoopForever() {
        val queue=LatestRenderResize();queue.seed(a)
        assertEquals(b,queue.offer(b));assertNull(queue.complete(false))
        assertEquals(b,queue.offer(b));assertNull(queue.complete(true))
        assertNull(queue.offer(b))
    }
}
