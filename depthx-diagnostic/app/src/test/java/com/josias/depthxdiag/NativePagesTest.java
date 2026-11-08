package com.josias.depthxdiag;
import org.junit.Test;
import static org.junit.Assert.*;
public class NativePagesTest {
    @Test public void observedGuideIsOfferedOnlyWhenAccessible() {
        String p="com.transsion.launcher3",a="com.transsion.xlauncher.wallpaper.depth.activity.TrDepthGuideActivity";
        assertTrue(CandidatePolicy.canOffer(p,a,true,true,true));
        assertFalse(CandidatePolicy.canOffer(p,a,true,false,true));
        assertFalse(CandidatePolicy.canOffer(p,a,false,true,true));
        assertFalse(CandidatePolicy.canOffer(p,a,true,true,false));
    }
    @Test public void privateLockEditorAndUndocumentedPreviewAreNotNativeEntryPoints() {
        assertFalse(NativePages.contains("com.transsion.aiwallpaper","com.transsion.aiwallpaper.ui.edit.LockScreenEditActivity"));
        assertFalse(CandidatePolicy.canOffer("com.transsion.aiwallpaper","com.transsion.aiwallpaper.ui.edit.LockScreenEditActivity",true,false,true));
        assertFalse(NativePages.contains("com.transsion.livewallpaper.page","com.transsion.livewallpaper.preview.theme.DesktopDepthPreview"));
    }
    @Test public void exactPackageAndClassAreRequiredForTheAllowlist() {
        assertTrue(NativePages.contains("com.transsion.livewallpaper.page","com.transsion.setting.livewalllpaper.page.activity.DepthFieldWallpaperActivity"));
        assertFalse(NativePages.contains("com.example.other","com.transsion.xlauncher.wallpaper.depth.activity.TrDepthGuideActivity"));
    }
}
