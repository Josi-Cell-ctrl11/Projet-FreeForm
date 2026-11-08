package com.josias.floatx

enum class AppPresence { VISIBLE, EMPTY, UNKNOWN }

/** A transition or an unreadable task list must never reset a live app. */
class RootRecovery {
    private var emptySamples=0
    private var armed=false
    fun observe(presence:AppPresence):Boolean {
        if(presence!=AppPresence.EMPTY) { emptySamples=0;armed=presence==AppPresence.VISIBLE;return false }
        if(!armed) return false
        emptySamples++
        if(emptySamples<2) return false
        emptySamples=0;armed=false
        return true
    }
    fun reset() { emptySamples=0;armed=false }
}
