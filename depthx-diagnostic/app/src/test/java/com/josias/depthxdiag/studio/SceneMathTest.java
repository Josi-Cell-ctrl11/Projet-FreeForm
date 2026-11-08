package com.josias.depthxdiag.studio;
import org.junit.Test;
import static org.junit.Assert.*;
public class SceneMathTest {
    @Test public void cropAlwaysCoversTheViewportEvenWhenDraggedOutsideBounds(){
        for(int[] image:new int[][]{{300,900},{1400,500},{720,1576}})for(float zoom:new float[]{1,2,4})for(float pan:new float[]{-10,0,10}){
            float[] f=SceneMath.fit(image[0],image[1],720,1576,zoom,pan,-pan);
            assertTrue(f[0]<=.01f);assertTrue(f[1]<=.01f);
            assertTrue(f[0]+f[2]>=719.99f);assertTrue(f[1]+f[3]>=1575.99f);
        }
    }
    @Test public void maskConfidenceIsBoundedAndNonFiniteValuesAreTransparent(){
        assertEquals(0,SceneMath.maskAlpha(Float.NaN));assertEquals(0,SceneMath.maskAlpha(Float.POSITIVE_INFINITY));
        assertEquals(0,SceneMath.maskAlpha(-1));assertEquals(255,SceneMath.maskAlpha(2));
        assertTrue(SceneMath.maskAlpha(.7f)>SceneMath.maskAlpha(.5f));
    }
    @Test public void sharedWallpaperSwitchesClockWithTheKeyguardState(){
        assertTrue(SceneMath.clockVisible(0,3,true,false));assertFalse(SceneMath.clockVisible(0,3,false,false));
        assertTrue(SceneMath.clockVisible(1,3,false,false));assertFalse(SceneMath.clockVisible(1,3,true,false));
        assertTrue(SceneMath.clockVisible(0,1,false,true));assertTrue(SceneMath.clockVisible(2,1,false,false));
    }
    @Test public void dedicatedLockAndHomeEnginesUseTheirOwnScreen(){
        assertTrue(SceneMath.clockVisible(0,2,false,false));assertFalse(SceneMath.clockVisible(0,1,true,false));
        assertTrue(SceneMath.clockVisible(1,1,true,false));assertFalse(SceneMath.clockVisible(1,2,false,false));
    }
}
