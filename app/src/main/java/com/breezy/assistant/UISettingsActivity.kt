package com.breezy.assistant

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class UISettingsActivity : BaseActivity() {

    private val memory by lazy { BreezyMemory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@UISettingsActivity))
        }
        root.addView(buildHeader("🎨 UI Settings") { finish() })

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }

        container.addView(TextView(this).apply {
            text = "THEME SELECTION"
            textSize = 11f
            setTextColor(ThemeManager.getAccentColor(this@UISettingsActivity))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(12))
        })

        val themes = ThemeManager.Theme.values()
        themes.forEach { theme ->
            container.addView(buildThemeOption(theme))
        }

        setContentView(root.apply { 
            val scroll = ScrollView(context).apply { addView(container) }
            addView(scroll)
        })
        applySystemBarInsets(root)
    }

    private fun buildThemeOption(theme: ThemeManager.Theme): LinearLayout {
        val current = ThemeManager.getCurrentTheme(this)
        val isSelected = current == theme

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@UISettingsActivity))
                cornerRadius = dp(12).toFloat()
                if (isSelected) setStroke(dp(2), ThemeManager.getAccentColor(this@UISettingsActivity))
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }

            addView(TextView(context).apply {
                text = theme.name.replace("_", " ")
                textSize = 15f
                setTextColor(ThemeManager.getTextPrimary(this@UISettingsActivity))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })

            if (isSelected) {
                addView(TextView(context).apply {
                    text = "✓"
                    setTextColor(ThemeManager.getAccentColor(this@UISettingsActivity))
                })
            }

            setOnClickListener {
                ThemeManager.applyTheme(this@UISettingsActivity, theme)
            }
        }
    }
}
