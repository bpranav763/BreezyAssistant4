package com.breezy.assistant

import android.os.Bundle
import android.widget.LinearLayout

class HardwareInspectorActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@HardwareInspectorActivity))
        }
        root.addView(buildHeader("🔧 Hardware Inspector") { finish() })
        // TODO: Display hardware info from HardwareInspector
        setContentView(root)
        applySystemBarInsets(root)
    }
}
