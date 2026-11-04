package com.josias.floatx

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

object Ui {
    const val GREEN = 0xFF16845B.toInt()
    const val INK = 0xFF202B25.toInt()
    const val MUTED = 0xFF69746D.toInt()
    fun dp(c: Context, value: Int) = (value * c.resources.displayMetrics.density).toInt()
    fun rounded(c: Context, color: Int, radius: Int = 24) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(c, radius).toFloat()
    }
    fun text(c: Context, value: String, size: Float = 16f, color: Int = INK, bold: Boolean = false) = TextView(c).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    fun button(c: Context, label: String, action: () -> Unit) = Button(c).apply {
        text = label; isAllCaps = false; textSize = 15f; setTextColor(Color.WHITE)
        background = rounded(c, GREEN, 16); setPadding(dp(c,16), dp(c,10), dp(c,16), dp(c,10))
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(c,54)).apply { topMargin = dp(c,12) }
    }
    fun icon(c: Context, label: String, description: String, action: () -> Unit) = TextView(c).apply {
        text = label; textSize = 22f; gravity = Gravity.CENTER; setTextColor(INK)
        contentDescription = description; setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(dp(c,46), dp(c,46))
    }
}
