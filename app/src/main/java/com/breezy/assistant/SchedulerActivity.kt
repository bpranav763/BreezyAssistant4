package com.breezy.assistant

import android.os.Bundle
import android.widget.LinearLayout

class SchedulerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@SchedulerActivity))
        }
        root.addView(buildHeader("⏰ Scheduler") { finish() })
        // TODO: Add scheduling UI
        setContentView(root)
        applySystemBarInsets(root)
    }
}
