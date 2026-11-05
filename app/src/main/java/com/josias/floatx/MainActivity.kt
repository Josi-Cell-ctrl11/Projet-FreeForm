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
    private lateinit var report: TextView
    private lateinit var compatibility: Switch
    private lateinit var large: CheckBox
    private val prefs by lazy { getSharedPreferences("preferences",MODE_PRIVATE) }
    private val permissionListener=Shizuku.OnRequestPermissionResultListener { _,result ->
        runOnUiThread {
            if(result==PackageManager.PERMISSION_GRANTED) chooseApp(true)
            else message("Autorise FloatX dans Shizuku → Applications autorisées pour utiliser le cadre arrondi.")
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        val scroll=ScrollView(this).apply { setBackgroundColor(0xFFF8F7F2.toInt());isFillViewport=true }
        val page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(d(24),d(24),d(24),d(36)) }
        scroll.addView(page)
        page.addView(Ui.text(this,"FLOATX  /  0.5.1 · MULTIFENÊTRES",12f,Ui.GREEN,true))
        page.addView(Ui.text(this,"Tes applis,\nen flottant.",38f,bold=true).apply { setPadding(0,d(16),0,d(12)) })
        page.addView(Ui.text(this,"Tes applis dans des fenêtres flottantes.",17f,Ui.MUTED))
        val card=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;background=Ui.rounded(this@MainActivity,0xFFF0DEAB.toInt())
            setPadding(d(20),d(20),d(20),d(20))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { topMargin=d(24);bottomMargin=d(20) }
        }
        card.addView(Ui.text(this,"Tout à portée de main.",23f,bold=true))
        card.addView(Ui.text(this,"Déplace tes fenêtres, ajuste leur taille et retrouve-les dans une bulle.",15f).apply { setPadding(0,d(12),0,0) })
        page.addView(card)
        status=Ui.text(this,"",13f,Ui.MUTED);page.addView(status)
        page.addView(Ui.button(this,"1 · Autoriser l’affichage flottant") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
        })
        page.addView(Ui.button(this,"2 · Ouvrir Shizuku") {
            val launch=packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if(launch!=null) startActivity(launch)
            else startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/guide/setup/")))
        })
        page.addView(Ui.text(this,"FloatX fonctionne localement. Pour limiter les arrêts de Shizuku, garde le débogage USB activé et autorise Shizuku en arrière-plan.",13f,Ui.MUTED))
        page.addView(Ui.button(this,"3 · Ouvrir une appli dans le cadre FloatX") { requestRounded() })
        page.addView(Ui.button(this,"Choisir les icônes de ma barre") { choosePins() })
        page.addView(Ui.button(this,"Afficher ma barre de raccourcis") {
            if(!Settings.canDrawOverlays(this)) message("Autorise d’abord l’affichage flottant.")
            else { startForegroundService(Intent(this,FloatingService::class.java).setAction("DOCK"));moveTaskToBack(true) }
        })
        page.addView(Ui.button(this,"Configurer le centre de contrôle") { tileSettings() })
        page.addView(Ui.button(this,"Ouvrir jusqu’à quatre applis en grille") { chooseGridApps() })
        page.addView(Ui.text(this,"Mode fenêtres système — secours",21f,bold=true))
        page.addView(Ui.button(this,"Ouvrir les options développeur") {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
            catch (_: android.content.ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        })
        page.addView(Ui.text(this,"Garde les options de fenêtres libres et de redimensionnement déjà utilisées avec Taskbar. FloatX ne change pas ces réglages.",13f,Ui.MUTED).apply { setPadding(0,d(10),0,d(12)) })
        compatibility=Switch(this).apply {
            text="Compatibilité de lancement type Taskbar";isChecked=prefs.getBoolean("compatibility",true)
            setTextColor(Ui.INK);setPadding(0,d(12),0,d(12))
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("compatibility",checked).apply() }
        }
        page.addView(compatibility)
        large=CheckBox(this).apply { text="Demander une grande fenêtre";isChecked=prefs.getBoolean("large",false)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("large",checked).apply() } }
        page.addView(large)
        page.addView(Ui.button(this,"Ouvrir avec le mode système type Taskbar") { chooseApp(false) })
        page.addView(Ui.button(this,"Tester une fenêtre FloatX") {
            launchNative(ComponentName(this,FreeformProbeActivity::class.java),"Test FloatX")
        })
        report=Ui.text(this,"",13f,Ui.MUTED).apply { setPadding(0,d(16),0,d(12));setTextIsSelectable(true) }
        page.addView(report)
        page.addView(Ui.text(this,"La démo validée",22f,bold=true))
        page.addView(Ui.text(this,"La conversation fictive reste disponible pour comparer les gestes.",14f,Ui.MUTED).apply { setPadding(0,d(8),0,0) })
        page.addView(Ui.button(this,"Autoriser l’affichage flottant de la démo") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
        })
        page.addView(Ui.button(this,"Essayer les gestes — démo") { launchDemo() })
        page.addView(Ui.button(this,"Arrêter la démo et l’espace de compatibilité") {
            stopService(Intent(this,FloatingService::class.java));NativeFreeformLauncher.stopWorkspace(this)
            message("Démo et espace de compatibilité arrêtés. Ferme les vraies applis avec leurs commandes Android.")
        })
        page.addView(Ui.text(this,"Shizuku doit rester actif et autorisé. Plusieurs fenêtres et bulles peuvent rester ouvertes.",12f,Ui.MUTED).apply { setPadding(0,d(22),0,0) })
        page.setOnApplyWindowInsetsListener { v,insets ->
            val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars())
            v.setPadding(d(24),d(24)+bars.top,d(24),d(36)+bars.bottom);insets
        }
        setContentView(scroll)
        if(intent.getBooleanExtra("pick_pin",false)) scroll.post { choosePins() }
        if(intent.hasExtra("configure_tile")) scroll.post {
            val slot=intent.getIntExtra("configure_tile",0)
            if(slot==0) requestRounded() else chooseTileApp(slot)
        }
    }
    override fun onResume() {
        super.onResume()
        if (!::status.isInitialized) return
        val setting=try { Settings.Global.getInt(contentResolver,"enable_freeform_support",0)==1 } catch (_: SecurityException) { false }
        val feature=packageManager.hasSystemFeature("android.software.freeform_window_management")
        val shizuku=if(!Shizuku.pingBinder()) "à démarrer" else if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED) "connecté et autorisé" else "autorisation FloatX requise"
        status.text="${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}\nShizuku : $shizuku\nFenêtres libres : ${if(setting||feature) "signalées par Android" else "à vérifier par le test"}"
        report.text=NativeFreeformLauncher.last(this)
        (0..4).forEach { AppTiles.refresh(this,it) }
    }
    private fun requestRounded() {
        if(Build.VERSION.SDK_INT<34) { message("Le cadre Shizuku direct nécessite Android 14 ou supérieur.");return }
        if(!Settings.canDrawOverlays(this)) { message("Autorise d’abord l’affichage flottant avec le premier bouton.");return }
        if(!Shizuku.pingBinder()) { message("Démarre Shizuku, puis reviens dans FloatX.");return }
        if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED) chooseApp(true)
        else if(Shizuku.shouldShowRequestPermissionRationale()) message("Autorise FloatX dans Shizuku → Applications autorisées.")
        else Shizuku.requestPermission(100)
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
        try { NativeFreeformLauncher.launch(this,target,label,compatibility.isChecked,large.isChecked) }
        catch(e:Exception) {
            val error="N02 · Android a refusé le lancement : ${e.message ?: e.javaClass.simpleName}"
            NativeFreeformLauncher.record(this,error);report.text=error;message(error)
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
