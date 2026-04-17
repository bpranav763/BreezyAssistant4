package com.breezy.assistant

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class OnboardingActivity : BaseActivity() {

  private lateinit var memory: BreezyMemory
  private lateinit var messagesLayout: LinearLayout
  private lateinit var scrollView: ScrollView
  private lateinit var inputField: EditText
  private lateinit var sendBtn: Button
  private var step = 0

  private val questions = listOf(
      "Hey! I'm Breezy. What should I call you?",
      "Nice to meet you! How should I talk to you?\n\n1. Friendly & warm\n2. Witty & playful\n3. Professional\n4. Calm & minimal",
      "Last one — where are you based?\n\n1. India\n2. Philippines\n3. United States\n4. Somewhere else"
  )

  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      memory = BreezyMemory(this)

      val root = LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setBackgroundColor(0xFF0A0F1E.toInt())
          setPadding(dp(24), dp(64), dp(24), dp(24))
      }

      val title = TextView(this).apply {
          text = "🌬️ Welcome to Breezy"
          textSize = 22f
          setTextColor(0xFF1D4ED8.toInt())
          setPadding(0, 0, 0, dp(24))
          gravity = Gravity.CENTER
      }

      scrollView = ScrollView(this).apply {
          layoutParams = LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
      }

      messagesLayout = LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(dp(8), dp(8), dp(8), dp(8))
      }
      scrollView.addView(messagesLayout)

      val inputRow = LinearLayout(this).apply {
          orientation = LinearLayout.HORIZONTAL
          setPadding(0, dp(16), 0, 0)
      }

      inputField = EditText(this).apply {
          hint = "Type here..."
          setHintTextColor(0xFF6B7280.toInt())
          setTextColor(android.graphics.Color.WHITE)
          setBackgroundColor(0xFF1F2937.toInt())
          setPadding(dp(16), dp(12), dp(16), dp(12))
          layoutParams = LinearLayout.LayoutParams(0,
              LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
      }

      sendBtn = Button(this).apply {
          text = "→"
          setBackgroundColor(0xFF1D4ED8.toInt())
          setTextColor(android.graphics.Color.WHITE)
          layoutParams = LinearLayout.LayoutParams(dp(120),
              LinearLayout.LayoutParams.WRAP_CONTENT)
          setOnClickListener { handleInput() }
      }

      inputRow.addView(inputField)
      inputRow.addView(sendBtn)

      root.addView(title)
      root.addView(scrollView)
      root.addView(inputRow)
      setContentView(root)
      applySystemBarInsets(root)
      
      addBreezyMessage(questions[0])
  }

  private fun handleInput() {
      val input = inputField.text.toString().trim()
      if (input.isEmpty()) return
      addUserMessage(input)
      inputField.setText("")

      when (step) {
          0 -> {
              val name = input.split(" ").first()
              memory.saveUserName(name)
              step++
              addBreezyMessage("Love that name $name! ${questions[1]}")
          }
          1 -> {
              val tone = when {
                  input.contains("1") || input.lowercase().contains("friend") -> "friendly"
                  input.contains("2") || input.lowercase().contains("wit") -> "witty"
                  input.contains("3") || input.lowercase().contains("pro") -> "professional"
                  input.contains("4") || input.lowercase().contains("calm") -> "calm"
                  else -> "friendly"
              }
              memory.saveTone(tone)
              step++
              addBreezyMessage("${tone.replaceFirstChar { it.uppercase() }} it is! ${questions[2]}")
          }
          2 -> {
              val region = when {
                  input.contains("1") || input.lowercase().contains("india") -> "IN"
                  input.contains("2") || input.lowercase().contains("phil") -> "PH"
                  input.contains("3") || input.lowercase().contains("us") ||
                  input.lowercase().contains("america") -> "US"
                  else -> "OTHER"
              }
              memory.saveRegion(region)
              memory.setOnboardingDone()
              addBreezyMessage("Perfect. I'm ready to watch over your phone ${memory.getUserName()}. Let's go.")
              android.os.Handler(mainLooper).postDelayed({
                  startActivity(Intent(this, MainActivity::class.java))
                  finish()
              }, 1500)
          }
      }
      scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
  }

  private fun addBreezyMessage(text: String) {
      messagesLayout.addView(TextView(this).apply {
          this.text = "🌬️ $text"
          setTextColor(0xFFDBEAFE.toInt())
          textSize = 15f
          setPadding(dp(12), dp(10), dp(48), dp(10))
      })
  }

  private fun addUserMessage(text: String) {
      messagesLayout.addView(TextView(this).apply {
          this.text = text
          setTextColor(android.graphics.Color.WHITE)
          textSize = 15f
          setPadding(dp(48), dp(10), dp(12), dp(10))
          gravity = Gravity.END
      })
  }
}
