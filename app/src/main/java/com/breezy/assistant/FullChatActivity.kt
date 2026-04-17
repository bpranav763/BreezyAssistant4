package com.breezy.assistant

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
    private val history = mutableListOf<Pair<String, String>>()
    private val memory by lazy { BreezyMemory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        responseEngine = ResponseEngine(this, BatteryMonitor(this))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Header
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), 0, dp(24), dp(16))
            setBackgroundColor(0xFF0A0F1E.toInt())
            addView(TextView(this@FullChatActivity).apply {
                text = "←"; textSize = 22f; setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 0, dp(16), 0); setOnClickListener { finish() }
            })
            val dot = View(this@FullChatActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).also {
                    it.setMargins(0, 0, dp(14), 0)
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFF1D4ED8.toInt())
                }
            }
            val nameCol = LinearLayout(this@FullChatActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@FullChatActivity).apply {
                    text = "Breezy"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
                addView(TextView(this@FullChatActivity).apply {
                    text = "Your protective companion"; textSize = 11f; setTextColor(0xFF6B7280.toInt())
                })
            }
            addView(dot); addView(nameCol)
        })

        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(0xFF1F2937.toInt())
        })

        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        scrollView.addView(messagesLayout)
        root.addView(scrollView)

        // Morning report
        addSystemCard("📋 Phone Report", MorningReport(this).generateReport())

        // Greeting
        val d = BatteryMonitor(this).getBatteryData()
        addBreezyMsg(ResponsePool.getGreeting(memory.getTone(), memory.getUserName(), d.level, d.temperature))

        // Input
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF111827.toInt())
            setPadding(dp(16), dp(10), dp(10), dp(10))
            gravity = Gravity.CENTER_VERTICAL

            inputField = EditText(this@FullChatActivity).apply {
                hint = "Message Breezy..."
                setHintTextColor(0xFF4B5563.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x00000000)
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(8), dp(12), dp(8), dp(12))
            }

            val send = TextView(this@FullChatActivity).apply {
                text = "↑"; textSize = 18f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFF1D4ED8.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                setOnClickListener { sendMessage() }
            }
            addView(inputField)
            addView(View(this@FullChatActivity).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) })
            addView(send)
        })

        setContentView(root)
        applySystemBarInsets(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, 0, 0, ime.bottom); insets
        }
    }

    private fun sendMessage() {
        val input = inputField.text.toString().trim()
        if (input.isEmpty()) return
        addUserMsg(input)
        inputField.setText("")
        history.add("user" to input)
        
        lifecycleScope.launch {
            val response = responseEngine.respondWithContext(input, history)
            addBreezyMsg(response)
            history.add("breezy" to response)
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun addSystemCard(title: String, content: String) {
        messagesLayout.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF111827.toInt()); cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(20), dp(16), dp(20), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dp(16)) }
            addView(TextView(this@FullChatActivity).apply {
                text = title; textSize = 13f; setTextColor(0xFF1D4ED8.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, dp(8))
            })
            addView(TextView(this@FullChatActivity).apply {
                text = content; textSize = 13f; setTextColor(0xFF9CA3AF.toInt())
            })
        })
    }

    private fun addBreezyMsg(text: String) {
        messagesLayout.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, dp(4), dp(48), dp(4)) }
            addView(View(this@FullChatActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).also {
                    it.setMargins(0, 0, dp(8), 0)
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFF1D4ED8.toInt())
                }
            })
            addView(TextView(this@FullChatActivity).apply {
                this.text = text; setTextColor(0xFFFFFFFF.toInt()); textSize = 14f
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFF1F2937.toInt()); cornerRadius = dp(18).toFloat()
                }
                setPadding(dp(16), dp(12), dp(16), dp(12))
            })
        })
    }

    private fun addUserMsg(text: String) {
        messagesLayout.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(dp(48), dp(4), 0, dp(4)) }
            addView(TextView(this@FullChatActivity).apply {
                this.text = text; setTextColor(0xFFFFFFFF.toInt()); textSize = 14f
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(18).toFloat()
                }
                setPadding(dp(16), dp(12), dp(16), dp(12))
            })
        })
    }
}
