package com.josias.floatx

import kotlin.math.abs

/** QS tints alpha masks, so preserve the glyph rather than an opaque launcher background. */
object TileGlyph {
    fun mask(pixels:IntArray,width:Int,height:Int):IntArray {
        require(width>0 && height>0 && pixels.size==width*height)
        fun alpha(c:Int)=c ushr 24
        fun distance(a:Int,b:Int)=maxOf(abs((a shr 16 and 255)-(b shr 16 and 255)),
            abs((a shr 8 and 255)-(b shr 8 and 255)),abs((a and 255)-(b and 255)))
        val corners=intArrayOf(pixels[0],pixels[width-1],pixels[(height-1)*width],pixels.last())
        val background=corners[0]
        val uniformBackground=corners.all { alpha(it)>=250 && distance(it,background)<=12 }
        val result=IntArray(pixels.size) { index ->
            val pixel=pixels[index]
            val a=if(uniformBackground) alpha(pixel)*distance(pixel,background).coerceAtMost(48)/48 else alpha(pixel)
            (a shl 24) or 0xFFFFFF
        }
        // A blank or extremely low-contrast source must not disappear altogether.
        if(result.none { alpha(it)>32 }) return IntArray(pixels.size) { (alpha(pixels[it]) shl 24) or 0xFFFFFF }
        return result
    }
}
