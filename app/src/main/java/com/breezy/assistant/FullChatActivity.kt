package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.*

class FullChatActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var messagesContainer: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var responseEngine: ResponseEngine
    private val memory by lazy { BreezyMemory(this) }
    private val chatHistory by lazy { ChatHistoryManager(this) }
    private val history = mutableListOf<Pair<String, String>>()
    private var voiceEngine: VoiceEngine? = null
    private var currentSessionId = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        responseEngine = ResponseEngine(this, BatteryMonitor(this))

        drawerLayout = DrawerLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Main content
        val mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@FullChatActivity))
        }

        mainContent.addView(buildHeaderWithDrawer())
        mainContent.addView(buildModelStatusBar())
        
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        messagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        scrollView.addView(messagesContainer)
        mainContent.addView(scrollView)
        mainContent.addView(buildInputBar())

        // Sidebar (left drawer)
        val sidebar = buildSidebar()

        drawerLayout.addView(mainContent)
        drawerLayout.addView(sidebar)

        val sidebarParams = sidebar.layoutParams as DrawerLayout.LayoutParams
        sidebarParams.width = dp(280)
        sidebarParams.gravity = GravityCompat.START
        sidebar.layoutParams = sidebarParams

        setContentView(drawerLayout)
        applySystemBarInsets(mainContent)

        // Load initial greeting if new session
        if (history.isEmpty()) {
            val battery = BatteryMonitor(this).getBatteryData()
            lifecycleScope.launch {
                val greeting = ResponsePool.getGreeting(memory.getTone(), memory.getUserName(), battery.level, battery.temperature)
                addBreezyMsg(greeting)
                history.add("breezy" to greeting)
            }
        }
        
        if (intent?.getBooleanExtra("VOICE_TRIGGER", false) == true) {
            startVoiceInput()
        }
        
        val prefill = intent?.getStringExtra("prefill")
        if (!prefill.isNullOrEmpty()) {
            inputField.setText(prefill)
        }
    }

    private fun buildHeaderWithDrawer(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundColor(ThemeManager.getBackgroundColor(this@FullChatActivity))

            // Hamburger menu
            addView(TextView(this@FullChatActivity).apply {
                text = "☰"
                textSize = 24f
                setTextColor(ThemeManager.getTextPrimary(this@FullChatActivity))
                setPadding(0, 0, dp(16), 0)
                setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
            })

            // Title
            addView(TextView(this@FullChatActivity).apply {
                text = "Breezy"
                textSize = 18f
                setTextColor(ThemeManager.getTextPrimary(this@FullChatActivity))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            // Share button
            addView(TextView(this@FullChatActivity).apply {
                text = "↗"
                textSize = 22f
                setTextColor(ThemeManager.getTextPrimary(this@FullChatActivity))
                setPadding(dp(12), 0, dp(12), 0)
                setOnClickListener { shareChat() }
            })

            // Voice call button (AI call)
            addView(TextView(this@FullChatActivity).apply {
                text = "📞"
                textSize = 22f
                setTextColor(ThemeManager.getAccentColor(this@FullChatActivity))
                setOnClickListener { startVoiceCall() }
            })
        }
    }

    private fun buildModelStatusBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(4), dp(16), dp(4))
            setBackgroundColor(ThemeManager.getCardColor(this@FullChatActivity))
            
            val llm = LLMInference(this@FullChatActivity)
            addView(TextView(this@FullChatActivity).apply {
                text = if (llm.isReady()) "🤖 Local AI · Active" else "☁️ Cloud Fallback · Active"
                textSize = 11f
                setTextColor(ThemeManager.getTextSecondary(this@FullChatActivity))
            })
        }
    }

    private fun buildInputBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(16))
            setBackgroundColor(ThemeManager.getBackgroundColor(this@FullChatActivity))

            inputField = EditText(this@FullChatActivity).apply {
                hint = "Message Breezy..."
                setHintTextColor(ThemeManager.getTextSecondary(this@FullChatActivity))
                setTextColor(ThemeManager.getTextPrimary(this@FullChatActivity))
                background = GradientDrawable().apply {
                    setColor(ThemeManager.getCardColor(this@FullChatActivity))
                    cornerRadius = cornerRadius().toFloat()
                }
                setPadding(dp(16), dp(12), dp(16), dp(12))
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnEditorActionListener { _, _, _ -> sendMessage(); true }
            }

            addView(inputField)

            // Voice input button
            addView(TextView(this@FullChatActivity).apply {
                text = "🎤"
                textSize = 22f
                setTextColor(ThemeManager.getTextSecondary(this@FullChatActivity))
                setPadding(dp(8), 0, dp(4), 0)
                setOnClickListener { startVoiceInput() }
            })

            // Send button
            addView(TextView(this@FullChatActivity).apply {
                text = "↑"
                textSize = 20f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ThemeManager.getAccentColor(this@FullChatActivity))
                }
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                setOnClickListener { sendMessage() }
            })
        }
    }

    private fun buildSidebar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getCardColor(this@FullChatActivity))
            layoutParams = DrawerLayout.LayoutParams(dp(280), ViewGroup.LayoutParams.MATCH_PARENT, GravityCompat.START)

            // Header
            addView(TextView(this@FullChatActivity).apply {
                text = "Breezy"
                textSize = 20f
                setTextColor(ThemeManager.getAccentColor(this@FullChatActivity))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(20), dp(24), 0, dp(8))
            })

            // New Chat button
            addView(TextView(this@FullChatActivity).apply {
                text = "+ New chat"
                textSize = 15f
                setTextColor(ThemeManager.getTextPrimary(this@FullChatActivity))
                background = GradientDrawable().apply {
                    setColor(ThemeManager.getAccentColor(this@FullChatActivity) and 0x22FFFFFF)
                    cornerRadius = dp(24).toFloat()
                }
                setPadding(dp(20), dp(12), dp(20), dp(12))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(12), dp(12), dp(12), dp(12))
                }
                setOnClickListener { newChat() }
            })

            // History list label
            addView(TextView(this@FullChatActivity).apply {
                text = "Recent"
                textSize = 12f
                setTextColor(ThemeManager.getTextSecondary(this@FullChatActivity))
                setPadding(dp(20), dp(16), 0, dp(4))
            })

            val historyList = LinearLayout(this@FullChatActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            loadHistoryItems(historyList)
            addView(ScrollView(this@FullChatActivity).apply { addView(historyList) })
        }
    }

    private fun loadHistoryItems(container: LinearLayout) {
        container.removeAllViews()
        val sessions = chatHistory.getAllSessions()
        if (sessions.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No recent chats"
                textSize = 13f
                setTextColor(ThemeManager.getTextSecondary(this@FullChatActivity))
                setPadding(dp(20), dp(10), dp(20), dp(10))
            })
        }
        sessions.forEach { session ->
            container.addView(TextView(this).apply {
                text = session.title
                textSize = 14f
                setTextColor(ThemeManager.getTextPrimary(this@FullChatActivity))
                setPadding(dp(20), dp(10), dp(20), dp(10))
                setOnClickListener { 
                    loadSession(session.id)
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
            })
        }
    }

    private fun sendMessage() {
        val input = inputField.text.toString().trim()
        if (input.isEmpty()) return
        
        if (history.isEmpty()) {
            // First message in session, save it to history
            chatHistory.saveSession(currentSessionId, if (input.length > 20) input.take(20) + "..." else input)
        }
        
        addUserMsg(input)
        inputField.setText("")
        history.add("user" to input)
        chatHistory.saveMessage(currentSessionId, "user", input)
        
        lifecycleScope.launch {
            val response = responseEngine.respondWithContext(input, history)
            addBreezyMsg(response)
            history.add("breezy" to response)
            chatHistory.saveMessage(currentSessionId, "breezy", response)
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun newChat() {
        currentSessionId = UUID.randomUUID().toString()
        history.clear()
        messagesContainer.removeAllViews()
        drawerLayout.closeDrawer(GravityCompat.START)
        
        val battery = BatteryMonitor(this).getBatteryData()
        lifecycleScope.launch {
            val greeting = ResponsePool.getGreeting(memory.getTone(), memory.getUserName(), battery.level, battery.temperature)
            addBreezyMsg(greeting)
            history.add("breezy" to greeting)
        }
    }

    private fun loadSession(id: String) {
        currentSessionId = id
        messagesContainer.removeAllViews()
        history.clear()
        
        val messages = chatHistory.getMessages(id)
        messages.forEach { msg ->
            if (msg.sender == "user") {
                addUserMsg(msg.text)
                history.add("user" to msg.text)
            } else {
                addBreezyMsg(msg.text)
                history.add("breezy" to msg.text)
            }
        }
    }

    private fun shareChat() {
        val chatText = history.joinToString("\n\n") { "${it.first.uppercase()}: ${it.second}" }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, chatText)
        }
        startActivity(Intent.createChooser(intent, "Share Chat"))
    }

    private fun startVoiceCall() {
        Toast.makeText(this, "Voice Call Mode Activated", Toast.LENGTH_SHORT).show()
        // Here we would implement continuous STT -> AI -> TTS
    }

    private fun startVoiceInput() {
        if (voiceEngine == null) voiceEngine = VoiceEngine(this)
        voiceEngine?.startListening(
            onResult = { text ->
                inputField.setText(text)
                sendMessage()
            },
            onError = { Toast.makeText(this, "Voice input failed", Toast.LENGTH_SHORT).show() }
        )
    }

    private fun addBreezyMsg(text: String) {
        if (text.isBlank()) return
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.setMargins(0, 0, dp(48), dp(12))
            }
        }
        
        // Avatar
        row.addView(View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ThemeManager.getAccentColor(this@FullChatActivity))
            }
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).also {
                it.setMargins(0, 0, dp(8), 0)
            }
        })
        
        // Bubble
        row.addView(TextView(this).apply {
            this.text = text; setTextColor(ThemeManager.getTextPrimary(this@FullChatActivity)); textSize = 15f
            setLineSpacing(0f, 1.2f)
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@FullChatActivity))
                cornerRadius = cornerRadius().toFloat()
                // Custom corners if desired, but rounded is fine for Oil Pastels
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
        })
        messagesContainer.addView(row)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun addUserMsg(text: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.setMargins(dp(48), 0, 0, dp(12))
            }
        }
        row.addView(TextView(this).apply {
            this.text = text; setTextColor(Color.WHITE); textSize = 15f
            setLineSpacing(0f, 1.2f)
            background = GradientDrawable().apply {
                setColor(ThemeManager.getAccentColor(this@FullChatActivity))
                cornerRadius = cornerRadius().toFloat()
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
        })
        messagesContainer.addView(row)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine?.destroy()
    }
}
