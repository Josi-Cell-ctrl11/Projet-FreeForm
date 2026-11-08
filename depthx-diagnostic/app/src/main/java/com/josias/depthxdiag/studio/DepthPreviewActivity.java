package com.josias.depthxdiag.studio;

import android.app.Activity;
import android.os.*;
import android.view.*;
import android.widget.Toast;
import java.util.concurrent.*;

/** A fullscreen visual preview; it never replaces or dismisses the secure keyguard. */
public class DepthPreviewActivity extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private DepthScene scene;
    private View preview;
    private final Runnable tick=new Runnable() { public void run() { if(preview!=null)preview.invalidate();main.postDelayed(this,60000-System.currentTimeMillis()%60000+20); } };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);getWindow().setDecorFitsSystemWindows(false);
        getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        DepthRenderer renderer=new DepthRenderer();
        preview=new View(this) { @Override protected void onDraw(android.graphics.Canvas c) { renderer.draw(c,getWidth(),getHeight(),scene,System.currentTimeMillis(),true,false); } };
        preview.setContentDescription("Aperçu plein écran du fond et de l’horloge DepthX. Retour pour quitter.");setContentView(preview);
        io.execute(()->{
            try { DepthScene next=SceneStore.loadPreview(this);main.post(()->{ if(!isDestroyed()) {scene=next;preview.invalidate();} }); }
            catch(Exception e) { main.post(()->{ if(!isDestroyed()) {Toast.makeText(this,"Composition indisponible",Toast.LENGTH_LONG).show();finish();} }); }
        });
    }
    @Override public void onResume() { super.onResume();main.post(tick); }
    @Override public void onPause() { main.removeCallbacks(tick);super.onPause(); }
    @Override public void onDestroy() { main.removeCallbacksAndMessages(null);io.shutdown();super.onDestroy(); }
}
