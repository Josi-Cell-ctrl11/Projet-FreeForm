package com.josias.floatx

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout

/** First-party probe can observe its own multi-window state without inspecting other apps. */
class FreeformProbeActivity : Activity() {
    private lateinit var result: android.widget.TextView
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val page = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;setPadding(Ui.dp(this@FreeformProbeActivity,20),Ui.dp(this@FreeformProbeActivity,24),Ui.dp(this@FreeformProbeActivity,20),Ui.dp(this@FreeformProbeActivity,24))
        }
        page.addView(Ui.text(this,"Test de fenêtre Android",24f,bold=true))
        result=Ui.text(this,"Vérification…",16f)
        page.addView(result)
        page.addView(Ui.button(this,"Fermer le test") { finishAndRemoveTask() })
        setContentView(page)
    }
    override fun onResume() { super.onResume(); updateResult() }
    override fun onMultiWindowModeChanged(inMultiWindowMode: Boolean, config: android.content.res.Configuration) {
        super.onMultiWindowModeChanged(inMultiWindowMode,config);updateResult()
    }
    private fun updateResult() {
        if (!::result.isInitialized) return
        val text=if (isInMultiWindowMode) "Test interne : mode multi-fenêtre détecté ✓\nEssaie maintenant Calculatrice depuis FloatX."
            else "Test interne : Android a ouvert ce test en plein écran.\nReviens dans FloatX et change l’option de compatibilité, puis réessaie."
        result.text=text
        NativeFreeformLauncher.record(this,text)
    }
}
