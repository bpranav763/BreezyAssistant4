package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.graphics.Typeface
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BreezySettingsActivity : BaseActivity() {

    private val memory by lazy { BreezyMemory(this) }
    private val allActions = mapOf(
        "security" to ("🛡️" to "Security"),
        "notes"    to ("📝" to "Notes"),
        "observe"  to ("👁️" to "Observe"),
        "vault"    to ("🔐" to "Vault"),
        "main"     to ("🏠" to "Home"),
        "brain"    to ("🧠" to "AI Brain"),
        "speed"    to ("⚡" to "Speed Test"),
        "usage"    to ("📊" to "App Usage"),
        "stalker"  to ("🕵️" to "Anti-Stalker"),
        "storage"  to ("🧹" to "Storage Clean")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
        }

        root.addView(buildHeader("⚙️ Settings") { finish() })

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(40))
        }

        // --- AI ENGINE SECTION ---
        container.addView(sectionLabel("AI BRAIN & PRIVACY"))
        
        val llm = LLMInference(this)
        container.addView(buildSettingsCard {
            addView(buildActionRow("🧠", "Manage Local Model", llm.getStatusText(), if (llm.isReady()) 0xFF34D399.toInt() else 0xFF9CA3AF.toInt()) {
                startActivity(Intent(this@BreezySettingsActivity, ModelDownloadActivity::class.java))
            })
            
            addView(divider())
            
            addView(buildSwitchRow("Force 100% Offline", "Ensures no data ever leaves the device", memory.getBubbleAiMode() == "llm") { isChecked ->
                memory.saveBubbleAiMode(if (isChecked) "llm" else "hybrid")
            })

            addView(divider())

            addView(buildSwitchRow("Auto-download on WiFi", "Keep AI model updated automatically", memory.isAutoDownloadEnabled()) { isChecked ->
                memory.saveAutoDownloadEnabled(isChecked)
            })
        })

        // --- API KEYS ---
        container.addView(sectionLabel("CLOUD FALLBACK (OPTIONAL)"))
        container.addView(buildSettingsCard {
            val geminiInput = buildInputRow("Gemini API Key", memory.getGeminiApiKey())
            val groqInput = buildInputRow("Groq API Key", memory.getGroqApiKey())
            
            addView(geminiInput)
            addView(divider())
            addView(groqInput)
            
            // We'll save these in the global save button
            this.tag = listOf(geminiInput, groqInput)
        })

        // --- BUBBLE BEHAVIOR ---
        container.addView(sectionLabel("CHAT BUBBLE BEHAVIOR"))
        container.addView(buildSettingsCard {
            val modes = listOf(
                "hybrid" to "Hybrid (Smart Fallback)",
                "llm" to "Pure Local (Private)",
                "groq" to "Always Groq (Fast)",
                "pool_only" to "Minimalist"
            )
            val currentMode = memory.getBubbleAiMode()
            
            val rg = RadioGroup(context).apply { orientation = RadioGroup.VERTICAL }
            modes.forEach { (id, label) ->
                rg.addView(RadioButton(context).apply {
                    text = label; setTextColor(Color.WHITE); tag = id
                    isChecked = id == currentMode
                    buttonTintList = android.content.res.ColorStateList.valueOf(0xFF1D4ED8.toInt())
                })
            }
            addView(rg)
        })

        // --- JOYSTICK ---
        container.addView(sectionLabel("JOYSTICK MENU (MAX 5)"))
        val currentConfig = memory.getJoystickConfig().split(",").toMutableSet()
        container.addView(buildSettingsCard {
            val grid = GridLayout(context).apply { columnCount = 2 }
            allActions.forEach { (id, data) ->
                val (icon, name) = data
                grid.addView(CheckBox(context).apply {
                    text = "$icon $name"; setTextColor(Color.WHITE)
                    isChecked = currentConfig.contains(id)
                    buttonTintList = android.content.res.ColorStateList.valueOf(0xFF1D4ED8.toInt())
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            if (currentConfig.size >= 5) {
                                this.isChecked = false
                                Toast.makeText(context, "Max 5 items allowed", Toast.LENGTH_SHORT).show()
                            } else {
                                currentConfig.add(id)
                            }
                        } else {
                            currentConfig.remove(id)
                        }
                    }
                })
            }
            addView(grid)
        })

        // --- SAVE ---
        val saveBtn = Button(this).apply {
            text = "SAVE CHANGES"
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(32) }
            setOnClickListener {
                // Find inputs from their container tags or direct references
                // For simplicity in this refactor, I'll use the tags
                val apiCard = container.getChildAt(3) as? LinearLayout
                val apiInputs = apiCard?.tag as? List<*>
                
                apiInputs?.filterIsInstance<EditText>()?.let {
                    if (it.size >= 2) {
                        memory.saveGeminiApiKey(it[0].text.toString().trim())
                        memory.saveGroqApiKey(it[1].text.toString().trim())
                    }
                }

                memory.saveJoystickConfig(currentConfig.joinToString(","))
                
                // Find RadioGroup
                val modeCard = container.getChildAt(5) as? LinearLayout
                val rg = modeCard?.getChildAt(0) as? RadioGroup
                val selectedId = rg?.findViewById<RadioButton>(rg.checkedRadioButtonId)?.tag as? String
                if (selectedId != null) memory.saveBubbleAiMode(selectedId)

                Toast.makeText(this@BreezySettingsActivity, "Settings Applied", Toast.LENGTH_SHORT).show()
                
                // Restart service
                stopService(Intent(this@BreezySettingsActivity, FloatingCircleService::class.java))
                startService(Intent(this@BreezySettingsActivity, FloatingCircleService::class.java))
                finish()
            }
        }
        container.addView(saveBtn)

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun buildSettingsCard(block: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt()); cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
            block()
        }
    }

    private fun buildActionRow(icon: String, title: String, sub: String, subColor: Int, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { onClick() }
            
            addView(TextView(context).apply { text = icon; textSize = 20f })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) }
                addView(TextView(context).apply { text = title; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD })
                addView(TextView(context).apply { text = sub; textSize = 11f; setTextColor(subColor) })
            })
            addView(TextView(context).apply { text = "→"; setTextColor(0xFF4B5563.toInt()) })
        }
    }

    private fun buildSwitchRow(title: String, desc: String, checked: Boolean, onCheck: (Boolean) -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                addView(TextView(context).apply { text = title; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD })
                addView(TextView(context).apply { text = desc; textSize = 11f; setTextColor(0xFF9CA3AF.toInt()) })
            })
            
            addView(Switch(context).apply {
                isChecked = checked
                thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                trackTintList = android.content.res.ColorStateList.valueOf(if (checked) 0xFF1D4ED8.toInt() else 0xFF374151.toInt())
                setOnCheckedChangeListener { _, isChecked -> 
                    onCheck(isChecked)
                    trackTintList = android.content.res.ColorStateList.valueOf(if (isChecked) 0xFF1D4ED8.toInt() else 0xFF374151.toInt())
                }
            })
        }
    }

    private fun buildInputRow(hint: String, value: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(value)
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF6B7280.toInt())
            background = null
            setPadding(0, dp(8), 0, dp(8))
        }
    }

    private fun sectionLabel(txt: String) = TextView(this).apply {
        text = txt; textSize = 11f; setTextColor(0xFF3B82F6.toInt())
        typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.1f
        setPadding(dp(4), dp(12), 0, dp(8))
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, 1).apply { setMargins(0, dp(12), 0, dp(12)) }
        setBackgroundColor(0xFF1F2937.toInt())
    }
}

