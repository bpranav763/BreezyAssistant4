package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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

class FullChatActivity : BaseActivity() {

    private lateinit var responseEngine: ResponseEngine
    private lateinit var messagesLayout: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var micBtn: TextView
    private val history = mutableListOf<Pair<String, String>>()
    private val memory  by lazy { BreezyMemory(this) }
    private var voiceEngine: VoiceEngine? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        responseEngine = ResponseEngine(this, BatteryMonitor(this))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF060C18.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        // Header — minimal, professional
        root.addView(buildChatHeader())

        // Divider
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 1)
            setBackgroundColor(0xFF111827.toInt())
        })

        // Messages
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f).toInt(), dp(12f).toInt(), dp(16f).toInt(), dp(12f).toInt())
        }
        scrollView.addView(messagesLayout)
        root.addView(scrollView)

        // Input bar
        root.addView(buildInputBar())

        setContentView(root)
        applySystemBarInsets(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, 0, 0, ime.bottom); insets
        }

        // Initial messages
        val battery = BatteryMonitor(this).getBatteryData()
        lifecycleScope.launch {
            addBreezyMsg(MorningReport(this@FullChatActivity).generateReport())
            addBreezyMsg(ResponsePool.getGreeting(memory.getTone(), memory.getUserName(), battery.level, battery.temperature))
        }

        if (intent?.getBooleanExtra("VOICE_TRIGGER", false) == true) {
            startVoice()
        }
    }

    private fun buildChatHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF060C18.toInt())
            setPadding(dp(16f).toInt(), dp(14f).toInt(), dp(16f).toInt(), dp(14f).toInt())

            // Back
            addView(TextView(this@FullChatActivity).apply {
                text = "←"; textSize = 20f; setTextColor(0xFF9CA3AF.toInt())
                setPadding(0, 0, dp(16f).toInt(), 0)
                setOnClickListener { finish() }
            })

            // Avatar dot
            addView(View(this@FullChatActivity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(memory.getBubbleColor())
                }
                layoutParams = LinearLayout.LayoutParams(dp(34f).toInt(), dp(34f).toInt()).also {
                    it.setMargins(0, 0, dp(12f).toInt(), 0)
                }
            })

            // Name + status
            val nameCol = LinearLayout(this@FullChatActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                addView(TextView(this@FullChatActivity).apply {
                    text = "Breezy"
                    textSize = 15f; setTextColor(0xFFE5E7EB.toInt())
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
                addView(TextView(this@FullChatActivity).apply {
                    text = "On-device · Private"
                    textSize = 11f; setTextColor(0xFF4B5563.toInt())
                })
            }
            addView(nameCol)

            // Model indicator
            val llm = LLMInference(this@FullChatActivity)
            addView(TextView(this@FullChatActivity).apply {
                text = if (llm.isReady()) "AI" else "Rule"
                textSize = 10f; setTextColor(if (llm.isReady()) 0xFF34D399.toInt() else 0xFF6B7280.toInt())
                background = GradientDrawable().apply {
                    setColor(if (llm.isReady()) 0xFF064E3B.toInt() else 0xFF1F2937.toInt())
                    cornerRadius = dp(4f)
                }
                setPadding(dp(8f).toInt(), dp(4f).toInt(), dp(8f).toInt(), dp(4f).toInt())
            })
        }
    }

    private fun buildInputBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF0D1117.toInt())
            setPadding(dp(12f).toInt(), dp(10f).toInt(), dp(12f).toInt(), dp(10f).toInt())
            gravity = Gravity.CENTER_VERTICAL

            inputField = EditText(this@FullChatActivity).apply {
                hint = "Message"
                setHintTextColor(0xFF374151.toInt()); setTextColor(0xFFE5E7EB.toInt())
                background = GradientDrawable().apply {
                    setColor(0xFF111827.toInt()); cornerRadius = dp(22f)
                }
                setPadding(dp(16f).toInt(), dp(10f).toInt(), dp(12f).toInt(), dp(10f).toInt())
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                setOnEditorActionListener { _, _, _ -> sendMessage(); true }
            }
            addView(inputField)

            // Mic button
            micBtn = TextView(this@FullChatActivity).apply {
                text = "🎙"
                textSize = 16f; gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(0xFF111827.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(dp(42f).toInt(), dp(42f).toInt()).also {
                    it.setMargins(dp(8f).toInt(), 0, dp(8f).toInt(), 0)
                }
                setOnClickListener { toggleVoice() }
            }
            addView(micBtn)

            // Send
            addView(TextView(this@FullChatActivity).apply {
                text = "↑"; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(memory.getBubbleColor())
                }
                layoutParams = LinearLayout.LayoutParams(dp(42f).toInt(), dp(42f).toInt())
                setOnClickListener { sendMessage() }
            })
        }
    }

    private fun sendMessage() {
        val input = inputField.text.toString().trim()
        if (input.isEmpty()) return
        addUserMsg(input); inputField.setText("")
        history.add("user" to input)
        lifecycleScope.launch {
            val response = responseEngine.respondWithContext(input, history)
            addBreezyMsg(response)
            history.add("breezy" to response)
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun toggleVoice() {
        if (isListening) {
            voiceEngine?.stopListening(); isListening = false
            micBtn.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(0xFF111827.toInt())
            }
        } else {
            startVoice()
        }
    }

    private fun startVoice() {
        if (voiceEngine == null) voiceEngine = VoiceEngine(this)
        isListening = true
        micBtn.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(0xFF1D4ED8.toInt())
        }
        voiceEngine?.startListening(
            onResult = { text ->
                isListening = false
                micBtn.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(0xFF111827.toInt())
                }
                addUserMsg(text)
                history.add("user" to text)
                lifecycleScope.launch {
                    val response = responseEngine.respond(text)
                    addBreezyMsg(response)
                    history.add("breezy" to response)
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            },
            onError = {
                isListening = false
                micBtn.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(0xFF111827.toInt())
                }
            }
        )
    }

    private fun addBreezyMsg(text: String) {
        if (text.isBlank()) return
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(-1, -2).also {
                it.setMargins(0, 0, dp(56f).toInt(), dp(8f).toInt())
            }
        }
        // Avatar
        row.addView(View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(memory.getBubbleColor())
            }
            layoutParams = LinearLayout.LayoutParams(dp(26f).toInt(), dp(26f).toInt()).also {
                it.setMargins(0, 0, dp(8f).toInt(), 0)
            }
        })
        // Bubble
        row.addView(TextView(this).apply {
            this.text = text; setTextColor(0xFFE5E7EB.toInt()); textSize = 14f
            setLineSpacing(0f, 1.4f)
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt()); cornerRadius = dp(16f)
                // flat bottom-left corner
                cornerRadii = floatArrayOf(dp(16f), dp(16f), dp(16f), dp(16f), dp(16f), dp(16f), 0f, 0f)
            }
            setPadding(dp(14f).toInt(), dp(10f).toInt(), dp(14f).toInt(), dp(10f).toInt())
        })
        messagesLayout.addView(row)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun addUserMsg(text: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(-1, -2).also {
                it.setMargins(dp(56f).toInt(), 0, 0, dp(8f).toInt())
            }
        }
        row.addView(TextView(this).apply {
            this.text = text; setTextColor(Color.WHITE); textSize = 14f
            setLineSpacing(0f, 1.4f)
            background = GradientDrawable().apply {
                setColor(memory.getBubbleColor())
                cornerRadii = floatArrayOf(dp(16f), dp(16f), 0f, 0f, dp(16f), dp(16f), dp(16f), dp(16f))
            }
            setPadding(dp(14f).toInt(), dp(10f).toInt(), dp(14f).toInt(), dp(10f).toInt())
        })
        messagesLayout.addView(row)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun dp(v: Float) = (v * resources.displayMetrics.density)
    override fun onDestroy() { super.onDestroy(); voiceEngine?.destroy() }
}
