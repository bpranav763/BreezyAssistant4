package com.breezy.assistant
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.*

class SettingsActivity : BaseActivity() {
    private val memory by lazy { BreezyMemory(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        root.addView(buildHeader("⚙️ Settings") { finish() })
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), dp(32)) }

        container.addView(buildLabel("BUBBLE SETTINGS"))
        val bubbleSwitch = Switch(this).apply {
            text = "Enable Floating Bubble"
            setTextColor(Color.WHITE)
            isChecked = memory.isBubbleEnabled()
            setPadding(0, dp(8), 0, dp(16))
            setOnCheckedChangeListener { _, isChecked ->
                memory.saveBubbleEnabled(isChecked)
                if (isChecked) {
                    startService(android.content.Intent(this@SettingsActivity, FloatingCircleService::class.java))
                } else {
                    stopService(android.content.Intent(this@SettingsActivity, FloatingCircleService::class.java))
                }
            }
        }
        container.addView(bubbleSwitch)

        container.addView(TextView(this).apply {
            text = "Bubble Size"
            textSize = 12f
            setTextColor(0xFF9CA3AF.toInt())
            setPadding(0, dp(8), 0, 0)
        })
        val sizeSeekBar = SeekBar(this).apply {
            max = 100
            progress = memory.getBubbleSize() - 40 // Range 40 to 140
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val size = progress + 40
                    memory.saveBubbleSize(size)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    Toast.makeText(this@SettingsActivity, "Size saved. Restart bubble to apply.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        container.addView(sizeSeekBar)

        container.addView(buildLabel("NAME"))
        val nameInput = EditText(this).apply {
            setText(memory.getUserName()); setTextColor(Color.WHITE)
            setBackgroundColor(0xFF111827.toInt()); setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0,0,0,dp(16)) }
        }
        container.addView(nameInput)
        container.addView(TextView(this).apply {
            text = "Save Name"; setTextColor(0xFF1D4ED8.toInt()); textSize = 14f
            setPadding(0, dp(8), 0, dp(24))
            setOnClickListener { memory.saveUserName(nameInput.text.toString().trim()); Toast.makeText(this@SettingsActivity,"Saved",Toast.LENGTH_SHORT).show() }
        })

        container.addView(buildLabel("PERSONALITY TONE"))
        val tones = listOf("friendly","witty","professional","calm")
        tones.forEach { tone ->
            container.addView(TextView(this).apply {
                text = tone.replaceFirstChar { it.uppercase() }
                textSize = 14f; setTextColor(if (memory.getTone() == tone) 0xFF1D4ED8.toInt() else Color.WHITE)
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener { memory.saveTone(tone); recreate() }
            })
        }

        container.addView(buildLabel("REGION"))
        listOf("IN" to "🇮🇳 India","PH" to "🇵🇭 Philippines","US" to "🇺🇸 USA","OTHER" to "🌍 Other").forEach { (code, label) ->
            container.addView(TextView(this).apply {
                text = label; textSize = 14f
                setTextColor(if (memory.getRegion() == code) 0xFF1D4ED8.toInt() else Color.WHITE)
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener { memory.saveRegion(code); recreate() }
            })
        }

        container.addView(buildLabel("DATA & PRIVACY"))
        container.addView(TextView(this).apply {
            text = "🛡️ Permissions Hub"; textSize = 14f; setTextColor(0xFF3B82F6.toInt()); setPadding(0, dp(12), 0, dp(12))
            setOnClickListener { showPermissionsHub() }
        })
        container.addView(TextView(this).apply {
            text = "🔍 See what Breezy knows"; textSize = 14f; setTextColor(Color.WHITE); setPadding(0, dp(12), 0, dp(12))
            setOnClickListener { startActivity(android.content.Intent(this@SettingsActivity, TransparencyActivity::class.java)) }
        })
        container.addView(TextView(this).apply {
            text = "🔄 Reset"; textSize = 14f; setTextColor(0xFFEF4444.toInt()); setPadding(0, dp(12), 0, dp(12))
            setOnClickListener { memory.clearAll(); startActivity(android.content.Intent(this@SettingsActivity, OnboardingActivity::class.java)); finish() }
        })

        scroll.addView(container); root.addView(scroll)
        setContentView(root); applySystemBarInsets(root)
    }
    private fun buildLabel(t: String) = TextView(this).apply { text = t; textSize = 11f; setTextColor(0xFF4B5563.toInt()); letterSpacing = 0.15f; setPadding(0, dp(24), 0, dp(8)) }

    private fun showPermissionsHub() {
        val dialog = android.app.Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setCancelable(true)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111827.toInt())
            setPadding(dp(24), dp(24), dp(24), dp(32))
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }

        layout.addView(TextView(this).apply {
            text = "Permissions Hub"; textSize = 18f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(16))
        })

        val perms = listOf(
            "Overlay" to "Allows the Floating Circle to appear over other apps.",
            "Accessibility" to "Used to detect app launches for the Note system.",
            "Usage Stats" to "Required for the App Usage monitor.",
            "Storage" to "Used for the Storage Analyzer tool."
        )

        perms.forEach { (title, desc) ->
            layout.addView(TextView(this).apply {
                text = title; textSize = 15f; setTextColor(Color.WHITE); setPadding(0, dp(12), 0, dp(2))
            })
            layout.addView(TextView(this).apply {
                text = desc; textSize = 12f; setTextColor(0xFF9CA3AF.toInt()); setPadding(0, 0, 0, dp(12))
            })
        }

        layout.addView(Button(this).apply {
            text = "System Settings"; setBackgroundColor(0xFF1D4ED8.toInt()); setTextColor(Color.WHITE)
            setOnClickListener { 
                startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                })
                dialog.dismiss()
            }
        })

        dialog.setContentView(layout)
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }
}
