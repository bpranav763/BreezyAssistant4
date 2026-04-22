package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ChatBubbleActivity : BaseActivity() {

    private lateinit var responseEngine: ResponseEngine
    private lateinit var messagesLayout: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var morningReportManager: MorningReportManager
    private lateinit var voiceEngine: VoiceEngine
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        responseEngine = ResponseEngine(this, BatteryMonitor(this))
        voiceEngine = VoiceEngine(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                setOnClickListener { finish() }
            })
        }

        val chatContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@ChatBubbleActivity))
                val r = cornerRadius().toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            elevation = 20f
            setPadding(dp(32), dp(32), dp(32), dp(32))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.45f).toInt()
            )
            setOnClickListener { }
        }

        chatContent.addView(TextView(this).apply {
            text = "🌬️ Breezy"
            textSize = 17f
            setTextColor(ThemeManager.getAccentColor(this@ChatBubbleActivity))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        })

        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }
        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        scrollView.addView(messagesLayout)

        val memory = BreezyMemory(this)
        val battery = BatteryMonitor(this)
        val data = battery.getBatteryData()

        val quickActionsRow = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(0, dp(8), 0, dp(4))
            isHorizontalScrollBarEnabled = false
        }

        val actionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), 0, dp(4), 0)
        }

        val actions = listOf(
            QuickAction("🔇", "Mute") { toggleMute() },
            QuickAction("🔕", "DND") { toggleDND() },
            QuickAction("📶", "WiFi") { toggleWiFi() },
            QuickAction("🔵", "BT") { toggleBluetooth() },
            QuickAction("☀️", "Bright") { setBrightness(80) },
            QuickAction("⚙️", "More") { openQuickSettings() }
        )

        actions.forEach { action ->
            actionsContainer.addView(createActionChip(action))
        }

        quickActionsRow.addView(actionsContainer)
        chatContent.addView(quickActionsRow)

        morningReportManager = MorningReportManager(this)
        if (morningReportManager.shouldShowReport()) {
            lifecycleScope.launch {
                addBreezyMsg(morningReportManager.generateReport())
            }
        } else {
            addBreezyMsg(ResponsePool.getGreeting(memory.getTone(), memory.getUserName(), data.level, data.temperature))
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        val micBtn = TextView(this).apply {
            text = "🎤"
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setOnClickListener { toggleVoice() }
        }

        inputField = EditText(this).apply {
            hint = "Ask Breezy anything..."
            setHintTextColor(ThemeManager.getTextSecondary(this@ChatBubbleActivity))
            setTextColor(ThemeManager.getTextPrimary(this@ChatBubbleActivity))
            setBackgroundResource(android.R.color.transparent)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dp(8), dp(20), dp(8), dp(20))
        }

        val sendBtn = TextView(this).apply {
            text = "↑"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(ThemeManager.getAccentColor(this@ChatBubbleActivity))
            }
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setOnClickListener {
                val input = inputField.text.toString().trim()
                if (input.isNotEmpty()) {
                    addUserMsg(input)
                    inputField.setText("")
                    // Limit bubble to 6 messages
                    if (messagesLayout.childCount > 6) messagesLayout.removeViewAt(0)
                    
                    lifecycleScope.launch {
                        val response = responseEngine.respond(input, isBubble = true)
                        addBreezyMsg(response)
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                }
            }
        }

        inputRow.addView(micBtn)
        inputRow.addView(inputField)
        inputRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) })
        inputRow.addView(sendBtn)

        chatContent.addView(scrollView)
        chatContent.addView(inputRow)
        root.addView(chatContent)
        setContentView(root)

        if (intent?.getBooleanExtra("VOICE_TRIGGER", false) == true) {
            toggleVoice()
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, 0, 0, ime.bottom)
            insets
        }
    }

    private fun addBreezyMsg(text: String) {
        messagesLayout.addView(TextView(this).apply {
            this.text = "🌬️ $text"
            setTextColor(ThemeManager.getTextPrimary(this@ChatBubbleActivity))
            textSize = 14f
            setPadding(0, dp(6), dp(32), dp(6))
        })
    }

    private fun addUserMsg(text: String) {
        messagesLayout.addView(TextView(this).apply {
            this.text = text
            setTextColor(ThemeManager.getTextSecondary(this@ChatBubbleActivity))
            textSize = 14f
            setPadding(dp(32), dp(6), 0, dp(6))
            gravity = Gravity.END
        })
    }

    private fun toggleVoice() {
        if (isRecording) {
            voiceEngine.stopListening()
            isRecording = false
        } else {
            isRecording = true
            voiceEngine.startListening(
                onResult = { text ->
                    isRecording = false
                    addUserMsg(text)
                    processInput(text)
                },
                onError = { err ->
                    isRecording = false
                    addBreezyMsg("Sorry, $err")
                }
            )
        }
    }

    private fun processInput(input: String) {
        lifecycleScope.launch {
            val response = responseEngine.respond(input, isBubble = true)
            addBreezyMsg(response)
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private data class QuickAction(val icon: String, val label: String, val onClick: () -> Unit)

    private fun createActionChip(action: QuickAction): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            
            addView(TextView(context).apply {
                text = action.icon
                textSize = 18f
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = action.label
                textSize = 10f
                setTextColor(ThemeManager.getTextSecondary(context))
                gravity = Gravity.CENTER
            })
            setOnClickListener { action.onClick() }
        }
    }

    private fun toggleMute() {
        val audio = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val current = audio.getStreamVolume(android.media.AudioManager.STREAM_RING)
        audio.setStreamVolume(android.media.AudioManager.STREAM_RING, if (current > 0) 0 else audio.getStreamMaxVolume(android.media.AudioManager.STREAM_RING) / 2, 0)
        Toast.makeText(this, if (current > 0) "Muted" else "Unmuted", Toast.LENGTH_SHORT).show()
    }

    private fun toggleDND() {
        SystemControls(this).setDND(!isDndEnabled())
    }

    private fun isDndEnabled(): Boolean {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        return nm.currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
    }

    private fun toggleWiFi() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startActivity(Intent(android.provider.Settings.Panel.ACTION_WIFI))
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
        }
    }

    private fun toggleBluetooth() {
        val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null) {
            if (btAdapter.isEnabled) btAdapter.disable() else btAdapter.enable()
        }
    }

    private fun setBrightness(percent: Int) {
        SystemControls(this).setBrightness(percent)
    }

    private fun openQuickSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine.destroy()
    }
}
