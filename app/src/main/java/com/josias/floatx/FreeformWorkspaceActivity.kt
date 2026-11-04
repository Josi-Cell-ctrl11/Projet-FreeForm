package com.josias.floatx

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

/** A separate resizable task prepares the native freeform workspace, as in Taskbar's launch sequence. */
class FreeformWorkspaceActivity : Activity() {
    private val main = Handler(Looper.getMainLooper())
    private var launched = false
    private val launchTarget = Runnable {
        if (isFinishing || isDestroyed || launched) return@Runnable
        val target = intent.getStringExtra(NativeFreeformLauncher.EXTRA_COMPONENT)
            ?.let(ComponentName::unflattenFromString)
        if (target == null) { finishAndRemoveTask(); return@Runnable }
        launched = true
        try {
            NativeFreeformLauncher.launchTarget(this,target,
                intent.getStringExtra(NativeFreeformLauncher.EXTRA_LABEL) ?: "Application",
                intent.getBooleanExtra(NativeFreeformLauncher.EXTRA_LARGE,false))
        } catch (e: Exception) {
            val message = "N02 · Lancement refusé : ${e.message ?: e.javaClass.simpleName}"
            NativeFreeformLauncher.record(this,message)
            AlertDialog.Builder(this).setMessage(message).setPositiveButton("Fermer") { _,_ -> finishAndRemoveTask() }.show()
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        launched = state?.getBoolean("launched") ?: false
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        setFinishOnTouchOutside(false)
    }
    override fun onResume() {
        super.onResume()
        if (!launched) main.postDelayed(launchTarget,300)
    }
    override fun onPause() { main.removeCallbacks(launchTarget); super.onPause() }
    override fun onSaveInstanceState(state: Bundle) { state.putBoolean("launched",launched); super.onSaveInstanceState(state) }
    override fun onDestroy() { main.removeCallbacksAndMessages(null); super.onDestroy() }
}
