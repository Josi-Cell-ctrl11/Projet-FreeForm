package com.josias.floatx

/** Main-thread queue: at most one Binder resize runs, and only the latest next size survives. */
class LatestRenderResize {
    private var running:RenderSize?=null
    private var pending:RenderSize?=null
    private var applied:RenderSize?=null
    fun seed(size:RenderSize) { applied=size }
    fun discardPending() { pending=null }
    fun offer(size:RenderSize):RenderSize? { pending=size;return takeNext() }
    fun complete(succeeded:Boolean):RenderSize? {
        if(succeeded) applied=running
        running=null
        return takeNext()
    }
    private fun takeNext():RenderSize? {
        if(running!=null) return null
        val candidate=pending ?: return null
        pending=null
        if(candidate==applied) return null
        running=candidate
        return candidate
    }
}
