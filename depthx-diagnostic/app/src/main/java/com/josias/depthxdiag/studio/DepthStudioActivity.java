package com.josias.depthxdiag.studio;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

/** Photo + live clock + editable foreground mask, without depending on OEM activity results. */
public class DepthStudioActivity extends Activity {
    private static final int PHOTO=71,EXPORT=72;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ArrayList<View> inputs=new ArrayList<>();
    private DepthEditorView editor;
    private TextView status,tool;
    private SeekBar heightSlider,topSlider;
    private Switch depthToggle,dateToggle;
    private Spinner screenTarget;
    private boolean busy,dirty;
    private final Runnable tick=new Runnable() { public void run() { if(editor!=null)editor.invalidate();main.postDelayed(this,60000-System.currentTimeMillis()%60000+20); } };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xFFF6F5FC);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(16),dp(18),dp(16),dp(24));scroll.addView(page);
        page.addView(label("DepthX",32,true));page.addView(label("Ton image. Ton horloge. Ton effet de profondeur.",14,false));
        status=label("Chargement…",14,false);page.addView(status);
        row(page,button("Choisir ma photo",this::choosePhoto),button("Plein écran",()->saveThen(()->startActivity(new Intent(this,DepthPreviewActivity.class)))));
        editor=new DepthEditorView(this);editor.setScene(DepthScene.demo());editor.changed=()->{dirty=true;syncSliders();};page.addView(editor,new LinearLayout.LayoutParams(-1,-2));
        tool=label("Horloge : glisse pour la placer ; pince pour la redimensionner.",14,false);page.addView(tool);
        row(page,button("Horloge",()->mode(DepthEditorView.CLOCK)),button("Photo",()->mode(DepthEditorView.PHOTO)));
        row(page,button("Peindre le sujet",()->mode(DepthEditorView.ADD)),button("Effacer le masque",()->mode(DepthEditorView.ERASE)));
        row(page,button("Auto portrait",this::autoMask),button("Annuler un trait",()->editor.undoMask()));
        page.addView(label("Auto est conçu pour les personnes. Pour un dessin comme Sasuke, peins les zones du sujet qui doivent passer devant l’heure. Utilise la photo originale, sans chiffres déjà imprimés.",13,false));
        slider(page,"Taille du pinceau",10,90,34,value->{editor.brushDp=value;});
        heightSlider=slider(page,"Hauteur des chiffres",10,52,29,value->{editor.scene.height=value/100f;normalizeClock();modified();});
        topSlider=slider(page,"Position verticale de l’heure",4,75,12,value->{editor.scene.top=value/100f;normalizeClock();modified();});
        row(page,button("Blanc",()->color(Color.WHITE)),button("Crème",()->color(0xFFFFE6BD)),button("Violet",()->color(0xFFE7D8FF)),button("Noir",()->color(Color.BLACK)));
        Switch depth=new Switch(this);depthToggle=depth;depth.setText("Sujet devant l’horloge");depth.setChecked(true);
        depth.setOnCheckedChangeListener((b,value)->{if(editor.scene!=null){editor.scene.depth=value;modified();}});page.addView(depth);inputs.add(depth);
        Switch date=new Switch(this);dateToggle=date;date.setText("Afficher la date");date.setChecked(true);
        date.setOnCheckedChangeListener((b,value)->{if(editor.scene!=null){editor.scene.date=value;modified();}});page.addView(date);inputs.add(date);
        page.addView(label("Où afficher l’horloge du fond animé ?",15,true));
        Spinner target=new Spinner(this);screenTarget=target;target.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Écran verrouillé","Écran d’accueil","Les deux écrans"}));
        target.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p,View v,int index,long id) { if(editor.scene!=null&&editor.scene.target!=index){editor.scene.target=index;modified();} }
            public void onNothingSelected(AdapterView<?> p){}
        });page.addView(target);inputs.add(target);
        row(page,button("Enregistrer",()->saveThen(()->status.setText("Composition enregistrée. Le fond animé utilise maintenant ces réglages."))),button("Installer le fond animé",this::install));
        page.addView(button("Exporter un aperçu PNG",this::export));
        row(page,button("Effacer toute la sélection",()->editor.clearMask()),button("Revenir à la démo",()->new AlertDialog.Builder(this).setMessage("Remplacer les modifications non enregistrées par la démo ?").setPositiveButton("Démo",(d,w)->{editor.setScene(DepthScene.demo());syncControls();dirty=true;status.setText("Démo : déplace l’horloge vers la montagne pour voir les couches.");}).setNegativeButton("Annuler",null).show()));
        page.addView(button("Accès et diagnostic XOS",()->startActivity(new Intent(this,com.josias.depthxdiag.MainActivity.class))));
        page.addView(label("Le fond animé dessine notre propre horloge. XOS peut conserver son horloge système au-dessus : DepthX ne la masque pas et ne remplace pas ton code de verrouillage.",13,false));
        page.addView(label("Inspiration étudiée : Atmo Engine (GitHub), sans reprise de son code. Détourage automatique : modèle ML Kit intégré ; aucun envoi de tes images par DepthX.",12,false));
        page.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(dp(16)+bars.left,dp(18)+bars.top,dp(16)+bars.right,dp(24)+bars.bottom);return insets;});
        setContentView(scroll);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::leave);
        setBusy(true,"Chargement de la composition…");
        io.execute(()->{
            try {DepthScene s=SceneStore.load(this);main.post(()->{if(!isDestroyed()){editor.setScene(s);syncControls();dirty=false;setBusy(false,s.demo?"Démo prête. Choisis ta photo pour commencer.":"Composition chargée. Choisis un outil sous l’aperçu.");}});}
            catch(Exception e){main.post(()->{if(!isDestroyed())setBusy(false,"La composition précédente est indisponible. La démo reste disponible.");});}
        });
    }
    private void leave(){
        if(busy){toast("Attends la fin de l’opération en cours.");return;}
        if(!dirty){finish();return;}
        new AlertDialog.Builder(this).setTitle("Enregistrer tes modifications ?")
            .setPositiveButton("Enregistrer",(d,w)->saveThen(this::finish))
            .setNegativeButton("Quitter sans enregistrer",(d,w)->finish()).setNeutralButton("Rester",null).show();
    }
    @Override @Deprecated public void onBackPressed(){leave();}
    private void mode(int mode) {
        editor.mode=mode;editor.invalidate();
        tool.setText(mode==DepthEditorView.CLOCK?"Horloge : glisse pour la placer ; pince pour la redimensionner.":mode==DepthEditorView.PHOTO?"Photo : glisse pour cadrer ; pince pour zoomer.":"Peins la sélection en vert. Pince à deux doigts pour zoomer et affiner. Annuler retire le dernier trait.");
    }
    private void normalizeClock() {
        editor.scene.top=SceneMath.clamp(editor.scene.top,.04f,.96f-editor.scene.height,.12f);
        editor.scene.centerX=SceneMath.clamp(editor.scene.centerX,editor.scene.width/2,1-editor.scene.width/2,.5f);
    }
    private void color(int value) {editor.scene.color=value;modified();}
    private void syncSliders(){
        if(editor==null||editor.scene==null)return;
        if(heightSlider!=null)heightSlider.setProgress(Math.round(editor.scene.height*100));
        if(topSlider!=null)topSlider.setProgress(Math.round(editor.scene.top*100));
    }
    private void syncControls(){syncSliders();depthToggle.setChecked(editor.scene.depth);dateToggle.setChecked(editor.scene.date);screenTarget.setSelection(editor.scene.target);}
    private void modified(){dirty=true;syncSliders();editor.invalidate();}
    private void setBusy(boolean value,String message) {
        busy=value;for(View input:inputs)input.setEnabled(!value);editor.editable=!value;status.setText(message);
    }
    private void choosePhoto() {
        if(busy)return;
        try {startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),PHOTO);}
        catch(RuntimeException e){toast("Aucun sélecteur de photos disponible.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(request==PHOTO) {
            setBusy(true,"Chargement de ta photo…");
            io.execute(()->{
                try {
                    ImageDecoder.Source source=ImageDecoder.createSource(getContentResolver(),uri);
                    Bitmap photo=ImageDecoder.decodeBitmap(source,(decoder,info,s)->{
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        int w=info.getSize().getWidth(),h=info.getSize().getHeight();float ratio=Math.min(1,1600f/Math.max(w,h));
                        decoder.setTargetSize(Math.max(1,Math.round(w*ratio)),Math.max(1,Math.round(h*ratio)));
                    });
                    Bitmap mask=Bitmap.createBitmap(photo.getWidth(),photo.getHeight(),Bitmap.Config.ARGB_8888);
                    main.post(()->{if(!isDestroyed()){
                        DepthScene previous=editor.scene,next=new DepthScene(photo,mask);try{next.configure(previous.json());}catch(Exception ignored){}
                        next.zoom=1;next.panX=0;next.panY=0;next.demo=false;editor.setScene(next);dirty=true;
                        setBusy(false,"Photo reçue. Lance Auto portrait ou peins le sujet, puis place l’horloge derrière.");
                    }});
                }catch(Exception|OutOfMemoryError e){main.post(()->{if(!isDestroyed())setBusy(false,"Cette image ne peut pas être chargée. Essaie une autre photo.");});}
            });
        } else if(request==EXPORT)writeExport(uri);
    }
    private void autoMask() {
        if(busy||editor.scene==null)return;
        final DepthScene scene=editor.scene;
        setBusy(true,"Détourage automatique sur ton téléphone…");
        Segmenter segmenter=Segmentation.getClient(new SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).enableRawSizeMask().build());
        segmenter.process(InputImage.fromBitmap(scene.photo,0)).addOnSuccessListener(result->{
            if(isDestroyed()){segmenter.close();return;}
            int w=result.getWidth(),h=result.getHeight();float[] confidence=new float[w*h];
            ByteBuffer source=result.getBuffer();ByteBuffer copy=source.duplicate().order(source.order());copy.rewind();copy.asFloatBuffer().get(confidence);segmenter.close();
            io.execute(()->{
                int[] pixels=new int[w*h];int foreground=0;
                for(int i=0;i<pixels.length;i++){int alpha=SceneMath.maskAlpha(confidence[i]);pixels[i]=(alpha<<24)|0xFFFFFF;if(alpha>128)foreground++;}
                float coverage=foreground/(float)pixels.length;
                Bitmap raw=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
                Bitmap mask=Bitmap.createBitmap(scene.photo.getWidth(),scene.photo.getHeight(),Bitmap.Config.ARGB_8888);
                new Canvas(mask).drawBitmap(raw,null,new Rect(0,0,mask.getWidth(),mask.getHeight()),new Paint(Paint.FILTER_BITMAP_FLAG));
                main.post(()->{if(!isDestroyed()){
                    if(coverage<.005f||coverage>.98f){setBusy(false,"Auto n’a pas isolé un sujet fiable. Utilise Peindre le sujet et Effacer le masque.");return;}
                    editor.rememberMask();scene.mask=mask;scene.depth=true;depthToggle.setChecked(true);dirty=true;editor.invalidate();
                    setBusy(false,"Sujet détouré. Vérifie les contours et corrige au pinceau si nécessaire.");
                }});
            });
        }).addOnFailureListener(error->{segmenter.close();if(!isDestroyed())setBusy(false,"Auto indisponible pour cette image. Le pinceau reste utilisable.");});
    }
    private void saveThen(Runnable action) {
        if(busy||editor.scene==null)return;
        final DepthScene snapshot;
        try{snapshot=editor.scene.snapshot();}catch(Exception|OutOfMemoryError e){toast("Mémoire insuffisante pour enregistrer.");return;}
        setBusy(true,"Enregistrement de la photo, du sujet et de l’horloge…");
        io.execute(()->{
            try{SceneStore.save(this,snapshot);main.post(()->{if(!isDestroyed()){dirty=false;setBusy(false,"Composition enregistrée.");action.run();}});}
            catch(Exception e){main.post(()->{if(!isDestroyed())setBusy(false,"Enregistrement impossible. Tes modifications restent dans l’éditeur.");});}
        });
    }
    private void install() {
        new AlertDialog.Builder(this).setTitle("Appliquer le fond DepthX")
            .setMessage("Android va afficher l’aperçu du fond animé et ses destinations disponibles. Notre horloge fonctionne dans le fond ; l’horloge XOS peut rester visible au-dessus. Ton verrouillage sécurisé est conservé.")
            .setPositiveButton("Continuer",(d,w)->saveThen(()->{
                Intent intent=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,DepthWallpaperService.class));
                try{startActivity(intent);}catch(RuntimeException e){toast("L’aperçu direct n’est pas disponible. Ouvre les fonds animés et choisis DepthX.");}
            })).setNegativeButton("Annuler",null).show();
    }
    private void export() {
        if(busy)return;
        try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("image/png").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"DepthX-apercu.png"),EXPORT);}
        catch(RuntimeException e){toast("Aucun sélecteur de fichiers disponible.");}
    }
    private void writeExport(Uri uri) {
        final DepthScene snapshot;try{snapshot=editor.scene.snapshot();}catch(Exception|OutOfMemoryError e){toast("Mémoire insuffisante.");return;}
        Rect screen=getSystemService(WindowManager.class).getMaximumWindowMetrics().getBounds();int w=Math.min(1080,screen.width()),h=Math.round(w*screen.height()/(float)screen.width());
        setBusy(true,"Export de l’aperçu…");
        io.execute(()->{
            try(OutputStream out=getContentResolver().openOutputStream(uri)){
                if(out==null)throw new java.io.IOException();Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                new DepthRenderer().draw(new Canvas(b),w,h,snapshot,System.currentTimeMillis(),true,false);
                if(!b.compress(Bitmap.CompressFormat.PNG,100,out))throw new java.io.IOException();
                main.post(()->{if(!isDestroyed())setBusy(false,"PNG exporté avec une heure figée. Le fond animé, lui, actualise l’heure.");});
            }catch(Exception|OutOfMemoryError e){main.post(()->{if(!isDestroyed())setBusy(false,"Export impossible. La composition est conservée.");});}
        });
    }
    private SeekBar slider(LinearLayout page,String title,int min,int max,int initial,java.util.function.IntConsumer action) {
        page.addView(label(title,14,true));SeekBar bar=new SeekBar(this);bar.setMin(min);bar.setMax(max);bar.setProgress(initial);page.addView(bar);inputs.add(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int value,boolean user){if(user&&!busy)action.accept(value);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});return bar;
    }
    private void row(LinearLayout parent,Button... buttons) {
        LinearLayout row=new LinearLayout(this);for(Button button:buttons){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),dp(6),dp(3),dp(3));row.addView(button,p);}parent.addView(row);
    }
    private Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(Color.WHITE);b.setPadding(dp(8),dp(10),dp(8),dp(10));b.setMinHeight(dp(48));GradientDrawable bg=new GradientDrawable();bg.setColor(0xFF6655CD);bg.setCornerRadius(dp(14));b.setBackground(bg);b.setOnClickListener(v->{if(!busy)action.run();});inputs.add(b);return b;}
    private TextView label(String title,int size,boolean bold){TextView v=new TextView(this);v.setText(title);v.setTextSize(size);v.setTextColor(0xFF332A43);v.setPadding(0,dp(7),0,dp(7));if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onResume(){super.onResume();main.post(tick);}
    @Override public void onPause(){main.removeCallbacks(tick);super.onPause();}
    @Override public void onConfigurationChanged(android.content.res.Configuration c){super.onConfigurationChanged(c);if(editor!=null)editor.requestLayout();}
    @Override public void onDestroy(){main.removeCallbacksAndMessages(null);io.shutdown();super.onDestroy();}
}
