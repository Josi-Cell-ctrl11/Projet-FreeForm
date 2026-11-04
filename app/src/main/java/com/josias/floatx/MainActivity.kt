package com.josias.floatx

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var status: TextView
    private var pendingPicker = false
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        runOnUiThread {
            if (result == PackageManager.PERMISSION_GRANTED) chooseApp()
            else message("Autorisation refusée. La démo reste disponible.")
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        val scroll = ScrollView(this).apply { setBackgroundColor(0xFFF8F7F2.toInt()); isFillViewport = true }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(d(24), d(24), d(24), d(36)) }
        scroll.addView(page)
        page.addView(Ui.text(this, "FLOATX  /  PROTOTYPE 0.1", 12f, Ui.GREEN, true))
        page.addView(Ui.text(this, "Tes applis,\nen flottant.", 38f, bold = true).apply { setPadding(0,d(16),0,d(12)) })
        page.addView(Ui.text(this, "Une petite fenêtre. Tout ton téléphone autour.", 17f, Ui.MUTED))
        val preview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = Ui.rounded(this@MainActivity, 0xFFF0DEAB.toInt())
            setPadding(d(20),d(20),d(20),d(20))
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply { topMargin=d(24); bottomMargin=d(20) }
        }
        preview.addView(Ui.text(this,"━━                  ↗   ×",20f,Ui.GREEN))
        preview.addView(Ui.text(this,"Déplacer. Ajuster. Réduire.",22f,bold=true).apply { setPadding(0,d(22),0,d(8)) })
        preview.addView(Ui.text(this,"Une poignée discrète et une bulle pour retrouver ta fenêtre.",15f))
        page.addView(preview)
        status = Ui.text(this,"",14f,Ui.MUTED)
        page.addView(status)
        page.addView(Ui.button(this,"1 · Autoriser l’affichage flottant") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        page.addView(Ui.button(this,"2 · Essayer les gestes — démo") { launch(null, "Démo interactive") })
        page.addView(Ui.text(this,"La démo est une conversation fictive : teste la poignée, le coin, la bulle et le clavier.",13f,Ui.MUTED).apply { setPadding(0,d(10),0,d(20)) })
        page.addView(Ui.text(this,"Vraies applications",22f,bold=true))
        page.addView(Ui.text(this,"Mode expérimental : nécessite Shizuku démarré et autorisé. Compatibilité XOS et clavier à vérifier sur ton téléphone.",14f,Ui.MUTED).apply { setPadding(0,d(8),0,0) })
        page.addView(Ui.button(this,"Choisir une application") { requestRealApp() })
        page.addView(Ui.button(this,"Guide Shizuku") { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/guide/setup/"))) })
        page.addView(Ui.button(this,"Fermer toutes les fenêtres") { stopService(Intent(this,FloatingService::class.java)) })
        page.addView(Ui.text(this,"Aucune modification du lanceur XOS. Aucun accès aux messages ou aux données privées des autres applis.",12f,Ui.MUTED).apply { setPadding(0,d(22),0,0) })
        page.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
            v.setPadding(d(24), d(24)+bars.top, d(24),d(36)+bars.bottom); insets
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) status.text = "Affichage flottant : " +
            (if (Settings.canDrawOverlays(this)) "autorisé ✓" else "à autoriser") +
            "\n" + Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE
        if (pendingPicker && Settings.canDrawOverlays(this)) { pendingPicker=false; requestRealApp() }
    }

    private fun requestRealApp() {
        if (!Settings.canDrawOverlays(this)) {
            pendingPicker=true
            message("Autorise d’abord l’affichage par-dessus les autres applications avec le premier bouton.")
            return
        }
        if (!Shizuku.pingBinder()) { message("Démarre d’abord Shizuku. Le bouton Guide explique comment faire ; la démo fonctionne sans lui."); return }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) chooseApp()
        else if (Shizuku.shouldShowRequestPermissionRationale()) message("Autorise FloatX dans Shizuku → Applications autorisées.")
        else Shizuku.requestPermission(100)
    }

    private fun chooseApp() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(query,0).filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = apps.map { "${it.loadLabel(packageManager)}\n${it.activityInfo.packageName}" }.toTypedArray()
        if (apps.isEmpty()) { message("Aucune application accessible."); return }
        AlertDialog.Builder(this).setTitle("Ouvrir en fenêtre — expérimental")
            .setItems(labels) { _, index ->
                val app = apps[index]
                val component = android.content.ComponentName(app.activityInfo.packageName,app.activityInfo.name)
                launch(component.flattenToString(),app.loadLabel(packageManager).toString())
            }.setNegativeButton("Annuler",null).show()
    }

    private fun launch(component: String?, label: String) {
        if (!Settings.canDrawOverlays(this)) { message("Autorise d’abord l’affichage flottant avec le premier bouton."); return }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),101)
        startForegroundService(Intent(this,FloatingService::class.java).putExtra("component",component).putExtra("label",label))
        moveTaskToBack(true)
    }
    private fun message(value: String) { AlertDialog.Builder(this).setMessage(value).setPositiveButton("Compris",null).show() }
    private fun d(value:Int)=Ui.dp(this,value)
    override fun onDestroy() { Shizuku.removeRequestPermissionResultListener(permissionListener); super.onDestroy() }
}
