package com.breezy.assistant

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow content to draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Make status bar and nav bar transparent
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    // Call this on your root view after setContentView
    protected fun applySystemBarInsets(rootView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(
                top = systemBars.top,
                bottom = maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
    }

    protected fun setupHeader(title: String) {
        val content = findViewById<ViewGroup>(android.R.id.content)
        val child = content.getChildAt(0)
        if (child != null) {
            content.removeView(child)
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF0A0F1E.toInt())
                addView(buildHeader(title) { finish() })
                addView(child, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ))
            }
            setContentView(root)
            applySystemBarInsets(root)
        }
    }

    protected fun buildHeader(title: String, onBack: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), 0, dp(24), dp(16))
            setBackgroundColor(0xFF0A0F1E.toInt())

            addView(TextView(this@BaseActivity).apply {
                text = "←"
                textSize = 22f
                setTextColor(Color.WHITE)
                setPadding(0, 0, dp(20), 0)
                setOnClickListener { onBack() }
            })
            addView(TextView(this@BaseActivity).apply {
                text = title
                textSize = 20f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
    }

    protected fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
