package com.josias.floatx

/** Profiles affect only the current virtual display; no permission is granted or changed. */
object DisplayProfiles {
    const val BASE = 1 or 2 or 8 or 256
    const val DECORATIONS = 512
    const val TRUSTED = 1024
    val creationOrder = listOf(BASE or DECORATIONS or TRUSTED, BASE or TRUSTED, BASE)
    fun trusted(flags:Int)=flags and TRUSTED != 0
    fun decorated(flags:Int)=flags and DECORATIONS != 0
}
