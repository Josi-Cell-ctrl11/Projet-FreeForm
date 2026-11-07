package com.josias.floatx

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import rikka.shizuku.Shizuku

/** A user tap in Quick Settings opens the floating app after the shade collapses. */
class TileLaunchActivity:Activity() {
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        val slot=intent.getIntExtra("tile_slot",0)
        val target=AppTiles.target(this,slot)
        val exists=target!=null && try { packageManager.getActivityInfo(target,0);true }
            catch(_:PackageManager.NameNotFoundException) { false }
        val ready=Build.VERSION.SDK_INT>=34 && Settings.canDrawOverlays(this) &&
            Shizuku.pingBinder() && Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED
        if(exists && ready) {
            startForegroundService(Intent(this,FloatingService::class.java)
                .putExtra("component",target!!.flattenToString()).putExtra("label",AppTiles.label(this,slot)))
            FloatingWorkspaceActivity.open(this)
        } else {
            val page=Intent(this,MainActivity::class.java)
            if(!exists) page.putExtra("configure_tile",slot)
            startActivity(page)
        }
        finish()
    }
}
