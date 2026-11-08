package com.josias.floatx

/** Profiles affect only the current virtual display; no permission is granted or changed. */
object DisplayProfiles {
    // A private display is excluded from forced external desktop mode (bars and local IME).
    const val BASE = 2 or 8 or 256
    const val DECORATIONS = 512
    const val TRUSTED = 1024
    val creationOrder = listOf(BASE or TRUSTED, BASE)
    fun trusted(flags:Int)=flags and TRUSTED != 0
    fun decorated(flags:Int)=flags and DECORATIONS != 0
}
