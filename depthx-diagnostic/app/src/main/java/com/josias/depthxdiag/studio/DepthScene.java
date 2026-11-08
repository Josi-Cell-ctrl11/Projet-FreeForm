package com.josias.depthxdiag.studio;

import android.graphics.*;
import org.json.JSONObject;

public final class DepthScene {
    public Bitmap photo,mask;
    public float centerX=.5f,top=.12f,width=.82f,height=.29f,zoom=1,panX=0,panY=0;
    public int color=Color.WHITE,target=0;
    public boolean depth=true,date=true,demo=false;
    public DepthScene(Bitmap photo,Bitmap mask) { this.photo=photo;this.mask=mask; }
    public JSONObject json() throws org.json.JSONException {
        JSONObject o=new JSONObject();o.put("centerX",centerX);o.put("top",top);o.put("width",width);o.put("height",height);
        o.put("zoom",zoom);o.put("panX",panX);o.put("panY",panY);o.put("color",color);o.put("target",target);
        o.put("depth",depth);o.put("date",date);o.put("demo",demo);return o;
    }
    public void configure(JSONObject o) {
        width=SceneMath.clamp((float)o.optDouble("width",.82),.25f,.96f,.82f);
        height=SceneMath.clamp((float)o.optDouble("height",.29),.10f,.52f,.29f);
        centerX=SceneMath.clamp((float)o.optDouble("centerX",.5),width/2,1-width/2,.5f);
        top=SceneMath.clamp((float)o.optDouble("top",.12),.04f,.96f-height,.12f);
        zoom=SceneMath.clamp((float)o.optDouble("zoom",1),1,4,1);
        panX=SceneMath.clamp((float)o.optDouble("panX",0),-4,4,0);panY=SceneMath.clamp((float)o.optDouble("panY",0),-4,4,0);
        color=o.optInt("color",Color.WHITE);target=o.optInt("target",0);if(target<0||target>2)target=0;
        depth=o.optBoolean("depth",true);date=o.optBoolean("date",true);demo=o.optBoolean("demo",false);
    }
    public DepthScene snapshot() throws org.json.JSONException {
        DepthScene copy=new DepthScene(photo.copy(Bitmap.Config.ARGB_8888,false),mask.copy(Bitmap.Config.ARGB_8888,false));copy.configure(json());return copy;
    }
    public static DepthScene demo() {
        int w=720,h=1576;Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(image);Paint p=new Paint(3);
        p.setShader(new LinearGradient(0,0,w,h,new int[]{0xFF04020E,0xFF211064,0xFF080311},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        p.setColor(0xFF6646CE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(16);c.drawCircle(w*.5f,h*.15f,w*.85f,p);p.setStyle(Paint.Style.FILL);
        Path peak=new Path();peak.moveTo(-100,h);peak.lineTo(w*.25f,h*.48f);peak.lineTo(w*.40f,h*.55f);peak.lineTo(w*.57f,h*.33f);peak.lineTo(w*.75f,h*.50f);peak.lineTo(w+100,h);peak.close();
        p.setShader(new LinearGradient(0,h*.3f,w,h,0xFF4C3584,0xFF090516,Shader.TileMode.CLAMP));c.drawPath(peak,p);p.setShader(null);
        Bitmap mask=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas m=new Canvas(mask);p.setColor(Color.WHITE);m.drawPath(peak,p);
        DepthScene scene=new DepthScene(image,mask);scene.demo=true;scene.height=.33f;return scene;
    }
}
