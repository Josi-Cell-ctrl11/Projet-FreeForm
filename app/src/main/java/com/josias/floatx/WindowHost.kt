package com.josias.floatx

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import java.util.concurrent.TimeUnit

/** Runs only in a user-authorized Shizuku shell process. No app-data access. */
class WindowHost(@Suppress("UNUSED_PARAMETER") context: Context) : IWindowHost.Stub() {
    private val main = Handler(Looper.getMainLooper())
    private var display: VirtualDisplay? = null
    private var id = -1

    private inline fun <T> privileged(block: () -> T): T {
        val token = Binder.clearCallingIdentity()
        try { return block() } finally { Binder.restoreCallingIdentity(token) }
    }

    // Flag 256 is the AOSP system flag DESTROY_CONTENT_ON_REMOVAL, absent from the public SDK IntDef.
    @SuppressLint("WrongConstant")
    @Synchronized override fun open(surface: Surface, width: Int, height: Int, dpi: Int, component: String): Int = privileged {
        require(component.matches(Regex("[A-Za-z0-9_.$]+/[A-Za-z0-9_.$]+"))) { "Composant invalide" }
        close()
        // Use the shell package for attribution: the process has shell UID, not app UID.
        val threadClass = Class.forName("android.app.ActivityThread")
        val thread = threadClass.getMethod("currentActivityThread").invoke(null)
            ?: threadClass.getMethod("systemMain").invoke(null)
        val system = threadClass.getMethod("getSystemContext").invoke(thread) as Context
        val shell = system.createPackageContext("com.android.shell", 0)
        val manager = shell.getSystemService(DisplayManager::class.java)
        // PUBLIC | PRESENTATION | OWN_CONTENT_ONLY | DESTROY_CONTENT_ON_REMOVAL.
        // No mirroring, secure-content capture or trusted-display permission is requested.
        display = manager.createVirtualDisplay("FloatX", width, height, dpi, surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or 256, null, main)
            ?: error("XOS a refusé la création de l'écran virtuel")
        id = display!!.display.displayId
        try {
            val process = ProcessBuilder("am", "start", "--display", id.toString(),
                "--activity-new-task", "--activity-multiple-task", "-n", component)
                .redirectErrorStream(true).start()
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroyForcibly(); error("Délai dépassé au lancement de l'appli")
            }
            val output = process.inputStream.bufferedReader().readText()
            check(process.exitValue() == 0 && !output.contains("Error:") && !output.contains("Exception")) { output }
            id
        } catch (e: Exception) { close(); throw IllegalStateException(e.message, e) }
    }

    private fun inject(event: InputEvent) = privileged {
        check(id >= 0) { "Aucune fenêtre active" }
        InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(event, id)
        val clazz = try { Class.forName("android.hardware.input.InputManagerGlobal") }
            catch (_: ClassNotFoundException) { Class.forName("android.hardware.input.InputManager") }
        val manager = clazz.getMethod("getInstance").invoke(null)
        val result = clazz.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
            .invoke(manager, event, 0)
        check(result != false) { "Injection tactile refusée par le système" }
    }

    @Synchronized override fun resize(width: Int, height: Int, dpi: Int) {
        privileged { display?.resize(width, height, dpi) }
    }

    @Synchronized override fun touch(event: MotionEvent): Boolean {
        return try { inject(event); true }
        catch (e: Exception) { android.util.Log.e("FloatXHost", "Input rejected", e); false }
        finally { event.recycle() }
    }

    @Synchronized override fun sendText(text: String) {
        require(text.length <= 1000) { "Texte trop long (1000 caractères maximum)" }
        val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(text.toCharArray())
            ?: error("Ces caractères ne sont pas pris en charge par le clavier de test")
        for (event in events) inject(event)
    }

    @Synchronized override fun back() {
        val now = SystemClock.uptimeMillis()
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            inject(KeyEvent(now, now, action, KeyEvent.KEYCODE_BACK, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD))
        }
    }

    @Synchronized override fun close() { privileged { display?.release(); display = null; id = -1 } }
    override fun destroy() { close(); System.exit(0) }
}
