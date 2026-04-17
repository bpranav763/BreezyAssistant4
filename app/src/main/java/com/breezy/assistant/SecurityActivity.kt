package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class SecurityActivity : BaseActivity() {

    private val battery by lazy { BatteryMonitor(this) }
    private val wifi by lazy { WifiSecurityMonitor(this) }
    private val memory by lazy { BreezyMemory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(buildHeader("🛡️ Security") { finish() })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(32))
        }

        // Threat Status
        container.addView(buildSectionLabel("THREAT STATUS"))
        container.addView(buildThreatStatusCard())

        // Quick Actions
        container.addView(buildSectionLabel("ACTIVE PROTECTION"))
        container.addView(buildQuickActions())

        // Scanner Button
        container.addView(TextView(this).apply {
            text = "🕵️ Anti-Stalkerware Scan →"
            textSize = 14f
            setTextColor(0xFF1D4ED8.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(24), dp(20), dp(24), dp(20))
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, dp(16), 0, dp(16)) }
            setOnClickListener {
                startActivity(Intent(this@SecurityActivity, AntiStalkerActivity::class.java))
            }
        })

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun buildThreatStatusCard(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(24), dp(20), dp(24), dp(20))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dp(24)) }
        }

        val wifiResult = wifi.analyzeCurrentNetwork()
        val data = battery.getBatteryData()

        val statusItems = listOf(
            Triple(if (wifiResult == WifiSecurityMonitor.SecurityResult.SECURE) "✅" else "⚠️", "WiFi Network", if (wifiResult == WifiSecurityMonitor.SecurityResult.SECURE) "Secure" else "Unsafe"),
            Triple(if (battery.isChargerSafe()) "✅" else "⚠️", "Charger Safety", if (battery.isChargerSafe()) "Normal" else "Unstable"),
            Triple(if (data.temperature < 42f) "✅" else "🔥", "Temperature", "${data.temperature}°C")
        )

        statusItems.forEach { (icon, title, desc) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(10))
            }
            row.addView(TextView(this).apply { text = icon; textSize = 18f; setPadding(0, 0, dp(16), 0) })
            
            val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            textCol.addView(TextView(this).apply { text = title; textSize = 14f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD })
            textCol.addView(TextView(this).apply { text = desc; textSize = 12f; setTextColor(0xFF6B7280.toInt()) })
            
            row.addView(textCol)
            layout.addView(row)
        }

        return layout
    }

    private fun buildQuickActions(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(24)) }
            
            val actions = listOf("🔕" to "DND", "📶" to "WiFi", "🔵" to "BT")
            actions.forEach { (icon, label) ->
                addView(LinearLayout(this@SecurityActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply { setColor(0xFF111827.toInt()); cornerRadius = dp(12).toFloat() }
                    setPadding(dp(12), dp(16), dp(12), dp(16))
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f).also { it.setMargins(dp(4), 0, dp(4), 0) }
                    
                    addView(TextView(this@SecurityActivity).apply { text = icon; textSize = 22f })
                    addView(TextView(this@SecurityActivity).apply { text = label; textSize = 10f; setTextColor(0xFF9CA3AF.toInt()) })
                })
            }
        }
    }

    private fun buildSectionLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(0xFF4B5563.toInt())
            letterSpacing = 0.15f
            setPadding(0, dp(8), 0, dp(12))
        }
    }
}
