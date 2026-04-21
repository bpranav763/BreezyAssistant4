package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class InbuiltAppsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        root.addView(buildHeader("📦 Inbuilt Apps") { finish() })

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }
        
        val grid = GridLayout(this).apply {
            columnCount = 2
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }

        val apps = listOf(
            AppInfo("📝", "Notes", NotesActivity::class.java),
            AppInfo("📱", "Startup Notes", AppNoteActivity::class.java),
            AppInfo("🛡️", "Privacy Filter", PrivacyScreenActivity::class.java),
            AppInfo("📢", "E-Flare", EFlareActivity::class.java),
            AppInfo("⚡", "Triggers", TriggerActivity::class.java),
            AppInfo("🔐", "Security Vault", VaultActivity::class.java),
            AppInfo("📡", "Hotspot", HotspotBridgeActivity::class.java),
            AppInfo("📊", "App Usage", AppUsageActivity::class.java),
            AppInfo("🚀", "Speed Test", SpeedTestActivity::class.java),
            AppInfo("🕵️", "Anti-Stalker", AntiStalkerActivity::class.java)
        )

        apps.forEach { app ->
            grid.addView(buildAppTile(app.icon, app.name) {
                startActivity(Intent(this, app.activityClass))
            })
        }

        scroll.addView(grid)
        root.addView(scroll)

        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun buildAppTile(icon: String, name: String, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(16), dp(20), dp(16), dp(20))
            setOnClickListener { onClick() }
            
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(6), dp(6), dp(6), dp(6))
            }
            layoutParams = params

            addView(TextView(context).apply {
                text = icon
                textSize = 32f
                setPadding(0, 0, 0, dp(8))
            })
            addView(TextView(context).apply {
                text = name
                textSize = 11f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
    }

    data class AppInfo(val icon: String, val name: String, val activityClass: Class<*>)
}
