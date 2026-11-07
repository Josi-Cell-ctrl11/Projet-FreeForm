package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class TileGlyphTest {
    @Test fun opaqueWhiteBackgroundDoesNotHideDarkLogoUnderSystemTint() {
        val image=IntArray(9) { -1 };image[4]=0xFF101010.toInt()
        val mask=TileGlyph.mask(image,3,3)
        assertEquals(0,mask[0] ushr 24)
        assertEquals(255,mask[4] ushr 24)
    }
    @Test fun coloredBackgroundAndWhiteGlyphProduceReadableShape() {
        val image=IntArray(9) { 0xFF159A58.toInt() };image[4]=-1
        val mask=TileGlyph.mask(image,3,3)
        assertEquals(0,mask[0] ushr 24)
        assertEquals(-1,mask[4])
    }
    @Test fun transparentAndAntialiasedAdaptiveGlyphKeepsItsAlpha() {
        val image=IntArray(9);image[4]=0x80123456.toInt()
        val mask=TileGlyph.mask(image,3,3)
        assertEquals(0,mask[0] ushr 24)
        assertEquals(128,mask[4] ushr 24)
    }
}
