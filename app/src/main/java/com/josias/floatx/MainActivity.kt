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

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var report: TextView
    private lateinit var compatibility: Switch
    private lateinit var large: CheckBox
    private val prefs by lazy { getSharedPreferences("preferences",MODE_PRIVATE) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val scroll=ScrollView(this).apply { setBackgroundColor(0xFFF8F7F2.toInt());isFillViewport=true }
        val page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(d(24),d(24),d(24),d(36)) }
        scroll.addView(page)
        page.addView(Ui.text(this,"FLOATX  /  0.2 · FENÊTRES ANDROID",12f,Ui.GREEN,true))
        page.addView(Ui.text(this,"Tes applis,\nen flottant.",38f,bold=true).apply { setPadding(0,d(16),0,d(12)) })
        page.addView(Ui.text(this,"Lancement direct, sans Shizuku ni root.",17f,Ui.MUTED))
        val card=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;background=Ui.rounded(this@MainActivity,0xFFF0DEAB.toInt())
            setPadding(d(20),d(20),d(20),d(20))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { topMargin=d(24);bottomMargin=d(20) }
        }
        card.addView(Ui.text(this,"On valide l’ouverture.",23f,bold=true))
        card.addView(Ui.text(this,"Commence par Calculatrice. La barre et les commandes de la vraie fenêtre sont celles d’Android pour cet essai.",15f).apply { setPadding(0,d(12),0,0) })
        page.addView(card)
        status=Ui.text(this,"",13f,Ui.MUTED);page.addView(status)
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
        page.addView(Ui.button(this,"Choisir une vraie application") { chooseApp() })
        page.addView(Ui.button(this,"Tester une fenêtre FloatX") {
            launchNative(ComponentName(this,FreeformProbeActivity::class.java),"Test FloatX")
        })
        report=Ui.text(this,"",13f,Ui.MUTED).apply { setPadding(0,d(16),0,d(12));setTextIsSelectable(true) }
        page.addView(report)
        page.addView(Ui.text(this,"La démo validée",22f,bold=true))
        page.addView(Ui.text(this,"Retrouve nos gestes et notre bulle dans la conversation fictive. Leur apparence n’est pas encore appliquée aux vraies fenêtres.",14f,Ui.MUTED).apply { setPadding(0,d(8),0,0) })
        page.addView(Ui.button(this,"Autoriser l’affichage flottant de la démo") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
        })
        page.addView(Ui.button(this,"Essayer les gestes — démo") { launchDemo() })
        page.addView(Ui.button(this,"Arrêter la démo et l’espace de compatibilité") {
            stopService(Intent(this,FloatingService::class.java));NativeFreeformLauncher.stopWorkspace(this)
            message("Démo et espace de compatibilité arrêtés. Ferme les vraies applis avec leurs commandes Android.")
        })
        page.addView(Ui.text(this,"Ton lanceur XOS reste en place. Aucun service système externe, aucune association ADB.",12f,Ui.MUTED).apply { setPadding(0,d(22),0,0) })
        page.setOnApplyWindowInsetsListener { v,insets ->
            val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars())
            v.setPadding(d(24),d(24)+bars.top,d(24),d(36)+bars.bottom);insets
        }
        setContentView(scroll)
    }
    override fun onResume() {
        super.onResume()
        if (!::status.isInitialized) return
        val setting=try { Settings.Global.getInt(contentResolver,"enable_freeform_support",0)==1 } catch (_: SecurityException) { false }
        val feature=packageManager.hasSystemFeature("android.software.freeform_window_management")
        status.text="${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}\nFenêtres libres : ${if(setting||feature) "signalées par Android" else "à vérifier par le test"}"
        report.text=NativeFreeformLauncher.last(this)
    }
    private fun chooseApp() {
        val query=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps=packageManager.queryIntentActivities(query,0).filter { it.activityInfo.packageName!=packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        if(apps.isEmpty()) { message("Aucune application accessible.");return }
        val labels=apps.map { "${it.loadLabel(packageManager)}\n${it.activityInfo.packageName}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Ouvrir dans une fenêtre Android").setItems(labels) { _,index ->
            val app=apps[index]
            launchNative(ComponentName(app.activityInfo.packageName,app.activityInfo.name),app.loadLabel(packageManager).toString())
        }.setNegativeButton("Annuler",null).show()
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
}
