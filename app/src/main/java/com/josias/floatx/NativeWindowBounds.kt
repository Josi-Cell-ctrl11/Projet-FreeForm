package com.josias.floatx

/** Screen-space bounds: keep the requested app window clear of status/navigation bars. */
object NativeWindowBounds {
    fun initial(screenWidth: Int, screenHeight: Int, leftInset: Int, topInset: Int,
                rightInset: Int, bottomInset: Int, density: Float, large: Boolean): WindowBox {
        val left = leftInset.coerceIn(0, (screenWidth - 1).coerceAtLeast(0))
        val top = topInset.coerceIn(0, (screenHeight - 1).coerceAtLeast(0))
        val availableWidth = (screenWidth - left - rightInset.coerceAtLeast(0)).coerceAtLeast(1)
        val availableHeight = (screenHeight - top - bottomInset.coerceAtLeast(0)).coerceAtLeast(1)
        val width = (availableWidth * if (large) .94 else .86).toInt()
            .coerceAtLeast((240 * density).toInt()).coerceAtMost(availableWidth)
        val height = (availableHeight * if (large) .82 else .64).toInt()
            .coerceAtLeast((300 * density).toInt()).coerceAtMost(availableHeight)
        return WindowBox(left + (availableWidth - width) / 2,
            top + (availableHeight - height) / 3, width, height)
    }
}
