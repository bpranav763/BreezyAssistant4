package com.breezy.assistant

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class VoiceSettingsActivity : BaseActivity() {

    private val memory by lazy { BreezyMemory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@VoiceSettingsActivity))
        }
        root.addView(buildHeader("🎤 Voice & Wake") { finish() })

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }

        // Voice Wake Toggle
        val wakeSwitch = Switch(this).apply {
            text = "Voice Wake (Hey Breezy)"
            setTextColor(ThemeManager.getTextPrimary(this@VoiceSettingsActivity))
            isChecked = memory.getFact("voice_wake_enabled") == "true"
            setPadding(dp(8), dp(16), dp(8), dp(16))
            setOnCheckedChangeListener { _, isChecked ->
                memory.saveFact("voice_wake_enabled", if (isChecked) "true" else "false")
                val intent = Intent(this@VoiceSettingsActivity, VoiceWakeService::class.java)
                if (isChecked) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                } else {
                    stopService(intent)
                }
            }
        }
        container.addView(wakeSwitch)
        
        container.addView(TextView(this).apply {
            text = "Enabling this will allow Breezy to listen for your wake word even when the screen is off (if charging) or when using other apps."
            textSize = 12f
            setTextColor(ThemeManager.getTextSecondary(this@VoiceSettingsActivity))
            setPadding(dp(8), 0, dp(8), dp(16))
        })

        setContentView(root.apply { addView(container) })
        applySystemBarInsets(root)
    }
}
