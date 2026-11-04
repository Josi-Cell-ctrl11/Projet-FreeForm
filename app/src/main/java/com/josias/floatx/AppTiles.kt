package com.josias.floatx

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

object AppTiles {
    fun type(slot:Int):Class<out TileService> = when(slot) {
        1 -> AppTileOne::class.java
        2 -> AppTileTwo::class.java
        3 -> AppTileThree::class.java
        4 -> AppTileFour::class.java
        else -> AppChooserTile::class.java
    }
    fun target(context:Context,slot:Int):ComponentName? = context.getSharedPreferences("tiles",Context.MODE_PRIVATE)
        .getString("slot_$slot",null)?.let { ComponentName.unflattenFromString(it) }
    fun label(context:Context,slot:Int):String = target(context,slot)?.let {
        try { context.packageManager.getActivityInfo(it,0).loadLabel(context.packageManager).toString() }
        catch(_:android.content.pm.PackageManager.NameNotFoundException) { null }
    } ?: if(slot==0) "FloatX · Choisir" else "FloatX $slot"
    fun icon(context:Context,slot:Int):Icon {
        val target=target(context,slot) ?: return Icon.createWithResource(context,R.drawable.ic_floatx)
        return try {
            val drawable=context.packageManager.getActivityIcon(target)
            val bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888)
            drawable.setBounds(0,0,96,96);drawable.draw(Canvas(bitmap))
            Icon.createWithBitmap(bitmap)
        } catch(_:android.content.pm.PackageManager.NameNotFoundException) {
            Icon.createWithResource(context,R.drawable.ic_floatx)
        }
    }
    fun refresh(context:Context,slot:Int) { TileService.requestListeningState(context,ComponentName(context,type(slot))) }
}

abstract class AppTileService : TileService() {
    protected abstract val slot:Int
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label=AppTiles.label(this@AppTileService,slot)
            icon=AppTiles.icon(this@AppTileService,slot)
            state=Tile.STATE_INACTIVE;updateTile()
        }
    }
    override fun onClick() {
        super.onClick()
        if(isLocked) unlockAndRun { open() } else open()
    }
    @Suppress("DEPRECATION")
    private fun open() {
        val intent=Intent(this,TileLaunchActivity::class.java).putExtra("tile_slot",slot)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if(Build.VERSION.SDK_INT>=34) startActivityAndCollapse(PendingIntent.getActivity(this,slot,intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        else startActivityAndCollapse(intent)
    }
}
class AppTileOne:AppTileService() { override val slot=1 }
class AppTileTwo:AppTileService() { override val slot=2 }
class AppTileThree:AppTileService() { override val slot=3 }
class AppTileFour:AppTileService() { override val slot=4 }
class AppChooserTile:AppTileService() { override val slot=0 }
