package com.josias.floatx

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.util.concurrent.Executors

/** The service owns independent display sessions and a persistent shortcut dock. */
class FloatingService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var wm: WindowManager
    private val sessions=linkedMapOf<String,WindowSession>()
    private var frontKey:String?=null
    private val binderDead=rikka.shizuku.Shizuku.OnBinderDeadListener {
        main.post {
            sessions.values.forEach { it.connectionLost() }
            toast("Shizuku s’est arrêté. Relance-le, puis rouvre l’appli depuis la barre FloatX. Le rendu est local.")
        }
    }
    private var dock:View?=null
    private var dockParams=WindowManager.LayoutParams()
    private val pins by lazy { getSharedPreferences("dock",MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        wm=getSystemService(WindowManager::class.java)
        rikka.shizuku.Shizuku.addBinderDeadListener(binderDead)
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
        if(intent?.action=="OPEN_GRID") {
            val components=intent.getStringArrayListExtra("components").orEmpty().distinct().take(4)
            val labels=intent.getStringArrayListExtra("labels").orEmpty()
            components.forEachIndexed { index,value -> openSession(value,labels.getOrNull(index) ?: "Application") }
            showDock();arrangeGrid(components);return START_NOT_STICKY
        }
        if(intent?.action=="DOCK") { showDock();return START_NOT_STICKY }
        openSession(intent?.getStringExtra("component"),intent?.getStringExtra("label") ?: "Démo interactive")
        showDock()
        return START_NOT_STICKY
    }

    private fun openSession(component:String?,label:String) {
        if(component!=null && (Build.VERSION.SDK_INT<34 || !rikka.shizuku.Shizuku.pingBinder() ||
            rikka.shizuku.Shizuku.checkSelfPermission()!=android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            toast("Ouvre FloatX pour démarrer et autoriser Shizuku.");return
        }
        val key=component ?: "demo"
        sessions[key]?.let {
            if(!it.isConnectionLost) { it.restore();return }
            closeSession(key,keepService=true)
        }
        val session=WindowSession(key,component,label)
        sessions[key]=session
        session.start()
    }
    private fun arrangeGrid(order:List<String>) {
        val chosen=order.mapNotNull { sessions[it] }.filter { it.canArrange() }.take(4)
        if(chosen.isEmpty()) return
        sessions.values.filter { it.canArrange() && it !in chosen }.forEach { it.minimizeForLayout() }
        val slots=when(chosen.size) {
            1 -> listOf(WindowPlacement.CENTER)
            2 -> listOf(WindowPlacement.LEFT,WindowPlacement.RIGHT)
            else -> listOf(WindowPlacement.TOP_LEFT,WindowPlacement.TOP_RIGHT,WindowPlacement.BOTTOM_LEFT,WindowPlacement.BOTTOM_RIGHT)
        }
        chosen.forEachIndexed { index,window -> window.snap(slots[index]) }
    }
    private fun closeSession(key:String,keepService:Boolean=false) {
        sessions.remove(key)?.dispose()
        if(frontKey==key) frontKey=null
        if(!keepService && sessions.isEmpty() && dock==null) stopSelf()
    }
    private fun d(value:Int)=Ui.dp(this,value)
    private fun toast(value:String) { Toast.makeText(this,value,Toast.LENGTH_LONG).show() }

    private fun showDock() {
        dock?.let { wm.removeView(it) }
        val row=LinearLayout(this).apply {
            gravity=Gravity.CENTER_VERTICAL;setPadding(d(6),d(4),d(6),d(4))
            background=Ui.rounded(this@FloatingService,0xF2FFFFFF.toInt(),22);elevation=d(10).toFloat()
        }
        fun picker() {
            startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("pick_pin",true))
        }
        row.addView(Ui.icon(this,"+","Ajouter des applications à la barre") { picker() })
        for(value in pins.getStringSet("apps",emptySet()).orEmpty().sorted()) {
            val target=ComponentName.unflattenFromString(value) ?: continue
            try {
                val info=packageManager.getActivityInfo(target,0)
                val label=info.loadLabel(packageManager).toString()
                row.addView(ImageView(this).apply {
                    setImageDrawable(info.loadIcon(packageManager));setPadding(d(8),d(8),d(8),d(8))
                    contentDescription="Ouvrir $label en flottant ; appui long pour retirer"
                    setOnClickListener { openSession(value,label) }
                    setOnLongClickListener {
                        val next=pins.getStringSet("apps",emptySet()).orEmpty().toMutableSet();next.remove(value)
                        pins.edit().putStringSet("apps",next).apply();showDock();true
                    }
                },LinearLayout.LayoutParams(d(48),d(48)))
            } catch(_:android.content.pm.PackageManager.NameNotFoundException) { }
        }
        row.addView(Ui.icon(this,"×","Masquer la barre de raccourcis") { dock?.let { wm.removeView(it) };dock=null;if(sessions.isEmpty()) stopSelf() })
        val scroll=HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false;addView(row) }
        dock=scroll
        val width=wm.maximumWindowMetrics.bounds.width()
        dockParams=WindowManager.LayoutParams((row.childCount*d(48)+d(12)).coerceAtMost(width-d(16)),d(56),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            android.graphics.PixelFormat.TRANSLUCENT).apply { gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL;y=d(8) }
        wm.addView(scroll,dockParams)
    }
    override fun onConfigurationChanged(newConfig:android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        sessions.values.toList().forEach { it.configurationChanged() }
        if(dock!=null) showDock()
    }
    override fun onDestroy() {
        rikka.shizuku.Shizuku.removeBinderDeadListener(binderDead)
        sessions.values.toList().forEach { it.dispose() };sessions.clear()
        dock?.let { wm.removeView(it) };dock=null
        io.shutdown();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null

    private inner class WindowSession(private val key:String,private val component:String?,private val label:String) {
    private var root: FrameLayout? = null
    private var menu:View?=null
    var isConnectionLost=false
        private set
    private var retainingTexture=false
    private val raiseRequest=Runnable { raise() }
    private var bubble: View? = null
    private var params = WindowManager.LayoutParams()
    private var bubbleParams = WindowManager.LayoutParams()
    private var host: DirectWindowBridge? = null
    private var texture: TextureView? = null
    private var screenW = 1
    private var screenH = 1
    private var renderW = 1
    private var renderH = 1
    private var minimized = false
    private var savedBox: WindowBox? = null
    private var full = false
    private var placement:WindowPlacement?=null
    private var generation = 0
    private var ready = false
    private var surface: Surface? = null
    private var status: TextView? = null
    private var failed = false
    private val startupTimeout = Runnable {
        if (!ready && !failed && root != null) {
            generation++
            val detail=host?.stage ?: "S00 · préparation"
            fail("$detail\nDélai dépassé. Ferme puis réessaie ; envoie ce message si le blocage persiste.")
            remote { close() }
        }
    }

    private var resizingByTouch=false
    private var resizeScheduled=false
    private val renderResizes=LatestRenderResize()
    private val resizeUpdate=Runnable { resizeScheduled=false;resizeDisplay() }
    private val choreographer=Choreographer.getInstance()
    private var layoutScheduled=false
    private var windowLayoutDirty=false
    private var bubbleLayoutDirty=false
    private var lastAppliedBox:WindowBox?=null
    private val layoutFrame=Choreographer.FrameCallback {
        layoutScheduled=false
        applyPendingLayouts()
    }
    private fun requestLayoutFrame() {
        if(layoutScheduled) return
        layoutScheduled=true;choreographer.postFrameCallback(layoutFrame)
    }
    private fun applyPendingLayouts() {
        if(windowLayoutDirty) {
            windowLayoutDirty=false
            val box=WindowBox(params.x,params.y,params.width,params.height)
            root?.let { if(it.isAttachedToWindow && box!=lastAppliedBox) {
                wm.updateViewLayout(it,params);lastAppliedBox=box
            } }
        }
        if(bubbleLayoutDirty) {
            bubbleLayoutDirty=false
            bubble?.let { if(it.isAttachedToWindow) wm.updateViewLayout(it,bubbleParams) }
        }
    }
    private fun flushLayouts() {
        if(layoutScheduled) { choreographer.removeFrameCallback(layoutFrame);layoutScheduled=false }
        applyPendingLayouts()
    }
    private fun beginResizeGesture() {
        resizingByTouch=true
        main.removeCallbacks(resizeUpdate);resizeScheduled=false
        renderResizes.discardPending()
        main.removeCallbacks(rootCheck)
    }
    private fun endResizeGesture() {
        resizingByTouch=false
        flushLayouts();scheduleResize()
        main.removeCallbacks(rootCheck)
        if(ready && !failed && !minimized) main.postDelayed(rootCheck,900)
    }
    private val rootCheck=object:Runnable {
        override fun run() {
            if(!ready || failed || minimized || resizingByTouch || root==null) return
            val target=host ?: return
            val token=generation
            io.execute {
                try { target.restoreRootIfEmpty() }
                catch(e:Exception) {
                    NativeFreeformLauncher.record(this@FloatingService,"Vérification de l’accueil : ${e.message}")
                }
                main.post { if(token==generation && ready && !failed && !minimized && !resizingByTouch) main.postDelayed(this,900) }
            }
        }
    }
    fun start() {
        if(component!=null) host=DirectWindowBridge(this@FloatingService)
        createWindow()
        if(component!=null) { main.postDelayed(startupTimeout,25_000);maybeOpen() }
    }

    private fun d(value:Int)=Ui.dp(this@FloatingService,value)
    private fun measureScreen() {
        val metrics=wm.maximumWindowMetrics
        val bars=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        screenW=(metrics.bounds.width()-bars.left-bars.right).coerceAtLeast(1)
        screenH=(metrics.bounds.height()-bars.top-bars.bottom).coerceAtLeast(1)
    }
    private fun windowParams(width:Int,height:Int)=WindowManager.LayoutParams(width,height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
        android.graphics.PixelFormat.TRANSLUCENT).apply {
        gravity=Gravity.TOP or Gravity.START
        softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createWindow() {
        measureScreen()
        params=windowParams((screenW*.88).toInt(),(screenH*.66).toInt()).apply { x=d(12)+(sessions.size-1)*d(20); y=d(50)+(sessions.size-1)*d(32) }
        val panel=object:FrameLayout(this@FloatingService) {
            override fun dispatchTouchEvent(event:MotionEvent):Boolean {
                if(event.actionMasked==MotionEvent.ACTION_DOWN) {
                    sessions.values.forEach { it.dismissMenu() }
                }
                val handled=super.dispatchTouchEvent(event)
                // Reorder after the complete gesture: never cancel the app's DOWN/UP sequence.
                if(event.actionMasked==MotionEvent.ACTION_UP || event.actionMasked==MotionEvent.ACTION_CANCEL) {
                    main.removeCallbacks(raiseRequest);main.post(raiseRequest)
                }
                return handled
            }
        }.apply {
            background=Ui.rounded(this@FloatingService,Color.WHITE,12)
            clipToOutline=true;elevation=d(10).toFloat()
        }
        root=panel
        val column=LinearLayout(this@FloatingService).apply { orientation=LinearLayout.VERTICAL }
        panel.addView(column,FrameLayout.LayoutParams(-1,-1))
        if(component==null) createDemo(column) else createAppSurface(column)
        // Overlay the pill directly on the app; no strip, padding or extra title bar.
        val handle=FrameLayout(this@FloatingService).apply {
            contentDescription="Commandes de $label ; glisser pour déplacer"
            isClickable=true;isFocusable=true
        }
        handle.addView(View(this@FloatingService).apply {
            background=Ui.rounded(this@FloatingService,0xFF5C626B.toInt(),2)
            importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
        },FrameLayout.LayoutParams(d(36),d(4),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin=d(7) })
        handle.setOnClickListener { showMenu() }
        handle.setOnTouchListener(gesture { dx,dy ->
            dismissMenu();placement=null;params.x+=dx;params.y+=dy;updateWindow()
        })
        panel.addView(handle,FrameLayout.LayoutParams(d(64),d(24),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        // Invisible corner handles preserve the frameless app surface.
        for(corner in ResizeCorner.entries) {
            val resize=View(this@FloatingService).apply {
                contentDescription=when(corner) {
                    ResizeCorner.TOP_LEFT -> "Coin supérieur gauche : redimensionner"
                    ResizeCorner.TOP_RIGHT -> "Coin supérieur droit : redimensionner"
                    ResizeCorner.BOTTOM_LEFT -> "Coin inférieur gauche : redimensionner"
                    ResizeCorner.BOTTOM_RIGHT -> "Coin inférieur droit : redimensionner"
                }
            }
            resize.setOnTouchListener(gesture(onBegin={ beginResizeGesture() },onEnd={ endResizeGesture() }) { dx,dy ->
                dismissMenu();placement=null;full=false
                val box=WindowGeometry.resize(WindowBox(params.x,params.y,params.width,params.height),
                    corner,dx,dy,screenW,screenH,d(120).coerceAtMost(params.width),d(140).coerceAtMost(params.height))
                params.x=box.x;params.y=box.y;params.width=box.width;params.height=box.height
                updateWindow();scheduleResize()
            })
            val gravity=(if(corner.top) Gravity.TOP else Gravity.BOTTOM) or
                (if(corner.left) Gravity.START else Gravity.END)
            panel.addView(resize,FrameLayout.LayoutParams(d(24),d(24),gravity))
        }
        wm.addView(panel,params)
        lastAppliedBox=WindowBox(params.x,params.y,params.width,params.height)
        frontKey=key
        updateWindow()
    }

    fun connectionLost() {
        if(component==null) return
        isConnectionLost=true;generation++;failed=true;ready=false
        main.removeCallbacks(startupTimeout)
        status?.apply { text="Shizuku arrêté\nRelance Shizuku, puis touche l’icône de cette appli dans la barre FloatX pour la rouvrir.";visibility=View.VISIBLE }
    }
    fun dismissMenu() {
        menu?.let { if(it.isAttachedToWindow) wm.removeView(it) };menu=null
    }
    private fun showMenu() {
        if(menu!=null) { dismissMenu();return }
        val entries=mutableListOf<Pair<String,()->Unit>>()
        entries.add("Réduire en bulle" to { minimize() })
        entries.add((if(full) "Restaurer la taille" else "Agrandir") to { toggleSize() })
        entries.add("Placement de la fenêtre" to { showPlacementMenu() })
        entries.add("Organiser les fenêtres (1 à 4)" to { arrangeGrid(listOf(key)+sessions.keys.filter { it!=key }) })
        entries.add("Fermer la fenêtre" to { closeSession(key) })
        entries.add("Masquer les commandes" to { })
        commandMenu(entries)
    }
    private fun showPlacementMenu() {
        commandMenu(listOf(
            "Moitié gauche" to { snap(WindowPlacement.LEFT) },
            "Moitié droite" to { snap(WindowPlacement.RIGHT) },
            "Au centre" to { snap(WindowPlacement.CENTER) },
            "Quart supérieur gauche" to { snap(WindowPlacement.TOP_LEFT) },
            "Quart supérieur droit" to { snap(WindowPlacement.TOP_RIGHT) },
            "Quart inférieur gauche" to { snap(WindowPlacement.BOTTOM_LEFT) },
            "Quart inférieur droit" to { snap(WindowPlacement.BOTTOM_RIGHT) },
            "Retour aux commandes" to { showMenu() }
        ))
    }
    private fun commandMenu(entries:List<Pair<String,()->Unit>>) {
        raise();sessions.values.forEach { it.dismissMenu() }
        val list=LinearLayout(this@FloatingService).apply {
            orientation=LinearLayout.VERTICAL;setPadding(d(8),d(6),d(8),d(6))
            background=Ui.rounded(this@FloatingService,Color.WHITE,12);elevation=d(14).toFloat()
        }
        entries.forEach { (title,action) ->
            list.addView(Ui.text(this@FloatingService,title,14f,Ui.INK).apply {
                gravity=Gravity.CENTER_VERTICAL;setPadding(d(12),0,d(12),0)
                layoutParams=LinearLayout.LayoutParams(-1,d(44))
                setOnClickListener { dismissMenu();action() }
            })
        }
        val height=(entries.size*d(44)+d(12)).coerceAtMost((screenH-d(24)).coerceAtLeast(1))
        val box=WindowGeometry.clamp(WindowBox(params.x+(params.width-d(250))/2,params.y+d(36),d(250),height),
            screenW,screenH,d(180),height)
        val scroll=ScrollView(this@FloatingService).apply { addView(list) }
        menu=scroll
        wm.addView(scroll,windowParams(box.width,box.height).apply { x=box.x;y=box.y })
    }
    fun canArrange()=component!=null && root!=null && !failed
    fun minimizeForLayout() { minimize() }
    fun snap(mode:WindowPlacement) {
        dismissMenu();restore();applyPlacement(mode)
    }
    private fun applyPlacement(mode:WindowPlacement) {
        placement=mode;full=false;savedBox=null
        measureScreen()
        val availableH=(screenH-if(dock!=null) d(64) else 0).coerceAtLeast(1)
        val box=WindowPlacements.box(mode,screenW,availableH,d(6))
        params.x=box.x;params.y=box.y;params.width=box.width;params.height=box.height
        updateWindow();scheduleResize()
    }
    private fun raise() {
        val panel=root ?: return
        if(minimized || !panel.isAttachedToWindow || frontKey==key) return
        // WindowManager order, not View.elevation: these are separate overlay windows.
        // Preserve the producer and VirtualDisplay while moving this overlay to the top.
        val retained=texture?.surfaceTexture
        retainingTexture=retained!=null
        try {
            wm.removeViewImmediate(panel)
            if(retained!=null) texture?.setSurfaceTexture(retained)
            wm.addView(panel,params)
            frontKey=key
        } catch(e:Exception) {
            toast("Premier plan indisponible : ${e.message}")
            if(!panel.isAttachedToWindow) { retainingTexture=false;closeSession(key) }
        } finally { retainingTexture=false }
    }

    private fun createDemo(panel: LinearLayout) {
        val area=LinearLayout(this@FloatingService).apply { orientation=LinearLayout.VERTICAL; setPadding(d(16),d(16),d(16),d(10)); setBackgroundColor(0xFFF4F0E6.toInt()) }
        panel.addView(area,LinearLayout.LayoutParams(-1,0,1f))
        area.addView(Ui.text(this@FloatingService,"Alex · conversation de démo",16f,bold=true))
        val messages=LinearLayout(this@FloatingService).apply { orientation=LinearLayout.VERTICAL; setPadding(0,d(20),0,0) }
        val scroll=ScrollView(this@FloatingService).apply { addView(messages) }
        area.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        fun addMessage(value:String, outgoing:Boolean) {
            messages.addView(Ui.text(this@FloatingService,value,15f).apply {
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
        val inputRow=LinearLayout(this@FloatingService).apply { gravity=Gravity.CENTER_VERTICAL }
        val input=EditText(this@FloatingService).apply {
            hint="Message de démo…"; textSize=14f; setSingleLine(true)
            setOnFocusChangeListener { _, focused -> if(focused) focusWindow() }
            setOnTouchListener { _,_ -> focusWindow();false }
            layoutParams=LinearLayout.LayoutParams(0,d(48),1f)
        }
        inputRow.addView(input)
        inputRow.addView(Ui.icon(this@FloatingService,"➤","Envoyer le message de démo") {
            val value=input.text.toString().trim()
            if(value.isNotEmpty()) { addMessage(value,true); input.setText("") }
        })
        area.addView(inputRow)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAppSurface(panel:LinearLayout) {
        val frame=FrameLayout(this@FloatingService).apply { setBackgroundColor(0xFFF1F3EF.toInt()) }
        panel.addView(frame,LinearLayout.LayoutParams(-1,0,1f))
        val view=TextureView(this@FloatingService)
        texture=view
        frame.addView(view,FrameLayout.LayoutParams(-1,-1))
        status=Ui.text(this@FloatingService,"Connexion à l’accès système…",14f,Ui.MUTED).apply {
            gravity=Gravity.CENTER; setPadding(d(16),d(16),d(16),d(16));setBackgroundColor(0xFFF1F3EF.toInt())
        }
        frame.addView(status,FrameLayout.LayoutParams(-1,-1))
        view.surfaceTextureListener=object:TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st:SurfaceTexture,w:Int,h:Int) {
                if (root == null || failed) return
                status?.text=if(host==null) "Affichage prêt · connexion au moteur Shizuku…" else "Moteur connecté · ouverture de l’appli…"
                maybeOpen()
            }
            override fun onSurfaceTextureSizeChanged(st:SurfaceTexture,w:Int,h:Int) {
                // Keep the current producer resolution while the view scales under the finger.
                if(ready && !failed) st.setDefaultBufferSize(renderW,renderH)
                scheduleResize()
            }
            override fun onSurfaceTextureUpdated(st:SurfaceTexture) {}
            override fun onSurfaceTextureDestroyed(st:SurfaceTexture):Boolean = !retainingTexture
        }
        view.setOnTouchListener { _,event ->
            if (ready) {
                val copy=MotionEvent.obtain(event)
                val matrix=Matrix().apply { setScale(renderW.toFloat()/view.width.coerceAtLeast(1),renderH.toFloat()/view.height.coerceAtLeast(1)) }
                copy.transform(matrix)
                val target=host
                io.execute { try { check(target?.touch(copy) == true) { "Le système a refusé le tactile sur cet écran" } } catch(e:Exception) { main.post { fail("Tactile indisponible : ${e.message}") } } finally { copy.recycle() } }
            }
            true
        }
    }

    private fun maybeOpen() {
        val view=texture ?: return
        val target=host ?: return
        val st=view.surfaceTexture ?: return
        val app=component ?: return
        if(ready || surface!=null) return
        if (failed) return
        status?.text="Moteur connecté · ouverture de l’appli…"
        val size=RenderSizing.forWindow(view.width,view.height,resources.displayMetrics.densityDpi)
        renderW=size.width;renderH=size.height
        st.setDefaultBufferSize(renderW,renderH)
        val nextSurface=Surface(st);surface=nextSurface
        val token=generation
        io.execute {
            try {
                target.open(nextSurface,renderW,renderH,size.dpi,app) { stage ->
                    main.post { if (token==generation && !failed) status?.text=stage }
                }
                main.post { if(token==generation && !failed) {
                    main.removeCallbacks(startupTimeout)
                    renderResizes.seed(size)
                    ready=true;status?.visibility=View.GONE
                    resizeDisplay()
                    main.removeCallbacks(rootCheck);main.postDelayed(rootCheck,900)
                    if(!target.trustedDisplay) toast("XOS a refusé l’écran TRUSTED. Mode standard conservé ; détails dans FloatX.")
                } }
            } catch(e:Exception) { main.post { if(token==generation) fail("Ouverture refusée : ${e.message}") } }
        }
    }
    private fun scheduleResize() {
        // TextureView scales the current app image during the gesture. Reflow only on release.
        if(resizingByTouch || resizeScheduled || !ready || failed) return
        resizeScheduled=true
        main.postDelayed(resizeUpdate,32)
    }
    private fun resizeDisplay() {
        if(resizingByTouch || !ready || failed) return
        val view=texture ?: return
        if(view.width<1 || view.height<1 || !view.isAvailable) return
        val size=RenderSizing.forWindow(view.width,view.height,resources.displayMetrics.densityDpi)
        renderResizes.offer(size)?.let { applyRenderResize(it) }
    }
    private fun applyRenderResize(size:RenderSize) {
        val view=texture ?: return
        val target=host ?: return
        if(!ready || failed) return
        val token=generation
        renderW=size.width;renderH=size.height
        view.surfaceTexture?.setDefaultBufferSize(size.width,size.height)
        io.execute {
            val error=try { target.resize(size.width,size.height,size.dpi);null } catch(e:Exception) { e }
            main.post {
                if(token!=generation || target!==host || !ready || failed) return@post
                if(error!=null) toast("Redimensionnement refusé : ${error.message}")
                renderResizes.complete(error==null)?.let { applyRenderResize(it) }
            }
        }
    }
    private fun remote(action:DirectWindowBridge.()->Unit) {
        val target=host ?: return
        io.execute { try { action(target) } catch(e:Exception) { main.post { toast("Commande refusée : ${e.message}") } } }
    }
    private fun focusWindow() {
        params.flags=params.flags and (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM).inv()
        root?.let { wm.updateViewLayout(it,params) }
    }
    private fun updateWindow() {
        val box=WindowGeometry.clamp(WindowBox(params.x,params.y,params.width,params.height),screenW,screenH,d(120).coerceAtMost(params.width),d(140).coerceAtMost(params.height))
        params.x=box.x;params.y=box.y;params.width=box.width;params.height=box.height
        windowLayoutDirty=true;requestLayoutFrame()
    }
    private fun toggleSize() {
        placement=null
        if(!full) {
            savedBox=WindowBox(params.x,params.y,params.width,params.height)
            params.x=0;params.y=0;params.width=screenW;params.height=screenH
        } else savedBox?.let { params.x=it.x;params.y=it.y;params.width=it.width;params.height=it.height }
        full=!full;updateWindow();scheduleResize()
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun minimize() {
        if(minimized) return
        main.removeCallbacks(rootCheck)
        dismissMenu();minimized=true
        if(frontKey==key) frontKey=null
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(root?.windowToken,0)
        // Keep the texture attached while minimized; removing it would destroy the display surface.
        params.alpha=0f
        params.flags=params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        root?.let { wm.updateViewLayout(it,params) }
        val b=FrameLayout(this@FloatingService).apply {
            background=Ui.rounded(this@FloatingService,Ui.GREEN,28)
            elevation=d(8).toFloat();contentDescription="Rouvrir $label ; glisser pour déplacer la bulle"
            setOnClickListener { restore() }
        }
        val icon=component?.let { ComponentName.unflattenFromString(it) }?.let {
            try { packageManager.getActivityIcon(it) } catch (_: android.content.pm.PackageManager.NameNotFoundException) { null }
        }
        if(icon!=null) {
            b.addView(ImageView(this@FloatingService).apply { setImageDrawable(icon) },FrameLayout.LayoutParams(d(36),d(36),Gravity.CENTER))
        } else b.addView(Ui.text(this@FloatingService,"F",24f,Color.WHITE,true).apply { gravity=Gravity.CENTER },FrameLayout.LayoutParams(-1,-1))
        bubble=b
        bubbleParams=windowParams(d(56),d(56)).apply { x=screenW-d(60);y=(d(70)+sessions.keys.indexOf(key)*d(64)).coerceIn(0,(screenH-d(56)).coerceAtLeast(0)) }
        b.setOnTouchListener(gesture(onEnd={
            bubbleParams.x=if(bubbleParams.x+ d(28)< screenW/2) d(4) else screenW-d(60)
            bubbleLayoutDirty=true;requestLayoutFrame()
        }) { dx,dy ->
            bubbleParams.x=(bubbleParams.x+dx).coerceIn(0,(screenW-d(56)).coerceAtLeast(0))
            bubbleParams.y=(bubbleParams.y+dy).coerceIn(0,(screenH-d(56)).coerceAtLeast(0))
            bubbleLayoutDirty=true;requestLayoutFrame()
        })
        wm.addView(b,bubbleParams)
    }
    fun restore() {
        bubble?.let { wm.removeView(it) };bubble=null;minimized=false
        params.alpha=1f;params.flags=params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        root?.let { wm.updateViewLayout(it,params) }
        raise()
        main.removeCallbacks(rootCheck);if(ready) main.postDelayed(rootCheck,900)
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun gesture(onBegin:()->Unit={},onEnd:()->Unit={},move:(Int,Int)->Unit):View.OnTouchListener {
        val motion=DragMotion(ViewConfiguration.get(this@FloatingService).scaledTouchSlop)
        var pointerId=-1
        fun sample(e:MotionEvent,index:Int) {
            motion.sample(e.getRawX(index),e.getRawY(index))?.let { (dx,dy) -> move(dx,dy) }
        }
        fun end(v:View,e:MotionEvent,index:Int,cancelled:Boolean) {
            if(pointerId<0) return
            if(!cancelled && index>=0) sample(e,index) // Include the last UP coordinates.
            val click=!cancelled && !motion.dragging
            pointerId=-1
            flushLayouts();onEnd();flushLayouts()
            if(click) v.performClick()
        }
        return View.OnTouchListener { v,e ->
            when(e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pointerId=e.getPointerId(0);motion.begin(e.getRawX(0),e.getRawY(0));onBegin();true
                }
                MotionEvent.ACTION_MOVE -> {
                    val index=e.findPointerIndex(pointerId)
                    if(index>=0) sample(e,index)
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if(e.getPointerId(e.actionIndex)==pointerId) { sample(e,e.actionIndex);end(v,e,e.actionIndex,true) }
                    true
                }
                MotionEvent.ACTION_UP -> { end(v,e,e.findPointerIndex(pointerId),false);true }
                MotionEvent.ACTION_CANCEL -> { end(v,e,-1,true);true }
                else -> true
            }
        }
    }
    private fun fail(message:String) {
        if(isConnectionLost) return
        failed=true;main.removeCallbacks(startupTimeout)
        ready=false;status?.apply { text=message;visibility=View.VISIBLE };toast(message)
    }
    private fun toast(value:String) { Toast.makeText(this@FloatingService,value,Toast.LENGTH_LONG).show() }
    fun configurationChanged() {
        dismissMenu();measureScreen()
        placement?.let { applyPlacement(it) } ?: run { updateWindow();scheduleResize() }
        bubble?.let { bubbleParams.x=(screenW-d(60)).coerceAtLeast(0);bubbleParams.y=bubbleParams.y.coerceIn(0,(screenH-d(56)).coerceAtLeast(0));wm.updateViewLayout(it,bubbleParams) }
    }
    fun dispose() {
        main.removeCallbacks(rootCheck)
        main.removeCallbacks(raiseRequest);dismissMenu()
        main.removeCallbacks(resizeUpdate);resizeScheduled=false
        choreographer.removeFrameCallback(layoutFrame);layoutScheduled=false
        windowLayoutDirty=false;bubbleLayoutDirty=false
        renderResizes.discardPending()
        main.removeCallbacks(startupTimeout)
        failed=true
        generation++;ready=false
        val target=host;host=null
        val ownedSurface=surface;surface=null
        val detachedTexture=if(root?.isAttachedToWindow!=true) texture?.surfaceTexture else null
        bubble?.let { wm.removeView(it) };bubble=null
        root?.let { if(it.isAttachedToWindow) wm.removeView(it) };root=null
        io.execute {
            try { target?.close() } catch (_:Exception) { }
            finally {
                ownedSurface?.release();detachedTexture?.release()
            }
        }

    }

}

}
