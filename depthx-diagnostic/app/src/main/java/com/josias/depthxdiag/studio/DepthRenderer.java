package com.josias.depthxdiag.studio;

import android.graphics.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** The same three-layer renderer serves editor, fullscreen preview, PNG export and live wallpaper. */
public final class DepthRenderer {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect bounds=new Rect();
    private final SimpleDateFormat time=new SimpleDateFormat("HHmm",Locale.ROOT);
    private final SimpleDateFormat date=new SimpleDateFormat("EEE d MMM",Locale.FRANCE);
    public void draw(Canvas c,int w,int h,DepthScene s,long now,boolean clock,boolean selection) {
        c.drawColor(0xFF080511);
        if(s==null||s.photo==null) return;
        float[] f=SceneMath.fit(s.photo.getWidth(),s.photo.getHeight(),w,h,s.zoom,s.panX,s.panY);
        RectF dst=new RectF(f[0],f[1],f[0]+f[2],f[1]+f[3]);
        c.drawBitmap(s.photo,null,dst,paint);
        if(clock) drawClock(c,w,h,s,now);
        if(s.depth && s.mask!=null) {
            int layer=c.saveLayer(0,0,w,h,null);
            c.drawBitmap(s.photo,null,dst,paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            c.drawBitmap(s.mask,null,dst,paint);paint.setXfermode(null);c.restoreToCount(layer);
        }
        if(selection && s.mask!=null) {
            int layer=c.saveLayer(0,0,w,h,null);
            paint.setColor(0x997CFFAA);c.drawRect(0,0,w,h,paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));c.drawBitmap(s.mask,null,dst,paint);
            paint.setXfermode(null);paint.setColor(Color.WHITE);c.restoreToCount(layer);
        }
    }
    private void drawClock(Canvas c,int w,int h,DepthScene s,long now) {
        time.setTimeZone(java.util.TimeZone.getDefault());date.setTimeZone(java.util.TimeZone.getDefault());
        Date current=new Date(now);String digits=time.format(current);
        float left=(s.centerX-s.width/2)*w,top=s.top*h,cw=s.width*w,ch=s.height*h;
        text.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));text.setColor(s.color);text.setTextSize(200);
        glyph(c,digits.substring(0,2),new RectF(left,top,left+cw*.435f,top+ch));
        glyph(c,digits.substring(2),new RectF(left+cw*.565f,top,left+cw,top+ch));
        float r=cw*.025f;c.drawCircle(left+cw*.5f,top+ch*.32f,r,text);c.drawCircle(left+cw*.5f,top+ch*.68f,r,text);
        if(s.date) {
            text.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));text.setTextSize(w*.044f);
            text.setTextAlign(Paint.Align.CENTER);c.drawText(date.format(current),s.centerX*w,Math.max(w*.05f,top-w*.055f),text);text.setTextAlign(Paint.Align.LEFT);
        }
    }
    private void glyph(Canvas c,String value,RectF box) {
        text.getTextBounds(value,0,value.length(),bounds);
        if(bounds.width()<1||bounds.height()<1)return;
        int save=c.save();c.translate(box.left,box.top);c.scale(box.width()/bounds.width(),box.height()/bounds.height());
        c.drawText(value,-bounds.left,-bounds.top,text);c.restoreToCount(save);
    }
}
