package com.josias.depthxdiag;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local inspection only: never writes settings, enables components or changes wallpaper. */
public class MainActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout candidates;
    private TextView status;
    private Button scan;
    private String report="";
    private final StringBuilder attempts=new StringBuilder();
    private LinearLayout nativePages;
    private static final int SAVE_REPORT=10;
    private static class Page {
        final String label;
        final ComponentName component;
        Page(String label,ComponentName component) { this.label=label;this.component=component; }
    }
    private static class Result {
        String report;
        int relevantPackages,found,blocked;
        final List<Page> pages=new ArrayList<>();
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setBackgroundColor(0xFFF6F5FC);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22),dp(24),dp(22),dp(28));scroll.addView(page);
        page.addView(text("DepthX",34,true,0xFF282237));
        page.addView(text("Les accès natifs à la profondeur sur ton Infinix.",16,false,0xFF696477));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(rounded(0xFFEAE5FC));
        LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2);cardParams.topMargin=dp(22);
        page.addView(card,cardParams);
        card.addView(text(Build.MANUFACTURER+" · "+Build.MODEL,18,true,0xFF3E3376));
        card.addView(text("Android "+Build.VERSION.RELEASE+" · "+Build.DISPLAY,13,false,0xFF696477));
        status=text("Recherche des menus disponibles…",15,false,0xFF3E3376);card.addView(status);
        page.addView(text("Essayer les accès XOS",20,true,0xFF282237));
        page.addView(text("Ces boutons ouvrent les menus repérés dans ton rapport. Leur contenu et l’effet derrière l’horloge restent à vérifier sur le téléphone.",14,false,0xFF696477));
        nativePages=new LinearLayout(this);nativePages.setOrientation(LinearLayout.VERTICAL);page.addView(nativePages);
        showNativePages();
        scan=button("Analyser à nouveau",this::scan);page.addView(scan);
        page.addView(button("Ouvrir les fonds d’écran",()->open(new Intent(Intent.ACTION_SET_WALLPAPER))));
        page.addView(button("Ouvrir les réglages d’affichage",()->open(new Intent(Settings.ACTION_DISPLAY_SETTINGS))));
        page.addView(button("Voir les fonds animés",()->open(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))));
        page.addView(text("Menus de personnalisation repérés",20,true,0xFF282237));
        candidates=new LinearLayout(this);candidates.setOrientation(LinearLayout.VERTICAL);page.addView(candidates);
        page.addView(text("La présence d’un menu ne prouve pas que l’effet de profondeur est disponible. Tu choisis toi-même les menus à ouvrir.",14,false,0xFF696477));
        page.addView(button("Voir le rapport",this::showReport));
        page.addView(button("Copier le rapport",()->{
            if(!reportReady()) return;
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Diagnostic DepthX",fullReport()));
            toast("Rapport copié. Tu peux le coller dans notre conversation.");
        }));
        page.addView(button("Enregistrer le rapport",()->{
            if(!reportReady()) return;
            try { startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"DepthX-diagnostic.txt"),SAVE_REPORT); }
            catch(RuntimeException e) { toast("Aucun sélecteur de fichiers disponible. Utilise Copier le rapport."); }
        }));
        page.addView(text("DepthX 0.2.0 · Accès natifs · Aucun changement automatique",12,false,0xFF696477));
        page.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            view.setPadding(dp(22)+bars.left,dp(24)+bars.top,dp(22)+bars.right,dp(28)+bars.bottom);return insets;
        });
        setContentView(scroll);scan();
    }
    private boolean reportReady() { if(report.isEmpty()) { toast("Attends la fin de l’analyse.");return false; } return true; }
    private void scan() {
        scan.setEnabled(false);status.setText("Recherche en cours…");report="";candidates.removeAllViews();
        worker.execute(()->{
            try {
                Result result=inspect();
                runOnUiThread(()->{ if(isDestroyed()) return;render(result);scan.setEnabled(true); });
            } catch(Exception e) {
                runOnUiThread(()->{ if(isDestroyed()) return;status.setText("Analyse incomplète : "+e.getClass().getSimpleName());scan.setEnabled(true); });
            }
        });
    }
    private Result inspect() {
        Result result=new Result();PackageManager pm=getPackageManager();
        StringBuilder out=new StringBuilder("DEPTHX — ACCÈS NATIFS 0.2.0\n");
        out.append("Fabricant : ").append(Build.MANUFACTURER).append("\nModèle : ").append(Build.MODEL)
            .append("\nAndroid : ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT)
            .append(")\nBuild : ").append(Build.DISPLAY).append("\nCorrectif : ").append(Build.VERSION.SECURITY_PATCH).append('\n');
        out.append("Version XOS exacte : non déduite du numéro de build.\n");
        try {
            WallpaperManager wm=WallpaperManager.getInstance(this);
            out.append("Fonds pris en charge : ").append(wm.isWallpaperSupported())
                .append("\nChangement de fond autorisé : ").append(wm.isSetWallpaperAllowed()).append('\n');
            WallpaperInfo live=wm.getWallpaperInfo();
            out.append("Fond animé accueil : ").append(live==null?"aucun déclaré":live.getPackageName()+"/"+live.getServiceName()).append('\n');
            if(Build.VERSION.SDK_INT>=34) {
                WallpaperInfo lock=wm.getWallpaperInfo(WallpaperManager.FLAG_LOCK);
                out.append("Fond animé verrouillé : ").append(lock==null?"aucun déclaré":lock.getPackageName()+"/"+lock.getServiceName()).append('\n');
            }
        } catch(RuntimeException e) { out.append("Informations de fond limitées : ").append(e.getClass().getSimpleName()).append('\n'); }
        String[] actions={Intent.ACTION_SET_WALLPAPER,Settings.ACTION_DISPLAY_SETTINGS,WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER};
        for(String action:actions) {
            android.content.pm.ResolveInfo resolved=pm.resolveActivity(new Intent(action),PackageManager.MATCH_DEFAULT_ONLY);
            out.append("Action ").append(action).append(" : ").append(resolved==null?"aucun accès déclaré":resolved.activityInfo.packageName+"/"+resolved.activityInfo.name).append('\n');
        }
        List<PackageInfo> packages=pm.getInstalledPackages(PackageManager.GET_ACTIVITIES|PackageManager.MATCH_DISABLED_COMPONENTS);
        packages.sort(Comparator.comparing(p->p.packageName));
        for(PackageInfo pkg:packages) {
            if(!CandidatePolicy.relatedPackage(pkg.packageName)) continue;
            List<ActivityInfo> matches=new ArrayList<>();
            if(pkg.activities!=null) for(ActivityInfo a:pkg.activities)
                if(CandidatePolicy.relevant(pkg.packageName,a.name)) matches.add(a);
            if(matches.isEmpty()) continue;
            result.relevantPackages++;
            out.append("\nPAQUET ").append(pkg.packageName).append(" · ").append(pkg.versionName).append(" · code ").append(pkg.getLongVersionCode()).append('\n');
            for(ActivityInfo a:matches) {
                result.found++;
                ComponentName component=new ComponentName(pkg.packageName,a.name);
                int state=pm.getComponentEnabledSetting(component);
                int appState=pm.getApplicationEnabledSetting(pkg.packageName);
                boolean appEnabled=appState==PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (appState==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && pkg.applicationInfo!=null && pkg.applicationInfo.enabled);
                boolean enabled=appEnabled && (state==PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (state==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && a.enabled));
                boolean allowed=a.permission==null || a.permission.isEmpty() || checkSelfPermission(a.permission)==PackageManager.PERMISSION_GRANTED;
                boolean offer=CandidatePolicy.canOffer(pkg.packageName,a.name,enabled,a.exported,allowed);
                String label=a.loadLabel(pm).toString().replace('\n',' ');
                out.append("  ").append(a.name).append("\n    Libellé : ").append(label)
                    .append(" · activé=").append(enabled).append(" · exporté=").append(a.exported)
                    .append(" · permission=").append(a.permission==null?"aucune":a.permission)
                    .append(" · ouverture proposée=").append(offer).append('\n');
                if(offer) result.pages.add(new Page(label,component)); else result.blocked++;
            }
        }
        out.append("\nCONCLUSION : ").append(result.relevantPackages).append(" paquet(s) de personnalisation repéré(s), ")
            .append(result.pages.size()).append(" page(s) proposée(s).\n")
            .append("Ces noms et accès ne prouvent pas la présence ou l’activation de l’effet de profondeur.\n")
            .append("Aucune écriture de réglage, aucun composant activé, aucun fond remplacé. Aucun numéro de série, IMEI ou compte collecté.\n");
        result.report=out.toString();return result;
    }
    private void render(Result result) {
        report=result.report;
        showNativePages();
        status.setText(result.relevantPackages+" composant(s) de personnalisation · "+result.pages.size()+" menu(s) à essayer");
        if(result.pages.isEmpty()) candidates.addView(text("Aucun menu compatible repéré. Copie le rapport pour poursuivre le diagnostic.",15,false,0xFF696477));
        for(Page page:result.pages) {
            if(NativePages.contains(page.component.getPackageName(),page.component.getClassName())) continue;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(8),dp(12),dp(12));
            card.setBackground(rounded(Color.WHITE));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(12);candidates.addView(card,params);
            card.addView(text(page.label,16,true,0xFF282237));
            card.addView(text(page.component.getPackageName(),12,false,0xFF696477));
            card.addView(button("Ouvrir ce menu",()->{
                try {
                    ActivityInfo a=getPackageManager().getActivityInfo(page.component,0);
                    boolean permission=a.permission==null||a.permission.isEmpty()||checkSelfPermission(a.permission)==PackageManager.PERMISSION_GRANTED;
                    if(!CandidatePolicy.canOffer(a.packageName,a.name,a.enabled&&a.applicationInfo.enabled,a.exported,permission)) {
                        toast("Ce menu n’est plus accessible.");return;
                    }
                    open(new Intent().setComponent(page.component));
                } catch(PackageManager.NameNotFoundException e) { toast("Ce menu a été retiré."); }
            }));
        }
    }
    private String fullReport() { return report+"\nESSAIS DES ACCÈS NATIFS\n"+attempts; }
    private boolean available(ComponentName component) {
        try {
            ActivityInfo a=getPackageManager().getActivityInfo(component,0);
            boolean permitted=a.permission==null||a.permission.isEmpty()||checkSelfPermission(a.permission)==PackageManager.PERMISSION_GRANTED;
            int state=getPackageManager().getComponentEnabledSetting(component);
            int appState=getPackageManager().getApplicationEnabledSetting(component.getPackageName());
            boolean enabled=(state==PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && a.enabled)) &&
                (appState==PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (appState==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && a.applicationInfo.enabled));
            return CandidatePolicy.canOffer(a.packageName,a.name,enabled,a.exported,permitted);
        } catch(RuntimeException|PackageManager.NameNotFoundException e) { return false; }
    }
    private void showNativePages() {
        nativePages.removeAllViews();
        for(NativePages.Entry entry:NativePages.ENTRIES) {
            ComponentName component=new ComponentName(entry.pkg,entry.activity);
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14),dp(12),dp(14),dp(14));card.setBackground(rounded(Color.WHITE));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);nativePages.addView(card,lp);
            card.addView(text(entry.title,17,true,0xFF282237));
            card.addView(text(entry.description,14,false,0xFF696477));
            boolean accessible=available(component);
            Button open=button(accessible?"Ouvrir":"Non accessible sur cette version",()->{
                if(!available(component)) { attempts.append(entry.title).append(" : accès absent ou bloqué au contrôle.\n");toast("Cet accès n’est plus disponible.");showNativePages();return; }
                try {
                    startActivity(new Intent().setComponent(component));
                    attempts.append(entry.title).append(" : ouverture demandée et acceptée par Android ; contenu non vérifié.\n");
                } catch(RuntimeException e) {
                    attempts.append(entry.title).append(" : ouverture refusée (").append(e.getClass().getSimpleName()).append(").\n");
                    toast("XOS a refusé cette ouverture. Essaie l’entrée suivante.");
                }
            });
            open.setEnabled(accessible);card.addView(open);
        }
        nativePages.addView(text("L’éditeur de verrouillage repéré dans le générateur IA est privé à XOS. DepthX ne le lance pas directement.",13,false,0xFF696477));
    }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch(SecurityException e) { toast("XOS bloque l’accès à ce menu. Aucun réglage n’a été modifié par DepthX."); }
        catch(RuntimeException e) { toast("Ce menu n’est pas disponible depuis cette application."); }
    }
    private void showReport() {
        if(!reportReady()) return;
        TextView body=text(fullReport(),12,false,0xFF282237);body.setTextIsSelectable(true);body.setPadding(dp(16),dp(12),dp(16),dp(12));
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        new AlertDialog.Builder(this).setTitle("Rapport local").setView(scroll).setPositiveButton("Fermer",null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=SAVE_REPORT||result!=RESULT_OK||data==null||data.getData()==null) return;
        Uri uri=data.getData();String content=fullReport();
        worker.execute(()->{
            try(OutputStream stream=getContentResolver().openOutputStream(uri)) {
                if(stream==null) throw new IllegalStateException();stream.write(content.getBytes(StandardCharsets.UTF_8));
                runOnUiThread(()->{ if(!isDestroyed()) toast("Rapport enregistré."); });
            } catch(Exception e) { runOnUiThread(()->{ if(!isDestroyed()) toast("Enregistrement impossible. Utilise Copier le rapport."); }); }
        });
    }
    private TextView text(String value,int size,boolean bold,int color) {
        TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        view.setPadding(0,dp(8),0,dp(8));if(bold) view.setTypeface(null,Typeface.BOLD);return view;
    }
    private Button button(String label,Runnable action) {
        Button view=new Button(this);view.setText(label);view.setAllCaps(false);view.setTextSize(15);view.setTextColor(Color.WHITE);
        view.setBackground(rounded(0xFF6655CD));view.setPadding(dp(14),dp(12),dp(14),dp(12));view.setMinHeight(dp(52));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(12);view.setLayoutParams(params);
        view.setOnClickListener(v->action.run());return view;
    }
    private GradientDrawable rounded(int color) { GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));return d; }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this,value,Toast.LENGTH_LONG).show(); }
    @Override protected void onDestroy() { worker.shutdown();super.onDestroy(); }
}
