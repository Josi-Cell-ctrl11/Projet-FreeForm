package com.josias.depthxdiag.studio;

import android.content.Context;
import android.graphics.*;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Publish a new complete generation only after both images and settings are written. */
public final class SceneStore {
    private SceneStore() {}
    public static SharedPreferences preferences(Context c) { return c.getSharedPreferences("depth_studio",Context.MODE_PRIVATE); }
    public static String current(Context c) { return preferences(c).getString("scene",""); }
    public static DepthScene load(Context c) throws Exception {
        String id=current(c);if(id.isEmpty())return DepthScene.demo();
        File dir=new File(c.getFilesDir(),"scenes/"+id);
        Bitmap photo=BitmapFactory.decodeFile(new File(dir,"photo.jpg").toString());
        Bitmap decoded=BitmapFactory.decodeFile(new File(dir,"mask.png").toString());
        if(photo==null||decoded==null) throw new IOException("Composition incomplète");
        DepthScene s=new DepthScene(photo,decoded.copy(Bitmap.Config.ARGB_8888,true));
        s.configure(new JSONObject(new String(Files.readAllBytes(new File(dir,"settings.json").toPath()),StandardCharsets.UTF_8)));return s;
    }
    public static void save(Context c,DepthScene s) throws Exception {
        File root=new File(c.getFilesDir(),"scenes");if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Stockage indisponible");
        String id=UUID.randomUUID().toString();File dir=new File(root,id);if(!dir.mkdir())throw new IOException("Création impossible");
        try {
            write(s.photo,new File(dir,"photo.jpg"),Bitmap.CompressFormat.JPEG);
            write(s.mask,new File(dir,"mask.png"),Bitmap.CompressFormat.PNG);
            Files.write(new File(dir,"settings.json").toPath(),s.json().toString().getBytes(StandardCharsets.UTF_8));
            if(!preferences(c).edit().putString("scene",id).commit())throw new IOException("Enregistrement non confirmé");
        } catch(Exception e) { remove(dir);throw e; }
        File[] dirs=root.listFiles();if(dirs==null)return;
        Arrays.sort(dirs,Comparator.comparingLong(File::lastModified).reversed());
        for(int i=3;i<dirs.length;i++) if(!dirs[i].getName().equals(id))remove(dirs[i]);
    }
    private static void write(Bitmap b,File f,Bitmap.CompressFormat format) throws IOException {
        try(FileOutputStream stream=new FileOutputStream(f)) { if(!b.compress(format,95,stream))throw new IOException("Image non enregistrée");stream.getFD().sync(); }
    }
    private static void remove(File dir) { File[] files=dir.listFiles();if(files!=null)for(File f:files)f.delete();dir.delete(); }
}
