package com.josias.depthxdiag;
import org.junit.Test;
import static org.junit.Assert.*;
public class CandidatePolicyTest {
    @Test public void onlyAccessibleSettingsPagesAreOffered() {
        String pkg="com.android.settings", page="com.android.settings.Settings$WallpaperSettingsActivity";
        assertTrue(CandidatePolicy.canOffer(pkg,page,true,true,true));
        assertFalse(CandidatePolicy.canOffer(pkg,page,true,false,true));
        assertFalse(CandidatePolicy.canOffer(pkg,page,false,true,true));
        assertFalse(CandidatePolicy.canOffer(pkg,page,true,true,false));
    }
    @Test public void destructiveAndAuthenticationPagesAreNeverOffered() {
        for(String name:new String[]{"WallpaperResetSettings","WallpaperApplyPreview","WallpaperClearSettings","LockscreenPasswordSettings","KeyguardUnlockSettings","WallpaperFactoryResetActivity"})
            assertFalse(name,CandidatePolicy.canOffer("com.transsion.theme","com.transsion.theme."+name,true,true,true));
    }
    @Test public void namesAloneDoNotProveDepthSupportOrAccessibility() {
        assertTrue(CandidatePolicy.relevant("com.transsion.launcher","example.DepthWallpaperSettings"));
        assertFalse(CandidatePolicy.canOffer("com.transsion.launcher","example.DepthWallpaperSettings",true,false,true));
        assertFalse(CandidatePolicy.relevant("com.example.game","example.ThemeWallpaperSettings"));
    }
}
