package com.josias.depthxdiag.studio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.ArrayDeque;

public final class DepthEditorView extends View {
    public static final int PHOTO=0,CLOCK=1,ADD=2,ERASE=3;
    private final DepthRenderer renderer=new DepthRenderer();
    private final ScaleGestureDetector scale;
    private final Paint brush=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayDeque<Bitmap> undo=new ArrayDeque<>();
    public DepthScene scene;
    public int mode=CLOCK;
    public boolean editable=true,selection=false;
    public float brushDp=34;
    public Runnable changed=()->{};
    private float lastX,lastY;
    private boolean multi,painted;
    public DepthEditorView(Context c) {
        super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        brush.setColor(Color.WHITE);brush.setStrokeCap(Paint.Cap.ROUND);brush.setStrokeJoin(Paint.Join.ROUND);
        scale=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                if(!editable||scene==null)return false;
                if(mode==CLOCK) {
                    scene.width=SceneMath.clamp(scene.width*detector.getScaleFactor(),.25f,.96f,.82f);
                    scene.height=SceneMath.clamp(scene.height*detector.getScaleFactor(),.10f,.52f,.29f);clampClock();
                } else { scene.zoom=SceneMath.clamp(scene.zoom*detector.getScaleFactor(),1,4,1);clampPan(); }
                dirty();return true;
            }
        });
        setContentDescription("Aperçu : déplacer l’horloge, cadrer la photo ou corriger le détourage selon l’outil choisi");
    }
    public void setScene(DepthScene s) { scene=s;undo.clear();invalidate(); }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int width=MeasureSpec.getSize(widthSpec);
        Rect screen=getContext().getSystemService(WindowManager.class).getMaximumWindowMetrics().getBounds();
        setMeasuredDimension(width,Math.max(1,Math.round(width*screen.height()/(float)screen.width())));
    }
    @Override protected void onDraw(Canvas c) {
        renderer.draw(c,getWidth(),getHeight(),scene,System.currentTimeMillis(),true,selection||mode==ADD||mode==ERASE);
    }
    public void rememberMask() { if(scene==null)return;undo.addLast(scene.mask.copy(Bitmap.Config.ARGB_8888,true));while(undo.size()>4)undo.removeFirst(); }
    public void undoMask() { if(scene!=null&&!undo.isEmpty()) { scene.mask=undo.removeLast();dirty(); } }
    public void clearMask() { if(scene!=null) { rememberMask();scene.mask.eraseColor(Color.TRANSPARENT);dirty(); } }
    private void clampClock() {
        scene.centerX=SceneMath.clamp(scene.centerX,scene.width/2,1-scene.width/2,.5f);
        scene.top=SceneMath.clamp(scene.top,.04f,.96f-scene.height,.12f);
    }
    private void clampPan() {
        if(getWidth()<1||getHeight()<1)return;
        float[] f=SceneMath.fit(scene.photo.getWidth(),scene.photo.getHeight(),getWidth(),getHeight(),scene.zoom,scene.panX,scene.panY);
        scene.panX=(f[0]-(getWidth()-f[2])/2)/getWidth();scene.panY=(f[1]-(getHeight()-f[3])/2)/getHeight();
    }
    private void dirty() { invalidate();changed.run(); }
    private void stroke(float x,float y,float px,float py) {
        float[] f=SceneMath.fit(scene.photo.getWidth(),scene.photo.getHeight(),getWidth(),getHeight(),scene.zoom,scene.panX,scene.panY);
        brush.setStrokeWidth(brushDp*getResources().getDisplayMetrics().density/f[4]);
        brush.setXfermode(mode==ERASE?new PorterDuffXfermode(PorterDuff.Mode.CLEAR):null);
        Canvas mask=new Canvas(scene.mask);mask.drawLine((px-f[0])/f[4],(py-f[1])/f[4],(x-f[0])/f[4],(y-f[1])/f[4],brush);
        mask.drawPoint((x-f[0])/f[4],(y-f[1])/f[4],brush);painted=true;dirty();
    }
    private float focusX(MotionEvent e) { float sum=0;for(int i=0;i<e.getPointerCount();i++)sum+=e.getX(i);return sum/e.getPointerCount(); }
    private float focusY(MotionEvent e) { float sum=0;for(int i=0;i<e.getPointerCount();i++)sum+=e.getY(i);return sum/e.getPointerCount(); }
    @SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(!editable||scene==null)return true;
        scale.onTouchEvent(e);
        float x=focusX(e),y=focusY(e);
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);lastX=x;lastY=y;multi=false;painted=false;
                if(mode==ADD||mode==ERASE)rememberMask();return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                multi=true;lastX=x;lastY=y;return true;
            case MotionEvent.ACTION_POINTER_UP:
                multi=true;lastX=x;lastY=y;return true;
            case MotionEvent.ACTION_MOVE:
                if(e.getPointerCount()>1) {
                    if(mode!=CLOCK) { scene.panX+=(x-lastX)/getWidth();scene.panY+=(y-lastY)/getHeight();clampPan();dirty(); }
                } else if(!multi) {
                    if(mode==PHOTO) { scene.panX+=(x-lastX)/getWidth();scene.panY+=(y-lastY)/getHeight();clampPan();dirty(); }
                    else if(mode==CLOCK) { scene.centerX+=(x-lastX)/getWidth();scene.top+=(y-lastY)/getHeight();clampClock();dirty(); }
                    else stroke(x,y,lastX,lastY);
                }
                lastX=x;lastY=y;return true;
            case MotionEvent.ACTION_UP:
                if(!multi&&!painted&&(mode==ADD||mode==ERASE))stroke(x,y,x,y);
                getParent().requestDisallowInterceptTouchEvent(false);performClick();return true;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);return true;
            default:return true;
        }
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
