package com.breezy.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class EFlareActivity : BaseActivity() {

    private val memory by lazy { BreezyMemory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        root.addView(buildHeader("📢 E-Flare Settings") { finish() })

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Info Card
        val infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111827.toInt())
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
        }
        infoCard.addView(TextView(this).apply {
            text = "What is E-Flare?"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        infoCard.addView(TextView(this).apply {
            text = "When your battery hits 5%, Breezy will automatically send your last known location to your emergency contacts via SMS."
            setTextColor(0xFF9CA3AF.toInt())
            textSize = 13f
            setPadding(0, dp(8), 0, 0)
        })
        content.addView(infoCard)

        content.addView(buildSectionLabel("EMERGENCY CONTACTS"))

        // Contact List (Simplification for V4: One primary contact for now or comma-separated)
        val contactInput = buildInputField("Emergency Phone Number", memory.getFact("emergency_contact"))
        content.addView(contactInput)

        // Permissions Check
        val permStatus = TextView(this).apply {
            val hasSms = ContextCompat.checkSelfPermission(this@EFlareActivity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
            val hasLoc = ContextCompat.checkSelfPermission(this@EFlareActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            text = if (hasSms && hasLoc) "✅ Permissions Granted" else "⚠️ Permissions Needed: SMS & Location"
            setTextColor(if (hasSms && hasLoc) 0xFF10B981.toInt() else 0xFFEF4444.toInt())
            textSize = 12f
            setPadding(0, dp(12), 0, dp(24))
        }
        content.addView(permStatus)

        val requestPermBtn = Button(this).apply {
            text = "GRANT PERMISSIONS"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                ActivityCompat.requestPermissions(
                    this@EFlareActivity,
                    arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION),
                    101
                )
            }
        }
        content.addView(requestPermBtn)

        // Save & Test
        val saveBtn = Button(this).apply {
            text = "SAVE & ARM E-FLARE"
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                val phone = contactInput.findViewById<EditText>(android.R.id.edit).text.toString()
                memory.saveFact("emergency_contact", phone)
                Toast.makeText(this@EFlareActivity, "E-Flare Armed", Toast.LENGTH_SHORT).show()
            }
        }
        val btnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply {
            topMargin = dp(32)
        }
        content.addView(saveBtn, btnParams)

        val testBtn = Button(this).apply {
            text = "SEND TEST E-FLARE"
            setTextColor(0xFF9CA3AF.toInt())
            background = null
            setOnClickListener {
                Toast.makeText(this@EFlareActivity, "Test E-Flare Sent to stored contact", Toast.LENGTH_SHORT).show()
            }
        }
        content.addView(testBtn)

        root.addView(content)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun buildInputField(label: String, value: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
            addView(TextView(context).apply {
                text = label
                setTextColor(0xFF9CA3AF.toInt())
                textSize = 12f
                setPadding(0, 0, 0, dp(8))
            })
            val input = EditText(context).apply {
                id = android.R.id.edit
                setText(value)
                setTextColor(Color.WHITE)
                textSize = 15f
                setBackgroundColor(0xFF111827.toInt())
                setPadding(dp(16), dp(16), dp(16), dp(16))
                background = GradientDrawable().apply {
                    setColor(0xFF111827.toInt())
                    cornerRadius = dp(12).toFloat()
                }
            }
            addView(input)
        }
    }

    private fun buildSectionLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(0xFF4B5563.toInt())
            letterSpacing = 0.15f
            setPadding(0, dp(24), 0, dp(12))
        }
    }
}
