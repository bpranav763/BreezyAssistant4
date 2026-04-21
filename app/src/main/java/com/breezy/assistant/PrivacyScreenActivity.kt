package com.breezy.assistant

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.core.content.ContextCompat

class PrivacyScreenActivity : BaseActivity() {

    private val memory by lazy { BreezyMemory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        // Header
        root.addView(buildHeader("🛡️ Privacy Screen") { finish() })

        // Description
        root.addView(TextView(this).apply {
            text = "Blocks visibility from side angles using a dark gradient overlay. Perfect for public transport."
            setTextColor(0xFF9CA3AF.toInt())
            textSize = 14f
            setPadding(0, dp(16), 0, dp(24))
        })

        // Intensity Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111827.toInt())
            setPadding(dp(20), dp(20), dp(20), dp(20))
            val shape = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
            background = shape
        }

        card.addView(TextView(this).apply {
            text = "FILTER INTENSITY"
            setTextColor(0xFF4B5563.toInt())
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.15f
        })

        val intensityValueText = TextView(this).apply {
            text = "Medium (60%)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, dp(8), 0, dp(16))
        }
        card.addView(intensityValueText)

        val seekBar = SeekBar(this).apply {
            max = 100
            progress = 60 // Default
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val label = when {
                        progress < 33 -> "Low (${progress}%)"
                        progress < 66 -> "Medium (${progress}%)"
                        else -> "High (${progress}%)"
                    }
                    intensityValueText.text = label
                    // Update service if running
                    if (isServiceRunning(PrivacyScreenService::class.java)) {
                        startService(Intent(this@PrivacyScreenActivity, PrivacyScreenService::class.java).apply {
                            putExtra("intensity", progress)
                        })
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        card.addView(seekBar)

        root.addView(card)

        // Toggle Button
        val toggleBtn = Button(this).apply {
            val isRunning = isServiceRunning(PrivacyScreenService::class.java)
            text = if (isRunning) "STOP PRIVACY SCREEN" else "START PRIVACY SCREEN"
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val bg = GradientDrawable().apply {
                setColor(if (isRunning) 0xFFEF4444.toInt() else 0xFF1D4ED8.toInt())
                cornerRadius = dp(12).toFloat()
            }
            background = bg
            
            setOnClickListener {
                if (isServiceRunning(PrivacyScreenService::class.java)) {
                    stopService(Intent(this@PrivacyScreenActivity, PrivacyScreenService::class.java))
                    text = "START PRIVACY SCREEN"
                    bg.setColor(0xFF1D4ED8.toInt())
                } else {
                    val intent = Intent(this@PrivacyScreenActivity, PrivacyScreenService::class.java).apply {
                        putExtra("intensity", seekBar.progress)
                    }
                    ContextCompat.startForegroundService(this@PrivacyScreenActivity, intent)
                    text = "STOP PRIVACY SCREEN"
                    bg.setColor(0xFFEF4444.toInt())
                }
            }
        }
        
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56)
        ).apply {
            topMargin = dp(32)
        }
        root.addView(toggleBtn, btnParams)

        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
