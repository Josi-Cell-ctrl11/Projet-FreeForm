package com.josias.depthxdiag.studio;

/** Device-independent geometry shared by the editor and wallpaper renderer. */
public final class SceneMath {
    private SceneMath() {}
    public static float clamp(float value,float min,float max,float fallback) {
        return Float.isFinite(value)?Math.max(min,Math.min(max,value)):fallback;
    }
    public static int maskAlpha(float confidence) {
        if(!Float.isFinite(confidence)) return 0;
        return Math.round(clamp((confidence-.4f)/.4f,0,1,0)*255);
    }
    public static float[] fit(int iw,int ih,int width,int height,float zoom,float panX,float panY) {
        float scale=Math.max(width/(float)Math.max(1,iw),height/(float)Math.max(1,ih))*clamp(zoom,1,4,1);
        float w=iw*scale,h=ih*scale;
        float x=clamp((width-w)/2+panX*width,width-w,0,(width-w)/2);
        float y=clamp((height-h)/2+panY*height,height-h,0,(height-h)/2);
        return new float[]{x,y,w,h,scale};
    }
    /** target: 0=lock, 1=home, 2=both. Android wallpaper flags: SYSTEM=1, LOCK=2. */
    public static boolean clockVisible(int target,int flags,boolean locked,boolean preview) {
        if(preview||target==2) return true;
        boolean onLock=flags==2 || (flags!=1 && locked);
        return target==0?onLock:!onLock;
    }
}
