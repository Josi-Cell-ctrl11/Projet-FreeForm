package com.josias.floatx

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var setup: LinearLayout
    private lateinit var favorites: LinearLayout
    private var pendingLaunch=false
    private val prefs by lazy { getSharedPreferences("preferences",MODE_PRIVATE) }
    private val permissionListener=Shizuku.OnRequestPermissionResultListener { _,result ->
        runOnUiThread {
            val launchAfterPermission=pendingLaunch;pendingLaunch=false
            if(result==PackageManager.PERMISSION_GRANTED) {
                refreshHome()
                if(launchAfterPermission) chooseApp(true)
            }
            else message("Autorise FloatX dans Shizuku → Applications autorisées pour utiliser le cadre arrondi.")
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        val scroll=ScrollView(this).apply { setBackgroundColor(0xFFF8F7F2.toInt());isFillViewport=true }
        val page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(d(22),d(24),d(22),d(28)) }
        scroll.addView(page)
        val heading=LinearLayout(this).apply { gravity=android.view.Gravity.CENTER_VERTICAL }
        heading.addView(Ui.text(this,"FloatX",34f,bold=true),LinearLayout.LayoutParams(0,-2,1f))
        heading.addView(Ui.icon(this,"⚙","Réglages") { settingsMenu() })
        page.addView(heading)
        page.addView(Ui.text(this,"Tes applis, à ta façon.",16f,Ui.MUTED).apply { setPadding(0,d(6),0,d(20)) })
        val card=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;background=Ui.rounded(this@MainActivity,0xFFEAF2E7.toInt(),18)
            setPadding(d(16),d(16),d(16),d(16))
        }
        status=Ui.text(this,"",14f,Ui.GREEN,true);card.addView(status)
        setup=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL };card.addView(setup)
        page.addView(card)
        page.addView(Ui.button(this,"+  Ouvrir une appli") { requestRounded() })
        page.addView(secondary("Organiser jusqu’à 4 applis") { chooseGridApps() })
        val favoriteHeading=LinearLayout(this).apply { gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(0,d(24),0,d(10)) }
        favoriteHeading.addView(Ui.text(this,"Mes applis",19f,bold=true),LinearLayout.LayoutParams(0,-2,1f))
        favoriteHeading.addView(Ui.icon(this,"+","Choisir mes applis favorites") { choosePins() })
        page.addView(favoriteHeading)
        favorites=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        page.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false;addView(favorites) })
        page.addView(secondary("Afficher la barre flottante") { showDock() })
        page.addView(secondary("Centre de contrôle") { tileSettings() })
        page.addView(Ui.text(this,"1.0.3  ·  Sur ton téléphone",12f,Ui.MUTED).apply { setPadding(0,d(24),0,0) })
        page.setOnApplyWindowInsetsListener { v,insets ->
            val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars())
            v.setPadding(d(22),d(24)+bars.top,d(22),d(28)+bars.bottom);insets
        }
        setContentView(scroll)
        if(intent.getBooleanExtra("pick_pin",false)) scroll.post { choosePins() }
        if(intent.hasExtra("configure_tile")) scroll.post {
            val slot=intent.getIntExtra("configure_tile",0)
            if(slot==0) requestRounded() else chooseTileApp(slot)
        }
    }
    private fun secondary(title:String,action:()->Unit)=Ui.button(this,title,action).apply {
        setTextColor(Ui.INK);background=Ui.rounded(this@MainActivity,android.graphics.Color.WHITE,16)
    }
    override fun onResume() {
        super.onResume()
        if(!::status.isInitialized) return
        refreshHome();(0..4).forEach { AppTiles.refresh(this,it) }
    }
    private fun refreshHome() {
        val overlay=Settings.canDrawOverlays(this)
        val online=Shizuku.pingBinder()
        val authorized=online && Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED
        status.text=when {
            Build.VERSION.SDK_INT<34 -> "Android 14 ou supérieur requis pour les applis flottantes"
            overlay && authorized -> "●  Prêt à ouvrir tes applis"
            !overlay -> "Une autorisation reste à activer"
            !online -> "Shizuku est à démarrer"
            else -> "Autorise FloatX dans Shizuku"
        }
        setup.removeAllViews()
        if(!overlay) setup.addView(secondary("Autoriser l’affichage flottant") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
        })
        if(!authorized) setup.addView(secondary(if(online) "Autoriser FloatX" else "Démarrer Shizuku") { connectShizuku() })
        favorites.removeAllViews()
        val apps=getSharedPreferences("dock",MODE_PRIVATE).getStringSet("apps",emptySet()).orEmpty()
        for(value in apps.sorted()) {
            val target=ComponentName.unflattenFromString(value) ?: continue
            try {
                val info=packageManager.getActivityInfo(target,0)
                val label=info.loadLabel(packageManager).toString()
                val tile=LinearLayout(this).apply {
                    orientation=LinearLayout.VERTICAL;gravity=android.view.Gravity.CENTER
                    setPadding(d(6),d(8),d(6),d(8));contentDescription="Ouvrir $label en flottant"
                    setOnClickListener { launchRounded(target,label) }
                    setOnLongClickListener { choosePins();true }
                }
                tile.addView(ImageView(this).apply { setImageDrawable(info.loadIcon(packageManager)) },LinearLayout.LayoutParams(d(44),d(44)))
                tile.addView(Ui.text(this,label,12f).apply {
                    gravity=android.view.Gravity.CENTER;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
                    setPadding(0,d(8),0,0)
                },LinearLayout.LayoutParams(-1,-2))
                favorites.addView(tile,LinearLayout.LayoutParams(d(86),d(98)))
            } catch(_:PackageManager.NameNotFoundException) { }
        }
        if(favorites.childCount==0) favorites.addView(Ui.text(this,"Ajoute tes applis avec + pour les retrouver ici.",14f,Ui.MUTED).apply { setPadding(0,d(12),0,d(20)) })
    }
    private fun connectShizuku() {
        if(Shizuku.pingBinder()) {
            if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED) refreshHome()
            else if(Shizuku.shouldShowRequestPermissionRationale()) message("Autorise FloatX dans Shizuku → Applications autorisées.")
            else Shizuku.requestPermission(100)
        } else {
            val launch=packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if(launch!=null) startActivity(launch)
            else startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/guide/setup/")))
        }
    }
    private fun showDock() {
        if(!Settings.canDrawOverlays(this)) { message("Autorise l’affichage flottant depuis l’accueil.");return }
        startForegroundService(Intent(this,FloatingService::class.java).setAction("DOCK"));moveTaskToBack(true)
    }
    private fun settingsMenu() {
        val items=arrayOf("Mes applis favorites","Centre de contrôle","Connexion Shizuku","Mode système de secours","Démo des gestes","Diagnostic","Tout fermer","À propos")
        AlertDialog.Builder(this).setTitle("Réglages").setItems(items) { _,index ->
            when(index) {
                0 -> choosePins()
                1 -> tileSettings()
                2 -> connectShizuku()
                3 -> advancedMode()
                4 -> launchDemo()
                5 -> AlertDialog.Builder(this).setTitle("Diagnostic").setView(Ui.text(this,
                    "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}\n\n${NativeFreeformLauncher.last(this)}",13f,Ui.MUTED)
                    .apply { setPadding(d(20),d(16),d(20),d(16));setTextIsSelectable(true) }).setPositiveButton("Fermer",null).show()
                6 -> { stopService(Intent(this,FloatingService::class.java));NativeFreeformLauncher.stopWorkspace(this);message("Les fenêtres FloatX et la barre sont fermées.") }
                7 -> message("FloatX 1.0.3\nTes applications dans des fenêtres flottantes.\n\nLe rendu est local. Shizuku doit rester actif. En cas d’arrêt à la coupure du Wi-Fi, conserve le débogage USB activé et autorise Shizuku en arrière-plan.")
            }
        }.setNegativeButton("Fermer",null).show()
    }
    private fun advancedMode() {
        val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(d(20),d(8),d(20),0) }
        column.addView(Switch(this).apply {
            text="Compatibilité de lancement";isChecked=prefs.getBoolean("compatibility",true)
            setOnCheckedChangeListener { _,value -> prefs.edit().putBoolean("compatibility",value).apply() }
        })
        column.addView(CheckBox(this).apply {
            text="Demander une grande fenêtre";isChecked=prefs.getBoolean("large",false)
            setOnCheckedChangeListener { _,value -> prefs.edit().putBoolean("large",value).apply() }
        })
        val dialog=AlertDialog.Builder(this).setTitle("Mode système de secours").setView(column)
            .setPositiveButton("Choisir une appli") { _,_ -> chooseApp(false) }
            .setNeutralButton("Options développeur") { _,_ ->
                try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                catch(_:android.content.ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
            }.setNegativeButton("Fermer",null).create()
        dialog.show()
    }
    private fun requestRounded() {
        if(Build.VERSION.SDK_INT<34) { message("Le cadre Shizuku direct nécessite Android 14 ou supérieur.");return }
        if(!Settings.canDrawOverlays(this)) { message("Autorise l’affichage flottant depuis l’accueil.");return }
        if(!Shizuku.pingBinder()) { message("Démarre Shizuku, puis reviens dans FloatX.");return }
        if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED) chooseApp(true)
        else if(Shizuku.shouldShowRequestPermissionRationale()) message("Autorise FloatX dans Shizuku → Applications autorisées.")
        else { pendingLaunch=true;Shizuku.requestPermission(100) }
    }
    private fun chooseApp(rounded:Boolean=false) {
        val query=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps=packageManager.queryIntentActivities(query,0).filter { it.activityInfo.packageName!=packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        if(apps.isEmpty()) { message("Aucune application accessible.");return }
        val labels=apps.map { "${it.loadLabel(packageManager)}\n${it.activityInfo.packageName}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle(if(rounded) "Ouvrir dans le cadre FloatX" else "Ouvrir dans une fenêtre Android").setItems(labels) { _,index ->
            val app=apps[index]
            val component=ComponentName(app.activityInfo.packageName,app.activityInfo.name)
            val label=app.loadLabel(packageManager).toString()
            if(rounded) launchRounded(component,label) else launchNative(component,label)
        }.setNegativeButton("Annuler",null).show()
    }
    private fun tileSettings() {
        val labels=(1..4).map { "Raccourci $it · ${AppTiles.label(this,it)}" }+"FloatX · choisir une appli à chaque appui"
        AlertDialog.Builder(this).setTitle("Centre de contrôle").setItems(labels.toTypedArray()) { _,index ->
            if(index==4) addTile(0) else chooseTileApp(index+1)
        }.setNegativeButton("Fermer",null).show()
    }
    private fun chooseTileApp(slot:Int) {
        val apps=packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
            .filter { it.activityInfo.packageName!=packageName }.sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        AlertDialog.Builder(this).setTitle("Appli du raccourci $slot")
            .setItems(apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()) { _,index ->
                val info=apps[index].activityInfo
                getSharedPreferences("tiles",MODE_PRIVATE).edit().putString("slot_$slot",ComponentName(info.packageName,info.name).flattenToString()).apply()
                AppTiles.refresh(this,slot);addTile(slot)
            }.setNegativeButton("Annuler",null).show()
    }
    private fun addTile(slot:Int) {
        if(Build.VERSION.SDK_INT>=33) {
            try {
                getSystemService(android.app.StatusBarManager::class.java).requestAddTileService(
                    ComponentName(this,AppTiles.type(slot)),AppTiles.label(this,slot),AppTiles.icon(this,slot),mainExecutor) { result ->
                    Toast.makeText(this,if(result==1 || result==2) "Raccourci prêt dans le centre de contrôle."
                        else "Ouvre le centre de contrôle → Modifier, puis ajoute FloatX${if(slot==0) " · Choisir" else " $slot"}.",Toast.LENGTH_LONG).show()
                }
            } catch(_:RuntimeException) { message("Ouvre le centre de contrôle → Modifier, puis ajoute FloatX${if(slot==0) " · Choisir" else " $slot"}.") }
        } else message("Ouvre le centre de contrôle → Modifier, puis ajoute la tuile FloatX.")
    }
    private fun chooseGridApps() {
        if(Build.VERSION.SDK_INT<34) { message("Le cadre FloatX nécessite Android 14 ou supérieur.");return }
        if(!Settings.canDrawOverlays(this) || !Shizuku.pingBinder() || Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED) {
            message("Active d’abord l’affichage flottant et Shizuku.");return
        }
        val apps=packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
            .filter { it.activityInfo.packageName!=packageName }
            .distinctBy { ComponentName(it.activityInfo.packageName,it.activityInfo.name).flattenToString() }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val selected=linkedSetOf<Int>()
        AlertDialog.Builder(this).setTitle("Choisir 1 à 4 applications")
            .setMultiChoiceItems(apps.map { it.loadLabel(packageManager).toString() }.toTypedArray(),BooleanArray(apps.size)) { dialog,index,checked ->
                if(checked && selected.size>=4) {
                    (dialog as AlertDialog).listView.setItemChecked(index,false)
                    Toast.makeText(this,"Quatre applications maximum pour cette grille.",Toast.LENGTH_SHORT).show()
                } else if(checked) selected.add(index) else selected.remove(index)
            }.setPositiveButton("Ouvrir en grille") { _,_ ->
                if(selected.isNotEmpty()) {
                    val targets=selected.map { ComponentName(apps[it].activityInfo.packageName,apps[it].activityInfo.name).flattenToString() }
                    val labels=selected.map { apps[it].loadLabel(packageManager).toString() }
                    startForegroundService(Intent(this,FloatingService::class.java).setAction("OPEN_GRID")
                        .putStringArrayListExtra("components",ArrayList(targets)).putStringArrayListExtra("labels",ArrayList(labels)))
                    moveTaskToBack(true)
                }
            }.setNegativeButton("Annuler",null).show()
    }
    private fun choosePins() {
        val query=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps=packageManager.queryIntentActivities(query,0).filter { it.activityInfo.packageName!=packageName }
            .distinctBy { ComponentName(it.activityInfo.packageName,it.activityInfo.name).flattenToString() }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val storage=getSharedPreferences("dock",MODE_PRIVATE)
        val selected=storage.getStringSet("apps",emptySet()).orEmpty().toMutableSet()
        val keys=apps.map { ComponentName(it.activityInfo.packageName,it.activityInfo.name).flattenToString() }
        AlertDialog.Builder(this).setTitle("Applications dans ma barre")
            .setMultiChoiceItems(apps.map { it.loadLabel(packageManager).toString() }.toTypedArray(),
                keys.map { it in selected }.toBooleanArray()) { _,index,checked ->
                if(checked) selected.add(keys[index]) else selected.remove(keys[index])
            }.setPositiveButton("Enregistrer") { _,_ ->
                storage.edit().putStringSet("apps",selected).apply()
                if(Settings.canDrawOverlays(this)) {
                    startForegroundService(Intent(this,FloatingService::class.java).setAction("DOCK"))
                    moveTaskToBack(true)
                }
            }.setNegativeButton("Annuler",null).show()
    }
    private fun launchRounded(target:ComponentName,label:String) {
        if(Build.VERSION.SDK_INT<34) { message("Le cadre FloatX nécessite Android 14 ou supérieur.");return }
        if(!Settings.canDrawOverlays(this)||!Shizuku.pingBinder()||Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED) {
            message("Affichage flottant et Shizuku autorisé sont nécessaires.");return
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),101)
        startForegroundService(Intent(this,FloatingService::class.java).putExtra("component",target.flattenToString()).putExtra("label",label))
        moveTaskToBack(true)
    }
    private fun launchNative(target: ComponentName,label: String) {
        // A leftover demo overlay should never obscure the native launch test.
        stopService(Intent(this,FloatingService::class.java))
        NativeFreeformLauncher.stopWorkspace(this)
        try { NativeFreeformLauncher.launch(this,target,label,prefs.getBoolean("compatibility",true),prefs.getBoolean("large",false)) }
        catch(e:Exception) {
            val error="N02 · Android a refusé le lancement : ${e.message ?: e.javaClass.simpleName}"
            NativeFreeformLauncher.record(this,error);message(error)
        }
    }
    private fun launchDemo() {
        if(!Settings.canDrawOverlays(this)) { message("Autorise d’abord l’affichage flottant de la démo.");return }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),101)
        startForegroundService(Intent(this,FloatingService::class.java));moveTaskToBack(true)
    }
    private fun message(value:String) { AlertDialog.Builder(this).setMessage(value).setPositiveButton("Compris",null).show() }
    private fun d(value:Int)=Ui.dp(this,value)
    override fun onDestroy() { Shizuku.removeRequestPermissionResultListener(permissionListener);super.onDestroy() }
}
