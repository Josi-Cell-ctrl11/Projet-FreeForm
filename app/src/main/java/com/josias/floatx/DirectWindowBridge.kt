package com.josias.floatx

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.VirtualDisplay
import android.hardware.display.VirtualDisplayConfig
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.Executor

/** Direct Shizuku Binder transport: does not start a UserService or create a LoadedApk Application. */
class DirectWindowBridge(private val context: Context) {
    private var display: VirtualDisplay? = null
    private var displayId = -1
    private var input: Any? = null
    @Volatile var stage = "S00 · préparation"
        private set

    private fun call(type: Class<*>, receiver: Any?, name: String, vararg args: Any?): Any? {
        try { return HiddenApiBypass.invoke(type,receiver,name,*args) }
        catch(e: InvocationTargetException) { throw e.targetException }
    }
    private fun service(name: String, stub: String): Any {
        check(Shizuku.pingBinder()) { "Shizuku n’est plus démarré" }
        val original=call(Class.forName("android.os.ServiceManager"),null,"getService",name) as? IBinder
            ?: error("Service Android absent : $name")
        return call(Class.forName(stub),null,"asInterface",ShizukuBinderWrapper(original))
            ?: error("Interface Android absente : $name")
    }
    private fun methods(type: Class<*>) = HiddenApiBypass.getDeclaredMethods(type).filterIsInstance<Method>()

    @android.annotation.SuppressLint("WrongConstant")
    @android.annotation.TargetApi(34)
    @Synchronized fun open(surface: Surface,width:Int,height:Int,dpi:Int,component:String,progress:(String)->Unit):Int {
        check(Build.VERSION.SDK_INT>=34) { "Le cadre Shizuku direct nécessite Android 14 ou supérieur" }
        val target=ComponentName.unflattenFromString(component) ?: error("Application invalide")
        fun step(value:String) { stage=value;progress(value) }
        close()
        try {
            step("S01 · connexion directe à Shizuku…")
            val dm=service("display","android.hardware.display.IDisplayManager\$Stub")
            val globalClass=Class.forName("android.hardware.display.DisplayManagerGlobal")
            // Separate manager: never replace the app's DisplayManagerGlobal singleton.
            val global=HiddenApiBypass.newInstance(globalClass,dm)
            val config=VirtualDisplayConfig.Builder("FloatX",width,height,dpi)
                .setFlags(1 or 2 or 8 or 256).setSurface(surface).build()
            val shellContext=context.createPackageContext("com.android.shell",0)
            val create=methods(globalClass).firstOrNull { method ->
                method.name=="createVirtualDisplay" && method.parameterTypes.firstOrNull()==Context::class.java &&
                    method.parameterTypes.contains(VirtualDisplayConfig::class.java)
            } ?: error("Signature de création d’écran non prise en charge")
            val args=create.parameterTypes.map { type -> when {
                type==Context::class.java -> shellContext
                type==VirtualDisplayConfig::class.java -> config
                Executor::class.java.isAssignableFrom(type) -> Executor { command -> command.run() }
                type==Int::class.javaPrimitiveType -> 0
                else -> null
            } }.toTypedArray()
            step("S02 · création de l’écran dans le cadre…")
            create.isAccessible=true
            display=create.invoke(global,*args) as? VirtualDisplay ?: error("Android a refusé l’écran virtuel")
            displayId=display!!.display.displayId
            step("S03 · ouverture de l’application…")
            val task=service("activity_task","android.app.IActivityTaskManager\$Stub")
            val taskClass=Class.forName("android.app.IActivityTaskManager")
            val start=methods(taskClass).firstOrNull { it.name=="startActivity" && it.parameterTypes.contains(Intent::class.java) }
                ?: error("Interface de lancement non prise en charge")
            val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            val activityOptions=ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
            call(ActivityOptions::class.java,activityOptions,"setLaunchWindowingMode",1)
            val options=activityOptions.toBundle()
            val launchArgs=ActivityStartCall.arguments(start.parameterTypes.map { it.name },intent,options)
            start.isAccessible=true
            val result=start.invoke(task,*launchArgs) as Int
            check(result>=0) { "Android a refusé le lancement (code $result)" }
            step("S04 · connexion du tactile…")
            input=service("input","android.hardware.input.IInputManager\$Stub")
            step("S05 · fenêtre prête")
            return displayId
        } catch(e:Throwable) {
            val cause=if(e is InvocationTargetException) e.targetException else e
            val code=stage.substringBefore(" ·")
            close()
            if(cause is VirtualMachineError || cause is ThreadDeath) throw cause
            throw IllegalStateException("$code · ${cause.message ?: cause.javaClass.simpleName}",cause)
        }
    }
    private fun inject(event:InputEvent) {
        check(displayId>=0) { "Aucune fenêtre active" }
        call(InputEvent::class.java,event,"setDisplayId",displayId)
        val target=input ?: error("Tactile non connecté")
        val type=Class.forName("android.hardware.input.IInputManager")
        val success=try { call(type,target,"injectInputEventToTarget",event,0,-1) }
            catch(_:NoSuchMethodException) { call(type,target,"injectInputEvent",event,0) }
        check(success==true) { "Interaction refusée par Android" }
    }
    @Synchronized fun touch(event:MotionEvent):Boolean { inject(event);return true }
    @Synchronized fun resize(width:Int,height:Int,dpi:Int) { display?.resize(width,height,dpi) }
    @Synchronized fun sendText(text:String) {
        require(text.length<=1000) { "1000 caractères maximum" }
        val keys=KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(text.toCharArray())
            ?: error("Caractères non pris en charge par la saisie de test")
        keys.forEach { inject(it) }
    }
    @Synchronized fun back() {
        val now=SystemClock.uptimeMillis()
        for(action in intArrayOf(KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP)) inject(KeyEvent(now,now,action,KeyEvent.KEYCODE_BACK,
            0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,InputDevice.SOURCE_KEYBOARD))
    }
    @Synchronized fun close() {
        val previous=display
        display=null;displayId=-1;input=null
        try { previous?.release() } catch(_:Exception) { }
    }
}
