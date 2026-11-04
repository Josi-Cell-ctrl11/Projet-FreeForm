package com.josias.floatx

/** A host Binder and a GPU surface arrive independently; launch exactly once after both. */
class WindowStartup {
    enum class Phase { WAITING_BOTH, WAITING_HOST, WAITING_SURFACE, READY_TO_OPEN, OPENING, READY, FAILED }
    private var host = false
    private var surface = false
    private var opening = false
    private var ready = false
    private var failed = false
    val phase: Phase get() = when {
        failed -> Phase.FAILED
        ready -> Phase.READY
        opening -> Phase.OPENING
        host && surface -> Phase.READY_TO_OPEN
        host -> Phase.WAITING_SURFACE
        surface -> Phase.WAITING_HOST
        else -> Phase.WAITING_BOTH
    }
    fun hostConnected() { if (!failed) host = true }
    fun surfaceAvailable() { if (!failed) surface = true }
    fun beginOpening(): Boolean {
        if (!host || !surface || opening || ready || failed) return false
        opening = true
        return true
    }
    fun complete() { if (opening && !failed) ready = true }
    fun fail() { failed = true }
    fun timeout(): Phase? {
        if (ready || failed) return null
        val stalled = phase
        failed = true
        return stalled
    }
}
