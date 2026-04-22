package com.breezy.assistant

import android.os.Bundle
import android.widget.LinearLayout

class CallScreeningActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@CallScreeningActivity))
        }
        root.addView(buildHeader("📞 Call Screening") { finish() })
        // TODO: Add call screening UI
        setContentView(root)
        applySystemBarInsets(root)
    }
}
