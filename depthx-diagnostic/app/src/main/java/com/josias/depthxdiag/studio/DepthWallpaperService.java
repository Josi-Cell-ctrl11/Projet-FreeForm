package com.josias.depthxdiag.studio;

import android.app.KeyguardManager;
import android.app.WallpaperManager;
import android.content.*;
import android.graphics.Canvas;
import android.os.*;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import java.util.concurrent.*;

/** Draw only on changes or minute boundaries, and stop scheduling while invisible. */
public class DepthWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new DepthEngine(); }
    private final class DepthEngine extends Engine {
        private final Handler main=new Handler(Looper.getMainLooper());
        private final ExecutorService io=Executors.newSingleThreadExecutor();
        private final DepthRenderer renderer=new DepthRenderer();
        private DepthScene scene;
        private boolean alive=true,registered=false;
        private int generation;
        private final Runnable tick=()->draw();
        private final SharedPreferences.OnSharedPreferenceChangeListener preference=(p,key)->{ if("scene".equals(key))reload(); };
        private final BroadcastReceiver receiver=new BroadcastReceiver() {
            @Override public void onReceive(Context c,Intent i) { if(isVisible())draw(); }
        };
        @Override public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);setTouchEventsEnabled(false);
            SceneStore.preferences(DepthWallpaperService.this).registerOnSharedPreferenceChangeListener(preference);
            IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_ON);filter.addAction(Intent.ACTION_USER_PRESENT);
            if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,filter);
            registered=true;reload();
        }
        private void reload() {
            if(!alive)return;final int token=++generation;
            io.execute(()->{
                DepthScene loaded;
                try { loaded=SceneStore.load(DepthWallpaperService.this); }
                catch(Exception e) { loaded=null; }
                DepthScene next=loaded;
                main.post(()->{ if(alive&&token==generation) { if(next!=null)scene=next;draw(); } });
            });
        }
        private void draw() {
            main.removeCallbacks(tick);if(!alive||!isVisible())return;
            SurfaceHolder holder=getSurfaceHolder();if(holder.getSurface()==null||!holder.getSurface().isValid())return;
            Canvas canvas=null;
            try {
                canvas=holder.lockCanvas();
                if(canvas!=null) {
                    int flags=Build.VERSION.SDK_INT>=34?getWallpaperFlags():0;
                    boolean locked=getSystemService(KeyguardManager.class).isKeyguardLocked();
                    boolean clock=scene!=null&&SceneMath.clockVisible(scene.target,flags,locked,isPreview());
                    renderer.draw(canvas,canvas.getWidth(),canvas.getHeight(),scene,System.currentTimeMillis(),clock,false);
                }
            } catch(RuntimeException ignored) { /* Surface can disappear between visibility and lockCanvas. */ }
            finally { if(canvas!=null)try { holder.unlockCanvasAndPost(canvas); }catch(RuntimeException ignored){} }
            main.postDelayed(tick,60000-System.currentTimeMillis()%60000+30);
        }
        @Override public void onVisibilityChanged(boolean visible) { if(visible)draw();else main.removeCallbacks(tick); }
        @Override public void onSurfaceChanged(SurfaceHolder h,int f,int w,int he) { super.onSurfaceChanged(h,f,w,he);draw(); }
        @Override public void onSurfaceDestroyed(SurfaceHolder h) { main.removeCallbacks(tick);super.onSurfaceDestroyed(h); }
        @Override public void onDestroy() {
            alive=false;generation++;main.removeCallbacksAndMessages(null);
            SceneStore.preferences(DepthWallpaperService.this).unregisterOnSharedPreferenceChangeListener(preference);
            if(registered)unregisterReceiver(receiver);io.shutdown();scene=null;super.onDestroy();
        }
    }
}
