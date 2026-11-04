package com.josias.floatx

import android.annotation.TargetApi
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/** Focus for display-0 navigation only. Never becomes an IME editor or covers its keyboard. */
@TargetApi(33)
class BackRouter(private val context:Context,private val wm:WindowManager,private val back:()->Unit) {
    private var anchor:View?=null
    fun activate() {
        if(anchor?.hasWindowFocus()==true) return
        hide()
        val callback=OnBackInvokedCallback { back() }
        var dispatcher:OnBackInvokedDispatcher?=null
        val view=object:View(context) {
            override fun dispatchKeyEvent(event:KeyEvent):Boolean {
                if(event.keyCode!=KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
                if(event.action==KeyEvent.ACTION_UP && !event.isCanceled) back()
                return true
            }
        }.apply { isFocusableInTouchMode=true }
        view.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v:View) {
                dispatcher=v.findOnBackInvokedDispatcher()
                dispatcher?.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,callback)
            }
            override fun onViewDetachedFromWindow(v:View) {
                dispatcher?.unregisterOnBackInvokedCallback(callback);dispatcher=null
            }
        })
        val params=WindowManager.LayoutParams(1,1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START }
        anchor=view
        try { wm.addView(view,params);view.requestFocus() }
        catch(e:RuntimeException) { anchor=null;throw e }
    }
    fun hide() {
        anchor?.let { if(it.isAttachedToWindow) wm.removeViewImmediate(it) };anchor=null
    }
}
