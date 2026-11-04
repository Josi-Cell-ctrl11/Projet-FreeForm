package com.josias.floatx

data class WindowBox(val x: Int, val y: Int, val width: Int, val height: Int)

object WindowGeometry {
    fun clamp(box: WindowBox, screenWidth: Int, screenHeight: Int, minWidth: Int, minHeight: Int): WindowBox {
        val w = box.width.coerceIn(minWidth.coerceAtMost(screenWidth), screenWidth)
        val h = box.height.coerceIn(minHeight.coerceAtMost(screenHeight), screenHeight)
        return WindowBox(box.x.coerceIn(0, screenWidth - w), box.y.coerceIn(0, screenHeight - h), w, h)
    }
}
