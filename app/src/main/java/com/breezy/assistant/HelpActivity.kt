package com.breezy.assistant

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class HelpActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@HelpActivity))
        }
        root.addView(buildHeader("❓ Help & Support") { finish() })

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }

        val faqs = listOf(
            "What is Breezy?" to "Breezy is your private AI protective companion, designed to keep your phone healthy and your data safe.",
            "Is my data private?" to "Yes! Breezy processes most data locally. For cloud AI, only the specific query is sent to our secure partners (Gemini/Groq).",
            "How do I use E-Flare?" to "Go to Security -> E-Flare to set up emergency contacts. It triggers automatically when your battery hits 5%.",
            "What is Ghost Hand?" to "It's an accessibility feature that allows Breezy to help you type and send messages in apps like WhatsApp."
        )

        faqs.forEach { (q, a) ->
            container.addView(TextView(this).apply {
                text = q
                textSize = 16f
                setTextColor(ThemeManager.getAccentColor(this@HelpActivity))
                setPadding(0, dp(16), 0, dp(4))
            })
            container.addView(TextView(this).apply {
                text = a
                textSize = 14f
                setTextColor(ThemeManager.getTextPrimary(this@HelpActivity))
                setPadding(0, 0, 0, dp(8))
            })
        }

        setContentView(root.apply { 
            addView(ScrollView(context).apply { addView(container) })
        })
        applySystemBarInsets(root)
    }
}
