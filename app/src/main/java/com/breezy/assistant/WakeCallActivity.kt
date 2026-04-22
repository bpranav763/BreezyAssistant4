package com.breezy.assistant

import android.os.Bundle
import android.widget.LinearLayout

class WakeCallActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@WakeCallActivity))
        }
        root.addView(buildHeader("⏰ Wake Up Call") { finish() })
        // TODO: Add alarm + TTS message UI
        setContentView(root)
        applySystemBarInsets(root)
    }
}
