package com.josias.floatx

import android.app.Activity
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.WindowInsets
import android.view.WindowManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

/** Requests Android's existing freeform mode. It does not host, mirror or inject input into apps. */
object NativeFreeformLauncher {
    private const val PREFS = "native-launch"
    const val EXTRA_COMPONENT = "target_component"
    const val EXTRA_LABEL = "target_label"
    const val EXTRA_LARGE = "large_window"

    data class Options(val options: ActivityOptions, val modeRequested: Boolean, val detail: String)

    fun bounds(context: Context, large: Boolean): Rect {
        val metrics = context.getSystemService(WindowManager::class.java).maximumWindowMetrics
        val inset = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        val box = NativeWindowBounds.initial(metrics.bounds.width(),metrics.bounds.height(),
            inset.left,inset.top,inset.right,inset.bottom,context.resources.displayMetrics.density,large)
        return Rect(box.x,box.y,box.x+box.width,box.y+box.height)
    }

    fun options(rect: Rect): Options {
        val options = ActivityOptions.makeBasic().setLaunchBounds(rect)
        return try {
            // Only invoke ActivityOptions' mode setter; no global hidden-API exemption or privilege grant.
            HiddenApiBypass.invoke(ActivityOptions::class.java,options,"setLaunchWindowingMode",5)
            Options(options,true,"Mode fenêtres libres demandé")
        } catch (e: ReflectiveOperationException) {
            Options(options,false,"N01 · Taille demandée ; accès au mode libre indisponible (${e.javaClass.simpleName})")
        } catch (e: RuntimeException) {
            Options(options,false,"N01 · Taille demandée ; accès au mode libre refusé (${e.javaClass.simpleName})")
        } catch (e: LinkageError) {
            Options(options,false,"N01 · Compatibilité Android indisponible (${e.javaClass.simpleName})")
        }
    }

    fun launch(activity: Activity, component: ComponentName, label: String, compatibility: Boolean, large: Boolean) {
        if (compatibility) {
            val metrics = activity.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            val anchor = options(Rect(metrics.width(),metrics.height(),metrics.width()+1,metrics.height()+1))
            val intent = Intent(activity,FreeformWorkspaceActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(EXTRA_COMPONENT,component.flattenToString()).putExtra(EXTRA_LABEL,label).putExtra(EXTRA_LARGE,large)
            record(activity,"Préparation de l’espace multi-fenêtre · $label\n${anchor.detail}")
            activity.startActivity(intent,anchor.options.toBundle())
        } else launchTarget(activity,component,label,large)
        activity.moveTaskToBack(true)
    }

    fun launchTarget(activity: Activity, component: ComponentName, label: String, large: Boolean) {
        val request = options(bounds(activity,large))
        val target = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(target,request.options.toBundle())
        record(activity,"Dernier essai : $label\n${request.detail}. Ouverture à vérifier sur le téléphone.")
    }

    fun record(context: Context, message: String) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("last",message).apply()
    }
    fun last(context: Context): String = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        .getString("last","Aucun essai de fenêtre système pour le moment.")!!

    fun stopWorkspace(activity: Activity) {
        val manager = activity.getSystemService(android.app.ActivityManager::class.java)
        manager.appTasks.forEach { task ->
            val base = task.taskInfo.baseIntent.component
            if (base?.className == FreeformWorkspaceActivity::class.java.name || base?.className == FreeformProbeActivity::class.java.name)
                task.finishAndRemoveTask()
        }
    }
}
