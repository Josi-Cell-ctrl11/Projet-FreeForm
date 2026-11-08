package com.josias.depthxdiag.studio;
import android.graphics.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DepthRendererTest {
    private final int w=240,h=520;
    private Bitmap draw(DepthScene s){Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);new DepthRenderer().draw(new Canvas(out),w,h,s,1700000000000L,true,false);return out;}
    private DepthScene scene(){
        Bitmap photo=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);photo.eraseColor(Color.RED);
        DepthScene s=new DepthScene(photo,Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888));s.date=false;s.top=.1f;s.height=.4f;return s;
    }
    private int white(Bitmap b){int result=0;for(int y=0;y<h;y++)for(int x=0;x<w;x++){int c=b.getPixel(x,y);if(Color.red(c)>220&&Color.green(c)>220&&Color.blue(c)>220)result++;}return result;}
    @Test public void transparentMaskLeavesTheClockVisible(){
        DepthScene s=scene();Bitmap transparent=draw(s);assertTrue("A real clock must be drawn",white(transparent)>100);
        s.depth=false;assertEquals(white(transparent),white(draw(s)));
    }
    @Test public void foregroundRestoresThePhotoOverTheClock(){
        DepthScene s=scene();assertTrue(white(draw(s))>100);
        s.mask.eraseColor(Color.WHITE);Bitmap covered=draw(s);
        assertEquals("A fully selected photo hides the clock",0,white(covered));
        assertEquals(Color.RED,covered.getPixel(w/2,h/3));
    }
    @Test public void halfMaskHidesOnlyTheOverlappingPartOfTheClock(){
        DepthScene s=scene();Bitmap before=draw(s);
        Paint p=new Paint();p.setColor(Color.WHITE);new Canvas(s.mask).drawRect(0,0,w,h*.3f,p);
        Bitmap after=draw(s);assertTrue(white(after)>0);assertTrue(white(after)<white(before));
        for(int y=0;y<(int)(h*.3f);y++)for(int x=0;x<w;x++)assertEquals(Color.RED,after.getPixel(x,y));
    }
    @Test public void fullscreenPreviewDoesNotReplaceTheAppliedWallpaper()throws Exception{
        android.content.Context context=org.robolectric.RuntimeEnvironment.getApplication();
        DepthScene active=scene();active.top=.15f;SceneStore.save(context,active);
        String applied=SceneStore.current(context);
        DepthScene preview=scene();preview.top=.3f;SceneStore.savePreview(context,preview);
        assertEquals(applied,SceneStore.current(context));
        assertEquals(.15f,SceneStore.load(context).top,.001f);assertEquals(.3f,SceneStore.loadPreview(context).top,.001f);
    }
    @Test public void renderRealDemoForVisualInspection()throws Exception{
        Bitmap image=Bitmap.createBitmap(720,1576,Bitmap.Config.ARGB_8888);
        new DepthRenderer().draw(new Canvas(image),720,1576,DepthScene.demo(),1700000000000L,true,false);
        java.io.File file=new java.io.File("build/reports/depthx-render-preview.png");file.getParentFile().mkdirs();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}
    }
    @Test public void persistedSceneRetainsMaskAndClockSettings()throws Exception{
        android.content.Context context=org.robolectric.RuntimeEnvironment.getApplication();DepthScene s=scene();s.top=.24f;s.height=.31f;s.target=2;s.mask.setPixel(30,50,Color.WHITE);
        SceneStore.save(context,s);DepthScene restored=SceneStore.load(context);
        assertEquals(.24f,restored.top,.001f);assertEquals(.31f,restored.height,.001f);assertEquals(2,restored.target);
        assertEquals(Color.WHITE,restored.mask.getPixel(30,50));assertEquals(0,Color.alpha(restored.mask.getPixel(0,0)));
    }
}
