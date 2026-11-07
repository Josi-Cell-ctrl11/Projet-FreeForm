package com.josias.floatx

data class WindowBox(val x: Int, val y: Int, val width: Int, val height: Int)

enum class ResizeCorner(val left:Boolean,val top:Boolean) {
    TOP_LEFT(true,true), TOP_RIGHT(false,true), BOTTOM_LEFT(true,false), BOTTOM_RIGHT(false,false)
}

object WindowGeometry {
    /** Clamp the dragged edges while keeping the opposite corner anchored. */
    fun resize(box:WindowBox,corner:ResizeCorner,dx:Int,dy:Int,screenWidth:Int,screenHeight:Int,
               minWidth:Int,minHeight:Int):WindowBox {
        val right=box.x+box.width
        val bottom=box.y+box.height
        val left=if(corner.left) (box.x+dx).coerceIn(0,(right-minWidth.coerceAtMost(box.width)).coerceAtLeast(0)) else box.x
        val top=if(corner.top) (box.y+dy).coerceIn(0,(bottom-minHeight.coerceAtMost(box.height)).coerceAtLeast(0)) else box.y
        val end=if(!corner.left) (right+dx).coerceIn((left+minWidth).coerceAtMost(screenWidth),screenWidth) else right
        val base=if(!corner.top) (bottom+dy).coerceIn((top+minHeight).coerceAtMost(screenHeight),screenHeight) else bottom
        return WindowBox(left,top,end-left,base-top)
    }

    fun clamp(box: WindowBox, screenWidth: Int, screenHeight: Int, minWidth: Int, minHeight: Int): WindowBox {
        val w = box.width.coerceIn(minWidth.coerceAtMost(screenWidth), screenWidth)
        val h = box.height.coerceIn(minHeight.coerceAtMost(screenHeight), screenHeight)
        return WindowBox(box.x.coerceIn(0, screenWidth - w), box.y.coerceIn(0, screenHeight - h), w, h)
    }
}
