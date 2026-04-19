package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
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
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(buildHeader("⚙️ Breezy Settings") { finish() })

        // --- LOCAL AI BRAIN ---
        root.addView(sectionLabel("LOCAL AI BRAIN"))
        val llm = LLMInference(this)
        val brainBtn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF1F2937.toInt()); cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener {
                startActivity(Intent(this@BreezySettingsActivity, ModelDownloadActivity::class.java))
            }
            
            addView(TextView(context).apply {
                text = "🧠"; textSize = 20f
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) }
                addView(TextView(context).apply {
                    text = "Manage Offline Model"; setTextColor(Color.WHITE)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
                addView(TextView(context).apply {
                    text = llm.getStatusText(); textSize = 11f
                    setTextColor(if (llm.isReady()) 0xFF34D399.toInt() else 0xFF9CA3AF.toInt())
                })
            })
            addView(TextView(context).apply {
                text = "→"; setTextColor(0xFF4B5563.toInt())
            })
        }
        root.addView(brainBtn)

        // --- AI ENGINE (GEMINI) ---
        root.addView(sectionLabel("AI ENGINE (GEMINI)"))
        val geminiInput = EditText(this).apply {
            hint = "Enter Gemini API Key"; setTextColor(Color.WHITE)
            setHintTextColor(0xFF6B7280.toInt())
            background = GradientDrawable().apply {
                setColor(0xFF1F2937.toInt()); cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setText(memory.getGeminiApiKey())
        }
        root.addView(geminiInput)

        // --- AI ENGINE (GROQ) ---
        root.addView(sectionLabel("AI ENGINE (GROQ)"))
        val groqInput = EditText(this).apply {
            hint = "Enter Groq API Key"; setTextColor(Color.WHITE)
            setHintTextColor(0xFF6B7280.toInt())
            background = GradientDrawable().apply {
                setColor(0xFF1F2937.toInt()); cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setText(memory.getGroqApiKey())
        }
        root.addView(groqInput)
        
        root.addView(TextView(this).apply {
            text = "Groq provides near-instant fallback when offline model is slow."; textSize = 11f
            setTextColor(0xFF9CA3AF.toInt()); setPadding(0, dp(4), 0, dp(20))
        })

        // --- Bubble AI Behavior ---
        root.addView(sectionLabel("CHAT BUBBLE AI BEHAVIOR"))
        val modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(0, 0, 0, dp(20))
        }
        
        val modes = listOf(
            "hybrid" to "Hybrid (Smart Casual -> Groq/LLM)",
            "llm" to "Pure Local LLM (Private & Offline)",
            "groq" to "Always Groq (Fastest, needs Internet)",
            "pool_only" to "Minimalist (Pre-set responses only)"
        )
        
        val currentMode = memory.getBubbleAiMode()
        modes.forEach { (id, label) ->
            val rb = RadioButton(this).apply {
                text = label; setTextColor(Color.WHITE)
                tag = id
                isChecked = id == currentMode
            }
            modeGroup.addView(rb)
        }
        root.addView(modeGroup)

        // --- Auto Download ---
        val autoDownloadSwitch = CheckBox(this).apply {
            text = "Auto-download AI Brain on WiFi"; setTextColor(Color.WHITE)
            isChecked = memory.isAutoDownloadEnabled()
            setPadding(0, 0, 0, dp(20))
        }
        root.addView(autoDownloadSwitch)

        // --- Joystick Section ---
        root.addView(sectionLabel("JOYSTICK MENU (PICK 5)"))
        val currentConfig = memory.getJoystickConfig().split(",").toMutableSet()
        val grid = GridLayout(this).apply { columnCount = 2 }

        allActions.forEach { (id, data) ->
            val (icon, name) = data
            val chip = CheckBox(this).apply {
                text = "$icon $name"; setTextColor(Color.WHITE)
                isChecked = currentConfig.contains(id)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) currentConfig.add(id) else currentConfig.remove(id)
                }
            }
            grid.addView(chip)
        }
        root.addView(grid)

        // --- Save Button ---
        val saveBtn = Button(this).apply {
            text = "SAVE ALL CHANGES"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(40) }
            setOnClickListener {
                memory.saveGroqApiKey(groqInput.text.toString().trim())
                memory.saveGeminiApiKey(geminiInput.text.toString().trim())
                memory.saveJoystickConfig(currentConfig.joinToString(","))
                memory.saveAutoDownloadEnabled(autoDownloadSwitch.isChecked)
                
                val selectedId = modeGroup.findViewById<RadioButton>(modeGroup.checkedRadioButtonId)?.tag as? String
                if (selectedId != null) memory.saveBubbleAiMode(selectedId)

                Toast.makeText(this@BreezySettingsActivity, "Settings Saved!", Toast.LENGTH_SHORT).show()
                
                // Restart service to apply changes
                stopService(Intent(this@BreezySettingsActivity, FloatingCircleService::class.java))
                startService(Intent(this@BreezySettingsActivity, FloatingCircleService::class.java))
                finish()
            }
        }
        root.addView(saveBtn)

        setContentView(ScrollView(this).apply { addView(root) })
        applySystemBarInsets(root)
    }

    private fun sectionLabel(txt: String) = TextView(this).apply {
        text = txt; textSize = 12f; setTextColor(0xFF3B82F6.toInt())
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(0, dp(12), 0, dp(8))
    }
}
