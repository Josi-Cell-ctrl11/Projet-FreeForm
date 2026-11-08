package com.josias.depthxdiag;

import java.util.Locale;

/** Names are only discovery hints. Exported/permission checks are still mandatory at runtime. */
public final class CandidatePolicy {
    private static final String[] TOPICS={"wallpaper","lockscreen","screenlock","keyguard","depth","theme","personal","customiz","aod","clock","magazine"};
    private static final String[] PAGES={"settings","setting","picker","select","choose","preview","customiz","personal","editor","style","gallery","browser","home","main"};
    private static final String[] UNSAFE={"reset","delete","remove","restore","clear","apply","confirm","factory","credential","password","pattern","enroll","setwallpaper","setlock","unlock","uninstall"};
    private CandidatePolicy() {}
    private static String lower(String value) { return value.toLowerCase(Locale.ROOT); }
    private static boolean contains(String value,String[] words) {
        for(String word:words) if(value.contains(word)) return true;
        return false;
    }
    public static boolean relatedPackage(String pkg) {
        String p=lower(pkg);
        return p.equals("com.android.settings") || p.startsWith("com.transsion.") ||
            p.startsWith("com.infinix.") || contains(p,new String[]{"wallpaper","theme","magazine","keyguard","lockscreen"});
    }
    public static boolean relevant(String pkg,String activity) {
        return relatedPackage(pkg) && contains(lower(pkg+" "+activity),TOPICS);
    }
    public static boolean canOffer(String pkg,String activity,boolean enabled,boolean exported,boolean permissionGranted) {
        String a=lower(activity);
        return enabled && exported && permissionGranted && relevant(pkg,activity) &&
            (NativePages.contains(pkg,activity) || contains(a,PAGES)) && !contains(a,UNSAFE);
    }
}
