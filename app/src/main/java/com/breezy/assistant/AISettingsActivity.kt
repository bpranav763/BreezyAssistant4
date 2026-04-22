package com.breezy.assistant

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout

class AISettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@AISettingsActivity))
        }
        root.addView(buildHeader("🧠 AI Settings") { finish() })
        
        // This is a stub, but we can point to BreezySettingsActivity for now or add AI specific ones
        // Actually, BreezySettingsActivity already handles most AI things.
        
        setContentView(root)
        applySystemBarInsets(root)
        
        // For now, just redirect or show a message
        // startActivity(Intent(this, BreezySettingsActivity::class.java))
        // finish()
    }
}
