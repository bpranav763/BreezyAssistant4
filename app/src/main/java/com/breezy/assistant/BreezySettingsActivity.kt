package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.graphics.Typeface
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog

class BreezySettingsActivity : BaseActivity() {

    private val memory by lazy { BreezyMemory(this) }
    private lateinit var geminiInput: EditText
    private lateinit var groqInput: EditText
    private lateinit var modeRadioGroup: RadioGroup

    private val allActions = mapOf(
        "main"     to ("🏠" to "Home"),
        "apps"     to ("📦" to "Apps Menu"),
        "security" to ("🛡️" to "Security"),
        "notes"    to ("📝" to "Notes"),
        "observe"  to ("👁️" to "Dashboard"),
        "vault"    to ("🔐" to "Vault"),
        "brain"    to ("🧠" to "AI Settings"),
        "speed"    to ("⚡" to "Speed Test"),
        "usage"    to ("📊" to "App Usage"),
        "stalker"  to ("🕵️" to "Anti-Stalker"),
        "storage"  to ("🧹" to "Storage")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@BreezySettingsActivity))
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

        // --- THEME ---
        container.addView(sectionLabel("APPEARANCE"))
        container.addView(buildSettingsCard {
            addView(buildActionRow("🎨", "Screen colour theme", ThemeManager.getCurrentTheme(this@BreezySettingsActivity).name.replace("_", " "), ThemeManager.getAccentColor(this@BreezySettingsActivity)) {
                showThemeDialog()
            })
        })

        // --- VOICE ---
        container.addView(sectionLabel("VOICE & WAKE"))
        container.addView(buildSettingsCard {
            addView(buildSwitchRow("Voice Wake", "Listen for 'Hey Breezy' (Battery usage increases)", memory.getFact("voice_wake_enabled") == "true") { enabled ->
                memory.saveFact("voice_wake_enabled", if (enabled) "true" else "false")
                val intent = Intent(this@BreezySettingsActivity, VoiceWakeService::class.java)
                if (enabled) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                } else {
                    stopService(intent)
                }
            })
        })

        // --- AI ENGINE ---
        container.addView(sectionLabel("AI BRAIN & LOCALITY"))
        val llm = LLMInference(this)
        container.addView(buildSettingsCard {
            addView(buildActionRow("🧠", "Manage Local Brain", llm.getStatusText(), if (llm.isReady()) 0xFF34D399.toInt() else 0xFF9CA3AF.toInt()) {
                startActivity(Intent(this@BreezySettingsActivity, ModelDownloadActivity::class.java))
            })
            addView(divider())
            addView(buildSwitchRow("Auto-download on WiFi", "Update model in background", memory.isAutoDownloadEnabled()) { 
                memory.saveAutoDownloadEnabled(it) 
            })
        })

        // --- AI BEHAVIOR ---
        container.addView(sectionLabel("RESPONSE STRATEGY"))
        container.addView(buildSettingsCard {
            val modes = listOf(
                "llm" to "100% Offline (Maximum Privacy)",
                "hybrid" to "Hybrid (Cloud fallback if local fails)",
                "groq" to "Cloud Priority (Fastest, uses API)",
                "pool_only" to "Minimal (Static responses only)"
            )
            val currentMode = memory.getBubbleAiMode()
            modeRadioGroup = RadioGroup(context).apply { orientation = RadioGroup.VERTICAL }
            modes.forEach { (id, label) ->
                modeRadioGroup.addView(RadioButton(context).apply {
                    text = label; setTextColor(ThemeManager.getTextPrimary(this@BreezySettingsActivity)); tag = id
                    isChecked = id == currentMode
                    buttonTintList = android.content.res.ColorStateList.valueOf(ThemeManager.getAccentColor(this@BreezySettingsActivity))
                    setPadding(dp(8), dp(12), 0, dp(12))
                })
            }
            addView(modeRadioGroup)
        })

        // --- CLOUD KEYS ---
        container.addView(sectionLabel("CLOUD API KEYS"))
        container.addView(buildSettingsCard {
            geminiInput = buildInputRow("Gemini API Key", memory.getGeminiApiKey())
            groqInput = buildInputRow("Groq API Key", memory.getGroqApiKey())
            addView(geminiInput)
            addView(divider())
            addView(groqInput)
        })

        // --- JOYSTICK ---
        container.addView(sectionLabel("JOYSTICK SLOTS (MAX 5)"))
        val currentConfig = memory.getJoystickConfig().split(",").toMutableSet()
        container.addView(buildSettingsCard {
            val grid = GridLayout(context).apply { columnCount = 2 }
            allActions.forEach { (id, data) ->
                val (icon, name) = data
                grid.addView(CheckBox(context).apply {
                    text = "$icon $name"; setTextColor(ThemeManager.getTextPrimary(this@BreezySettingsActivity))
                    isChecked = currentConfig.contains(id)
                    buttonTintList = android.content.res.ColorStateList.valueOf(ThemeManager.getAccentColor(this@BreezySettingsActivity))
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            if (currentConfig.size >= 5) {
                                this.isChecked = false
                                Toast.makeText(context, "Max 5 slots reached", Toast.LENGTH_SHORT).show()
                            } else { currentConfig.add(id) }
                        } else { currentConfig.remove(id) }
                    }
                })
            }
            addView(grid)
        })

        // --- SAVE ---
        val saveBtn = Button(this).apply {
            text = "APPLY ALL CHANGES"
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(ThemeManager.getAccentColor(this@BreezySettingsActivity)); cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(32) }
            setOnClickListener {
                // Save AI Mode
                val selectedId = modeRadioGroup.findViewById<RadioButton>(modeRadioGroup.checkedRadioButtonId)?.tag as? String
                if (selectedId != null) memory.saveBubbleAiMode(selectedId)

                // Save API Keys
                memory.saveGeminiApiKey(geminiInput.text.toString().trim())
                memory.saveGroqApiKey(groqInput.text.toString().trim())

                // Save Joystick
                memory.saveJoystickConfig(currentConfig.joinToString(","))

                Toast.makeText(this@BreezySettingsActivity, "Settings Updated ✓", Toast.LENGTH_SHORT).show()
                
                // Restart Service to apply UI/AI changes
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

    private fun buildSettingsCard(block: LinearLayout.() -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            setColor(ThemeManager.getCardColor(this@BreezySettingsActivity))
            cornerRadius = ThemeManager.getCornerRadius(this@BreezySettingsActivity).dp(this@BreezySettingsActivity).toFloat()
        }
        setPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
        block()
    }

    private fun showThemeDialog() {
        val themes = ThemeManager.Theme.values()
        val names = themes.map { it.name.replace("_", " ") }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Theme")
            .setItems(names) { _, which ->
                ThemeManager.applyTheme(this, themes[which])
            }
            .show()
    }

    private fun buildActionRow(icon: String, title: String, sub: String, subColor: Int, onClick: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setOnClickListener { onClick() }
        addView(TextView(context).apply { text = icon; textSize = 20f })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) }
            addView(TextView(context).apply { text = title; setTextColor(ThemeManager.getTextPrimary(this@BreezySettingsActivity)); typeface = Typeface.DEFAULT_BOLD })
            addView(TextView(context).apply { text = sub; textSize = 11f; setTextColor(subColor) })
        })
        addView(TextView(context).apply { text = "→"; setTextColor(ThemeManager.getTextSecondary(this@BreezySettingsActivity)) })
    }

    private fun buildSwitchRow(title: String, desc: String, checked: Boolean, onCheck: (Boolean) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(8), 0, dp(8))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            addView(TextView(context).apply { text = title; setTextColor(ThemeManager.getTextPrimary(this@BreezySettingsActivity)); typeface = Typeface.DEFAULT_BOLD })
            addView(TextView(context).apply { text = desc; textSize = 11f; setTextColor(ThemeManager.getTextSecondary(this@BreezySettingsActivity)) })
        })
        addView(Switch(context).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, it -> onCheck(it) }
        })
    }

    private fun buildInputRow(hint: String, value: String) = EditText(this).apply {
        this.hint = hint; setText(value); setTextColor(ThemeManager.getTextPrimary(this@BreezySettingsActivity))
        setHintTextColor(ThemeManager.getTextSecondary(this@BreezySettingsActivity)); background = null
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun sectionLabel(txt: String) = TextView(this).apply {
        text = txt; textSize = 11f; setTextColor(ThemeManager.getAccentColor(this@BreezySettingsActivity))
        typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.1f
        setPadding(dp(4), dp(12), 0, dp(8))
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, 1).apply { setMargins(0, dp(12), 0, dp(12)) }
        setBackgroundColor(ThemeManager.getTextSecondary(this@BreezySettingsActivity) and 0x33FFFFFF)
    }
}
