package com.breezy.assistant

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import java.util.*

class SettingsActivity : BaseActivity() {
    private val memory by lazy { BreezyMemory(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        root.addView(buildHeader("👤 Profile & Settings") { finish() })

        val scroll = ScrollView(this).apply { 
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            isFillViewport = true
        }
        
        val container = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(40)) 
        }

        // --- SECTION: PROFILE ---
        container.addView(buildSectionHeader("MY PROFILE"))
        
        val profileCard = buildCard()
        profileCard.addView(buildEditableRow("Name", memory.getUserName()) { memory.saveUserName(it) })
        
        val dob = if (memory.getUserDOB().isEmpty()) "Not set" else memory.getUserDOB()
        profileCard.addView(buildClickableRow("Birthday", dob) { showDatePicker { memory.saveUserDOB(it); recreate() } })
        
        profileCard.addView(buildEditableRow("Profession", memory.getUserProfession()) { memory.saveUserProfession(it) })
        
        val region = when(memory.getRegion()) {
            "IN" -> "India 🇮🇳"
            "US" -> "USA 🇺🇸"
            "PH" -> "Philippines 🇵🇭"
            else -> "Other 🌍"
        }
        profileCard.addView(buildClickableRow("Country", region) { showRegionPicker() })
        container.addView(profileCard)

        // --- SECTION: BUBBLE CUSTOMIZATION ---
        container.addView(buildSectionHeader("BUBBLE CUSTOMIZATION"))
        val bubbleCard = buildCard()
        
        val bubbleToggle = Switch(this).apply {
            text = "Activation Status"; setTextColor(Color.WHITE)
            isChecked = memory.isBubbleEnabled()
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnCheckedChangeListener { _, isChecked ->
                memory.saveBubbleEnabled(isChecked)
                if (isChecked) startService(android.content.Intent(context, FloatingCircleService::class.java))
                else stopService(android.content.Intent(context, FloatingCircleService::class.java))
            }
        }
        bubbleCard.addView(bubbleToggle)
        
        bubbleCard.addView(buildSliderRow("Active Size", memory.getBubbleSize(), 40, 140) { memory.saveBubbleSize(it) })
        bubbleCard.addView(buildSliderRow("Idle Size", memory.getBubbleIdleSize(), 10, 40) { memory.saveBubbleIdleSize(it) })
        bubbleCard.addView(buildSliderRow("Idle Timer (sec)", memory.getBubbleIdleTime(), 3, 30) { memory.saveBubbleIdleTime(it) })
        
        // Color Picker Row
        val colorRow = buildClickableRow("Bubble Color", "Change") { showColorPicker() }
        bubbleCard.addView(colorRow)
        
        container.addView(bubbleCard)

        // --- SECTION: CHAT CUSTOMIZATION ---
        container.addView(buildSectionHeader("CHAT EXPERIENCE"))
        val chatCard = buildCard()
        chatCard.addView(buildClickableRow("Personality Tone", memory.getTone().replaceFirstChar { it.uppercase() }) { showTonePicker() })
        chatCard.addView(buildClickableRow("AI Engine & Brain", "Configure") { 
            startActivity(android.content.Intent(this, BreezySettingsActivity::class.java))
        })
        container.addView(chatCard)

        // --- SECTION: SYSTEM ---
        container.addView(buildSectionHeader("SYSTEM & PRIVACY"))
        val systemCard = buildCard()
        systemCard.addView(buildClickableRow("Permissions Hub", "Manage") { showPermissionsHub() })
        systemCard.addView(buildClickableRow("Notification Settings", "Configure") { /* Future implementation */ })
        systemCard.addView(buildClickableRow("Breezy Transparency", "View Facts") { 
            startActivity(android.content.Intent(this, TransparencyActivity::class.java))
        })
        container.addView(systemCard)

        // Reset Button
        container.addView(TextView(this).apply {
            text = "Reset Breezy Memory"
            setTextColor(0xFFEF4444.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(32), 0, dp(16))
            setOnClickListener { 
                memory.clearAll()
                startActivity(android.content.Intent(this@SettingsActivity, OnboardingActivity::class.java))
                finish()
            }
        })

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun buildSectionHeader(title: String) = TextView(this).apply {
        text = title
        textSize = 12f
        setTextColor(0xFF6B7280.toInt())
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.1f
        setPadding(dp(4), dp(24), 0, dp(8))
    }

