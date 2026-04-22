package com.breezy.assistant

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class PrivacyPolicyActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@PrivacyPolicyActivity))
        }
        root.addView(buildHeader("🛡️ Privacy Policy") { finish() })

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }

        val policy = """
            Privacy is our core value.
            
            1. Local Processing: Most of your data, including notes, triggers, and health stats, never leaves your device.
            
            2. AI Queries: When using Cloud AI (Gemini/Groq), we send only the current chat message. No personal identifying info is attached.
            
            3. Permissions: We ask only for permissions needed to function. You can revoke them anytime in Settings.
            
            4. No Ads: Breezy is 100% ad-free and does not track you for marketing.
        """.trimIndent()

        container.addView(TextView(this).apply {
            text = policy
            textSize = 14f
            setTextColor(ThemeManager.getTextPrimary(this@PrivacyPolicyActivity))
            setLineSpacing(0f, 1.3f)
        })

        setContentView(root.apply { 
            addView(ScrollView(context).apply { addView(container) })
        })
        applySystemBarInsets(root)
    }
}
