package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts

class ProfileActivity : BaseActivity() {

    private val memory by lazy { BreezyMemory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        // Header
        root.addView(buildHeader("👤 Profile") { finish() })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Avatar Section
        val avatarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(32))
        }

        val avatar = TextView(this).apply {
            val name = memory.getUserName()
            text = if (name.isNotEmpty()) name.first().uppercase() else "■■"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF1D4ED8.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(80))
        }
        avatarContainer.addView(avatar)

        avatarContainer.addView(TextView(this).apply {
            text = "Tap to change photo"
            setTextColor(0xFF4B5563.toInt())
            textSize = 12f
            setPadding(0, dp(12), 0, 0)
        })
        content.addView(avatarContainer)

        // Fields
        content.addView(buildSectionLabel("PERSONAL INFO"))
        
        val nameInput = buildInputField("Display Name", memory.getUserName())
        content.addView(nameInput)
        
        val bioInput = buildInputField("Bio", memory.getUserProfession(), isMultiline = true)
        content.addView(bioInput)

        content.addView(buildSectionLabel("EMERGENCY & CONTACTS"))
        
        val eFlareBtn = buildMenuButton("📢 E-Flare Settings", "Manage emergency contacts") {
            startActivity(Intent(this, EFlareActivity::class.java))
        }
        content.addView(eFlareBtn)

        content.addView(buildSectionLabel("BREEZY MEMORY"))
        val transparencyBtn = buildMenuButton("🧠 Transparency", "See every fact Breezy knows about you") {
            startActivity(Intent(this, TransparencyActivity::class.java))
        }
        content.addView(transparencyBtn)

        // Save Button
        val saveBtn = Button(this).apply {
            text = "SAVE PROFILE"
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                memory.saveUserName(nameInput.findViewById<EditText>(android.R.id.edit).text.toString())
                memory.saveUserProfession(bioInput.findViewById<EditText>(android.R.id.edit).text.toString())
                Toast.makeText(this@ProfileActivity, "Profile Updated", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        val btnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply {
            topMargin = dp(40)
            bottomMargin = dp(40)
        }
        content.addView(saveBtn, btnParams)

        scroll.addView(content)
        root.addView(scroll)

        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun buildInputField(label: String, value: String, isMultiline: Boolean = false): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(20))
            
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
                if (isMultiline) {
                    minLines = 3
                    gravity = Gravity.TOP
                }
                background = GradientDrawable().apply {
                    setColor(0xFF111827.toInt())
                    cornerRadius = dp(12).toFloat()
                }
            }
            addView(input)
        }
    }

    private fun buildMenuButton(title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener { onClick() }
            
            val textContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textContainer.addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            textContainer.addView(TextView(context).apply {
                text = subtitle
                setTextColor(0xFF9CA3AF.toInt())
                textSize = 12f
            })
            addView(textContainer)
            
            addView(TextView(context).apply {
                text = "→"
                setTextColor(0xFF4B5563.toInt())
                textSize = 18f
            })
            
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }
    }

    private fun buildSectionLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(0xFF4B5563.toInt())
            letterSpacing = 0.15f
            setPadding(0, dp(12), 0, dp(12))
        }
    }
}
