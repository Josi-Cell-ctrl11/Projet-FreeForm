package com.josias.floatx

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*

/** Local gesture demo only. Native app windows are owned and controlled by Android. */
class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var bubble: TextView? = null
    private var params = WindowManager.LayoutParams()
    private var bubbleParams = WindowManager.LayoutParams()
    private var label = "Démo interactive"
    private var screenW = 1
    private var screenH = 1
    private var minimized = false
    private var savedBox: WindowBox? = null
    private var full = false
    override fun onCreate() {
        super.onCreate()
        wm=getSystemService(WindowManager::class.java)
        val channel = NotificationChannel("windows","Fenêtre flottante",NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val open = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this,1,Intent(this,FloatingService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this,"windows").setSmallIcon(com.josias.floatx.R.drawable.ic_floatx)
            .setContentTitle("FloatX · fenêtre active").setContentText("Appuie pour revenir aux commandes.")
            .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null,"Tout fermer",stop).build()).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1,notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP" || !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (root != null) { toast("Ferme la fenêtre actuelle avant d’en ouvrir une autre."); return START_NOT_STICKY }
        label="Démo interactive"
        createWindow()
        return START_NOT_STICKY
    }

    private fun d(value:Int)=Ui.dp(this,value)
    private fun measureScreen() {
        val metrics=wm.maximumWindowMetrics
        val bars=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        screenW=(metrics.bounds.width()-bars.left-bars.right).coerceAtLeast(1)
        screenH=(metrics.bounds.height()-bars.top-bars.bottom).coerceAtLeast(1)
    }
    private fun windowParams(width:Int,height:Int)=WindowManager.LayoutParams(width,height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        android.graphics.PixelFormat.TRANSLUCENT).apply {
        gravity=Gravity.TOP or Gravity.START
        softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createWindow() {
        measureScreen()
        params=windowParams((screenW*.88).toInt(),(screenH*.66).toInt()).apply { x=d(16); y=d(70) }
        val panel=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; background=Ui.rounded(this@FloatingService,Color.WHITE,24)
            clipToOutline=true; elevation=d(14).toFloat()
        }
        root=panel
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(d(8),0,d(2),0) }
        val drag=Ui.text(this,"━━  $label",13f,Ui.MUTED,true).apply {
            maxLines=1; ellipsize=android.text.TextUtils.TruncateAt.END
            gravity=Gravity.CENTER_VERTICAL
            layoutParams=LinearLayout.LayoutParams(0,d(48),1f)
            contentDescription="Poignée : glisser pour déplacer la fenêtre"
        }
        drag.setOnTouchListener(gesture { dx,dy -> params.x+=dx;params.y+=dy; updateWindow() })
        header.addView(drag)
        header.addView(Ui.icon(this,"−","Réduire en bulle") { minimize() })
        header.addView(Ui.icon(this,"⛶","Agrandir ou restaurer la fenêtre") { toggleSize() })
        header.addView(Ui.icon(this,"×","Fermer la fenêtre") { stopSelf() })
        panel.addView(header)
        createDemo(panel)
        val footer=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        footer.addView(Ui.text(this,"DÉMO · conversation fictive",10f,Ui.MUTED).apply {
            setPadding(d(10),0,0,0); layoutParams=LinearLayout.LayoutParams(0,d(36),1f); gravity=Gravity.CENTER_VERTICAL
        })
        val resize=Ui.icon(this,"◢","Glisser pour redimensionner") {}.apply { setTextColor(Ui.GREEN) }
        resize.setOnTouchListener(gesture() { dx,dy ->
            full=false; params.width+=dx;params.height+=dy;updateWindow()
        })
        footer.addView(resize)
        panel.addView(footer)
        wm.addView(panel,params)
        updateWindow()
    }

    private fun createDemo(panel: LinearLayout) {
        val area=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(d(16),d(16),d(16),d(10)); setBackgroundColor(0xFFF4F0E6.toInt()) }
        panel.addView(area,LinearLayout.LayoutParams(-1,0,1f))
        area.addView(Ui.text(this,"Alex · conversation de démo",16f,bold=true))
        val messages=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,d(20),0,0) }
        val scroll=ScrollView(this).apply { addView(messages) }
        area.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        fun addMessage(value:String, outgoing:Boolean) {
            messages.addView(Ui.text(this,value,15f).apply {
                setPadding(d(12),d(10),d(12),d(10))
                background=Ui.rounded(this@FloatingService,if(outgoing) 0xFFD7F7C5.toInt() else Color.WHITE,14)
                layoutParams=LinearLayout.LayoutParams(-2,-2).apply {
                    bottomMargin=d(10); gravity=if(outgoing) Gravity.END else Gravity.START
                }
            })
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
        addMessage("On se retrouve à 18 h ?",true)
        addMessage("Oui, parfait !",false)
        val inputRow=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val input=EditText(this).apply {
            hint="Message de démo…"; textSize=14f; setSingleLine(true)
            setOnFocusChangeListener { _, focused -> if(focused) focusWindow() }
            setOnTouchListener { _,_ -> focusWindow();false }
            layoutParams=LinearLayout.LayoutParams(0,d(48),1f)
        }
        inputRow.addView(input)
        inputRow.addView(Ui.icon(this,"➤","Envoyer le message de démo") {
            val value=input.text.toString().trim()
            if(value.isNotEmpty()) { addMessage(value,true); input.setText("") }
        })
        area.addView(inputRow)
    }

    private fun focusWindow() {
        params.flags=params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        root?.let { wm.updateViewLayout(it,params) }
    }
    private fun updateWindow() {
        val box=WindowGeometry.clamp(WindowBox(params.x,params.y,params.width,params.height),screenW,screenH,d(260),d(300))
        params.x=box.x;params.y=box.y;params.width=box.width;params.height=box.height
        root?.let { if(it.isAttachedToWindow) wm.updateViewLayout(it,params) }
    }
    private fun toggleSize() {
        if(!full) {
            savedBox=WindowBox(params.x,params.y,params.width,params.height)
            params.x=0;params.y=0;params.width=screenW;params.height=screenH
        } else savedBox?.let { params.x=it.x;params.y=it.y;params.width=it.width;params.height=it.height }
        full=!full;updateWindow()
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun minimize() {
        if(minimized) return
        minimized=true
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(root?.windowToken,0)
        // Keep the demo view attached to preserve its local conversation state.
        params.alpha=0f
        params.flags=params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        root?.let { wm.updateViewLayout(it,params) }
        val b=Ui.text(this,"F",24f,Color.WHITE,true).apply {
            gravity=Gravity.CENTER; background=Ui.rounded(this@FloatingService,Ui.GREEN,28)
            elevation=d(8).toFloat();contentDescription="Rouvrir $label ; glisser pour déplacer la bulle"
            setOnClickListener { restore() }
        }
        bubble=b
        bubbleParams=windowParams(d(56),d(56)).apply { x=screenW-d(60);y=params.y.coerceIn(0,(screenH-d(56)).coerceAtLeast(0)) }
        b.setOnTouchListener(gesture(onEnd={
            bubbleParams.x=if(bubbleParams.x+ d(28)< screenW/2) d(4) else screenW-d(60)
            bubble?.let { wm.updateViewLayout(it,bubbleParams) }
        },onClick={ restore() }) { dx,dy ->
            bubbleParams.x=(bubbleParams.x+dx).coerceIn(0,(screenW-d(56)).coerceAtLeast(0))
            bubbleParams.y=(bubbleParams.y+dy).coerceIn(0,(screenH-d(56)).coerceAtLeast(0))
            bubble?.let { wm.updateViewLayout(it,bubbleParams) }
        })
        wm.addView(b,bubbleParams)
    }
    private fun restore() {
        bubble?.let { wm.removeView(it) };bubble=null;minimized=false
        params.alpha=1f;params.flags=params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        root?.let { wm.updateViewLayout(it,params) }
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun gesture(onEnd:()->Unit={},onClick:()->Unit={},move:(Int,Int)->Unit):View.OnTouchListener {
        var lastX=0f;var lastY=0f;var startX=0f;var startY=0f;var moved=false
        val slop=ViewConfiguration.get(this).scaledTouchSlop
        return View.OnTouchListener { v,e ->
            when(e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX=e.rawX;startY=e.rawY;lastX=e.rawX;lastY=e.rawY;moved=false;true }
                MotionEvent.ACTION_MOVE -> {
                    if(!moved && (kotlin.math.abs(e.rawX-startX)>slop || kotlin.math.abs(e.rawY-startY)>slop)) moved=true
                    if(moved) { move((e.rawX-lastX).toInt(),(e.rawY-lastY).toInt());lastX=e.rawX;lastY=e.rawY }
                    true
                }
                MotionEvent.ACTION_UP -> { if(moved) onEnd() else { v.performClick();onClick() };true }
                MotionEvent.ACTION_CANCEL -> { onEnd();true }
                else -> false
            }
        }
    }
    private fun toast(value:String) { Toast.makeText(this,value,Toast.LENGTH_LONG).show() }
    override fun onConfigurationChanged(newConfig:android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig);measureScreen();updateWindow()
        bubble?.let { bubbleParams.x=(screenW-d(60)).coerceAtLeast(0);bubbleParams.y=bubbleParams.y.coerceIn(0,(screenH-d(56)).coerceAtLeast(0));wm.updateViewLayout(it,bubbleParams) }
    }
    override fun onDestroy() {
        bubble?.let { wm.removeView(it) };bubble=null
        root?.let { wm.removeView(it) };root=null
        stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
}
