package com.josias.floatx

import kotlin.math.abs
import kotlin.math.roundToInt

/** Round total displacement, so repeated subpixel samples never lose distance. */
class DragMotion(private val slop:Int) {
    private var startX=0f
    private var startY=0f
    private var emittedX=0
    private var emittedY=0
    var dragging=false
        private set
    fun begin(x:Float,y:Float) {
        startX=x;startY=y;emittedX=0;emittedY=0;dragging=false
    }
    fun sample(x:Float,y:Float):Pair<Int,Int>? {
        val dx=x-startX;val dy=y-startY
        if(!dragging && (abs(dx)>slop || abs(dy)>slop)) dragging=true
        if(!dragging) return null
        val totalX=dx.roundToInt();val totalY=dy.roundToInt()
        val delta=(totalX-emittedX) to (totalY-emittedY)
        emittedX=totalX;emittedY=totalY
        return delta.takeUnless { it.first==0 && it.second==0 }
    }
}
