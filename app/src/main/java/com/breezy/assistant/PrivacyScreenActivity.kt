package com.breezy.assistant

import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class PrivacyScreenActivity : BaseActivity() {
    private val prefs by lazy { getSharedPreferences("privacy_prefs", MODE_PRIVATE) }
    private var isActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
        }
        root.addView(buildHeader("🛡️ Privacy Screen") { finish() })

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            gravity = Gravity.CENTER_VERTICAL
        }
        val statusText = TextView(this).apply {
            text = "Privacy Filter"
            textSize = 16f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        
        isActive = PrivacyScreenService.isRunning
        
        val toggle = Switch(this).apply {
            isChecked = isActive
            setOnCheckedChangeListener { _, isChecked ->
                isActive = isChecked
                if (isChecked) {
                    startPrivacyService()
                } else {
                    stopPrivacyService()
                }
            }
        }
        statusCard.addView(statusText)
        statusCard.addView(toggle)
        content.addView(statusCard)

        content.addView(TextView(this).apply {
            text = "INTENSITY"
            textSize = 11f
            setTextColor(0xFF4B5563.toInt())
            letterSpacing = 0.15f
            setPadding(0, dp(24), 0, dp(8))
        })

        val seekBar = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("intensity", 80)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                    prefs.edit().putInt("intensity", p).apply()
                    if (isActive) {
                        updatePrivacyService(p)
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        content.addView(seekBar)

        val intensityLabel = TextView(this).apply {
            text = "Low                          Medium                          High"
            textSize = 10f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, dp(4), 0, dp(16))
        }
        content.addView(intensityLabel)

        content.addView(TextView(this).apply {
            text = "This creates a gradient overlay that darkens the screen edges, making it harder for people next to you to see your screen."
            textSize = 12f
            setTextColor(0xFF9CA3AF.toInt())
            setLineSpacing(0f, 1.4f)
        })

        root.addView(ScrollView(this).apply { addView(content) })
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun startPrivacyService() {
        val intent = Intent(this, PrivacyScreenService::class.java)
        intent.putExtra("intensity", prefs.getInt("intensity", 80))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopPrivacyService() {
        stopService(Intent(this, PrivacyScreenService::class.java))
    }

    private fun updatePrivacyService(intensity: Int) {
        val intent = Intent(this, PrivacyScreenService::class.java)
        intent.putExtra("intensity", intensity)
        startService(intent)
    }
}
