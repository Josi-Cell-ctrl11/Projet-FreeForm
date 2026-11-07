package com.josias.floatx

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** One ordinary Android task represents the live overlay group in Recents. */
class FloatingWorkspaceActivity:Activity() {
    private var service:FloatingService?=null
    private var bound=false
    private var resumed=false
    private val owner=Any()
    private lateinit var summary:TextView
    private lateinit var preview:ImageView
    private lateinit var windows:LinearLayout
    private var lastPreview:Bitmap?=null
    private val connection=object:ServiceConnection {
        override fun onServiceConnected(name:ComponentName,binder:IBinder) {
            val active=(binder as FloatingService.LocalBinder).service
            service=active
            active.observeWorkspace(owner) { render() }
            if(resumed) active.setWorkspaceActive(owner,true)
            render()
        }
        override fun onServiceDisconnected(name:ComponentName) { service=null;render() }
    }

    companion object {
        fun open(context:Context) {
            context.startActivity(Intent(context,FloatingWorkspaceActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
    }

    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        if(Build.VERSION.SDK_INT>=33) {
            setTaskDescription(ActivityManager.TaskDescription.Builder().setLabel("FloatX · Fenêtres")
                .setPrimaryColor(0xFFF8F7F2.toInt()).build())
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                moveTaskToBack(true)
            }
        } else {
            @Suppress("DEPRECATION")
            setTaskDescription(ActivityManager.TaskDescription("FloatX · Fenêtres"))
        }
        val page=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(d(20),d(20),d(20),d(16));setBackgroundColor(0xFFF8F7F2.toInt())
        }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        header.addView(Ui.text(this,"Mes fenêtres",24f,bold=true),LinearLayout.LayoutParams(0,-2,1f))
        header.addView(Ui.icon(this,"+","Ouvrir FloatX pour ajouter une application") {
            startActivity(Intent(this,MainActivity::class.java))
        })
        page.addView(header)
        summary=Ui.text(this,"",14f,Ui.MUTED)
        page.addView(summary)
        preview=ImageView(this).apply {
            scaleType=ImageView.ScaleType.FIT_CENTER
            contentDescription="Aperçu du groupe de fenêtres FloatX"
        }
        page.addView(preview,LinearLayout.LayoutParams(-1,0,1f))
        windows=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        page.addView(windows)
        page.setOnApplyWindowInsetsListener { v,insets ->
            val bars=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(d(20)+bars.left,d(20)+bars.top,d(20)+bars.right,d(16)+bars.bottom);insets
        }
        setContentView(page)
        render()
    }
    override fun onStart() {
        super.onStart()
        // A stale Recents card must not silently recreate an empty foreground service.
        bound=bindService(Intent(this,FloatingService::class.java),connection,0)
        if(!bound) render()
    }
    override fun onResume() {
        super.onResume();resumed=true
        preview.visibility=View.INVISIBLE
        service?.setWorkspaceActive(owner,true)
        render()
    }
    override fun onPause() {
        // Capture before hiding, so the single task's snapshot represents the whole group.
        val active=service
        lastPreview=runCatching { active?.workspacePreview() }.getOrNull()
        preview.setImageBitmap(lastPreview);preview.visibility=View.VISIBLE
        resumed=false
        active?.setWorkspaceActive(owner,false)
        super.onPause()
    }
    override fun onStop() {
        service?.forgetWorkspace(owner);service=null
        if(bound) { unbindService(connection);bound=false }
        super.onStop()
    }
    @Deprecated("Legacy navigation below Android 13")
    override fun onBackPressed() { moveTaskToBack(true) }

    private fun render() {
        if(!::summary.isInitialized) return
        val active=service
        val entries=active?.workspaceEntries().orEmpty()
        summary.text=if(entries.isEmpty()) "Aucune fenêtre active. Ouvre FloatX pour ajouter une appli."
            else "${entries.size} fenêtre(s) · Retrouve ce groupe dans les applis récentes"
        windows.removeAllViews()
        // Minimized windows stay minimized when the group is restored.
        entries.filter { it.minimized }.forEach { entry ->
            windows.addView(Ui.button(this,"Rouvrir ${entry.label}") { active?.restoreWindow(entry.key) })
        }
        if(entries.isEmpty()) {
            windows.addView(Ui.button(this,"Ouvrir FloatX") { startActivity(Intent(this,MainActivity::class.java)) })
            if(active?.isWorkspaceClosed()==true) {
                // The group has been closed, or its service has stopped; remove its stale card.
                finishAndRemoveTask()
            }
        }
    }
    private fun d(value:Int)=Ui.dp(this,value)
}
