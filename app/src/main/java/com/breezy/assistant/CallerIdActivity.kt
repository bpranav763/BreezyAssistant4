package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class CallerIdActivity : BaseActivity() {

    private val spamDb by lazy { SpamDatabase(this) }
    private lateinit var statusText: TextView
    private lateinit var switchBtn: TextView
    private lateinit var lastCallText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        root.addView(buildHeader("📞 Caller ID") { finish() })

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(32))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt()); cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(24), dp(24), dp(24), dp(24))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(24)) }
            
            addView(TextView(this@CallerIdActivity).apply {
                text = "🛡️ Breezy Shield"
                textSize = 18f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(8))
            })

            statusText = TextView(this@CallerIdActivity).apply {
                text = "Breezy is scanning incoming calls for spam and fraud."
                textSize = 13f; setTextColor(0xFF9CA3AF.toInt())
                setPadding(0, 0, 0, dp(20))
            }
            addView(statusText)

            switchBtn = TextView(this@CallerIdActivity).apply {
                text = "✅ Shield Active"
                textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(0xFF065F46.toInt()); cornerRadius = dp(12).toFloat()
                }
                setPadding(0, dp(14), 0, dp(14))
                setOnClickListener { toggleShield() }
            }
            addView(switchBtn)
        }
        container.addView(card)

        container.addView(TextView(this).apply {
            text = "RECENT PROTECTION"
            textSize = 11f; setTextColor(0xFF4B5563.toInt()); letterSpacing = 0.15f
            setPadding(0, dp(8), 0, dp(16))
        })

        lastCallText = TextView(this).apply {
            text = "No spam calls detected yet today."
            textSize = 14f; setTextColor(0xFF6B7280.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(48), 0, dp(48))
        }
        container.addView(lastCallText)

        root.addView(container)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun toggleShield() {
        val active = switchBtn.text.contains("Active")
        if (active) {
            switchBtn.text = "❌ Shield Inactive"
            switchBtn.background = GradientDrawable().apply {
                setColor(0xFF1F2937.toInt()); cornerRadius = dp(12).toFloat()
            }
            statusText.text = "Spam protection is currently disabled."
        } else {
            switchBtn.text = "✅ Shield Active"
            switchBtn.background = GradientDrawable().apply {
                setColor(0xFF065F46.toInt()); cornerRadius = dp(12).toFloat()
            }
            statusText.text = "Breezy is scanning incoming calls for spam and fraud."
        }
    }
}
