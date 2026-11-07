package com.josias.floatx

enum class WindowPlacement { LEFT, RIGHT, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

object WindowPlacements {
    fun box(mode:WindowPlacement,width:Int,height:Int,gap:Int):WindowBox {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1)
        if(w<4 || h<4) return WindowBox(0,0,w,h)
        val g=gap.coerceIn(0,minOf(w/8,h/8))
        val innerW=w-2*g;val innerH=h-2*g
        val leftW=(innerW-g)/2;val rightW=innerW-g-leftW
        val topH=(innerH-g)/2;val bottomH=innerH-g-topH
        return when(mode) {
            WindowPlacement.LEFT -> WindowBox(g,g,leftW,innerH)
            WindowPlacement.RIGHT -> WindowBox(g+leftW+g,g,rightW,innerH)
            WindowPlacement.CENTER -> {
                val cw=(innerW*.78).toInt().coerceAtLeast(1)
                val ch=(innerH*.72).toInt().coerceAtLeast(1)
                WindowBox((w-cw)/2,(h-ch)/2,cw,ch)
            }
            WindowPlacement.TOP_LEFT -> WindowBox(g,g,leftW,topH)
            WindowPlacement.TOP_RIGHT -> WindowBox(g+leftW+g,g,rightW,topH)
            WindowPlacement.BOTTOM_LEFT -> WindowBox(g,g+topH+g,leftW,bottomH)
            WindowPlacement.BOTTOM_RIGHT -> WindowBox(g+leftW+g,g+topH+g,rightW,bottomH)
        }
    }
}