    private fun buildCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(android.R.drawable.dialog_holo_dark_frame) // Simple dark background
        background.setTint(0xFF111827.toInt())
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(4)) }
    }

    private fun buildEditableRow(label: String, value: String, onSave: (String) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        gravity = Gravity.CENTER_VERTICAL
        
        addView(TextView(context).apply {
            text = label; setTextColor(0xFF9CA3AF.toInt()); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        
        val edit = EditText(context).apply {
            setText(value); setTextColor(Color.WHITE); textSize = 14f
            background = null; gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, -2, 1.5f)
            setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onSave(text.toString()) }
        }
        addView(edit)
    }

    private fun buildClickableRow(label: String, value: String, onClick: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        setOnClickListener { onClick() }
        
        addView(TextView(context).apply {
            text = label; setTextColor(0xFF9CA3AF.toInt()); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        
        addView(TextView(context).apply {
            text = value; setTextColor(0xFF3B82F6.toInt()); textSize = 14f
            gravity = Gravity.END
        })
    }

    private fun buildSliderRow(label: String, current: Int, min: Int, max: Int, onProgress: (Int) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(12))
        
        addView(TextView(context).apply {
            text = "$label: $current"; setTextColor(0xFF9CA3AF.toInt()); textSize = 13f
        })
        
        addView(SeekBar(context).apply {
            this.max = max - min
            this.progress = current - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                    val actual = p + min
                    onProgress(actual)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) { recreate() }
            })
        })
    }

    private fun showDatePicker(onSelected: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d -> onSelected("$d/${m+1}/$y") }, 
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTonePicker() {
        val tones = listOf("friendly", "witty", "professional", "calm", "protective")
        val dialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Select Personality Tone")
            .setItems(tones.map { it.replaceFirstChar { c -> c.uppercase() } }.toTypedArray()) { _, i ->
                memory.saveTone(tones[i])
                recreate()
            }.show()
    }

    private fun showRegionPicker() {
        val regions = listOf("IN" to "India 🇮🇳", "US" to "USA 🇺🇸", "PH" to "Philippines 🇵🇭", "OTHER" to "Other 🌍")
        android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Select Region")
            .setItems(regions.map { it.second }.toTypedArray()) { _, i ->
                memory.saveRegion(regions[i].first)
                recreate()
            }.show()
    }

    private fun showColorPicker() {
        val colors = listOf(0xFF1D4ED8.toInt(), 0xFFEF4444.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(), 0xFF8B5CF6.toInt())
        val names = listOf("Breezy Blue", "Alert Red", "Safety Green", "Warning Orange", "Cosmic Purple")
        android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Bubble Color")
            .setItems(names.toTypedArray()) { _, i ->
                memory.saveBubbleColor(colors[i])
                Toast.makeText(this, "Color updated. Restart bubble to see.", Toast.LENGTH_SHORT).show()
                recreate()
            }.show()
    }

    private fun showPermissionsHub() {
        val dialog = android.app.Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF111827.toInt())
                setPadding(dp(24), dp(24), dp(24), dp(32))
                
                addView(TextView(context).apply {
                    text = "Permissions Hub"; textSize = 18f; setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, dp(16))
                })

                val perms = listOf(
                    "Overlay" to "Allows Breezy to float over other apps for quick access.",
                    "Accessibility" to "Required to detect app switches and show startup notes.",
                    "Microphone" to "Used for voice commands and Hinglish conversation.",
                    "Usage Stats" to "Helps Breezy analyze your phone's health and usage patterns.",
                    "Location" to "Used for network security analysis and local alerts."
                )

                perms.forEach { (t, d) ->
                    addView(TextView(context).apply { text = t; setTextColor(Color.WHITE); textSize = 15f; setPadding(0, dp(12), 0, dp(2)) })
                    addView(TextView(context).apply { text = d; setTextColor(0xFF9CA3AF.toInt()); textSize = 12f; setPadding(0, 0, 0, dp(12)) })
                }

                addView(Button(context).apply {
                    text = "Open System Settings"; setBackgroundColor(0xFF1D4ED8.toInt()); setTextColor(Color.WHITE)
                    setOnClickListener {
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", packageName, null)
                        })
                        dismiss()
                    }
                })
            })
        }
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }
}
