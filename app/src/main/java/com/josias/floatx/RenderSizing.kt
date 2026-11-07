package com.josias.floatx

import kotlin.math.min

data class RenderSize(val width:Int,val height:Int,val dpi:Int)

/** Keep the entire app in the surface, with at least a compact phone layout in small windows. */
object RenderSizing {
    fun forWindow(width:Int,height:Int,deviceDpi:Int):RenderSize {
        val w=width.coerceAtLeast(1)*2
        val h=height.coerceAtLeast(1)*2
        val dpi=min((deviceDpi*2).coerceAtMost(640),min(w*160/320,h*160/240)).coerceAtLeast(1)
        return RenderSize(w,h,dpi)
    }
}
