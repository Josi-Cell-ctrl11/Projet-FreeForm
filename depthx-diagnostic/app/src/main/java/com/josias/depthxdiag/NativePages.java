package com.josias.depthxdiag;

/** Exact exported entry points observed in the user's X6895 diagnostic; not proof of functionality. */
public final class NativePages {
    public static final class Entry {
        public final String title,description,pkg,activity;
        private Entry(String title,String description,String pkg,String activity) {
            this.title=title;this.description=description;this.pkg=pkg;this.activity=activity;
        }
    }
    public static final Entry[] ENTRIES={
        new Entry("Profondeur — menu XOS","Le menu dédié repéré sur ton téléphone. À essayer en premier.",
            "com.transsion.livewallpaper.page","com.transsion.setting.livewalllpaper.page.activity.DepthFieldWallpaperActivity"),
        new Entry("Guide de profondeur de l’accueil","Guide du lanceur XOS ; il peut concerner l’accueil uniquement.",
            "com.transsion.launcher3","com.transsion.xlauncher.wallpaper.depth.activity.TrDepthGuideActivity"),
        new Entry("Fonds d’écran du lanceur XOS","Le sélecteur natif de fonds d’écran du lanceur.",
            "com.transsion.launcher3","com.transsion.systemtheme.local.WallpaperSettingActivity"),
        new Entry("Générateur de thèmes IA","L’entrée principale du générateur XOS. L’éditeur de verrouillage reste interne.",
            "com.transsion.aiwallpaper","com.transsion.aiwallpaper.ui.tab_bar.MainActivity")
    };
    private NativePages() {}
    public static boolean contains(String pkg,String activity) {
        for(Entry entry:ENTRIES) if(entry.pkg.equals(pkg)&&entry.activity.equals(activity)) return true;
        return false;
    }
}
