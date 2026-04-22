package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.*

class VoiceConversationActivity : BaseActivity(), TextToSpeech.OnInitListener {

    private lateinit var responseEngine: ResponseEngine
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var transcriptView: TextView
    private lateinit var waveformView: View
    private var isListening = false
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildVoiceUI())

        responseEngine = ResponseEngine(this, BatteryMonitor(this))
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()

        // Start the conversation with a greeting
        handler.postDelayed({
            speakAndListen("Hi ${BreezyMemory(this).getUserName()}, I'm listening.")
        }, 1000)
    }

    private fun buildVoiceUI(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(context))
            setPadding(dp(24), dp(40), dp(24), dp(24))

            // Header with close and keyboard buttons
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(TextView(context).apply {
                    text = "←"
                    textSize = 24f
                    setTextColor(ThemeManager.getTextPrimary(context))
                    setOnClickListener { finish() }
                })
                addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
                addView(TextView(context).apply {
                    text = "⌨️"
                    textSize = 24f
                    setTextColor(ThemeManager.getTextPrimary(context))
                    setOnClickListener { switchToTypingMode() }
                })
            })

            // Transcript area
            transcriptView = TextView(context).apply {
                textSize = 18f
                setTextColor(ThemeManager.getTextPrimary(context))
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
                gravity = Gravity.CENTER
            }
            addView(transcriptView)

            // Waveform animation (pulsing circle)
            waveformView = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ThemeManager.getAccentColor(context))
                }
                layoutParams = LinearLayout.LayoutParams(dp(80), dp(80)).apply {
                    gravity = Gravity.CENTER
                    topMargin = dp(40)
                }
            }
            addView(waveformView)

            // Hint text
            addView(TextView(context).apply {
                text = "Listening..."
                textSize = 14f
                setTextColor(ThemeManager.getTextSecondary(context))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) }
            })
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                animateWaveform(true)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    transcriptView.text = "You: $text"
                    conversationHistory.add("user" to text)
                    processUserInput(text)
                } else {
                    speakAndListen("I didn't catch that. Could you repeat?")
                }
            }
            override fun onError(error: Int) {
                isListening = false
                animateWaveform(false)
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "I didn't hear anything. Try again?"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Let's try once more."
                    else -> "Something went wrong. Let's continue."
                }
                speakAndListen(msg)
            }
            override fun onEndOfSpeech() {
                isListening = false
                animateWaveform(false)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processUserInput(input: String) {
        lifecycleScope.launch {
            val response = responseEngine.respondWithContext(input, conversationHistory)
            conversationHistory.add("breezy" to response)
            speakAndListen(response)
        }
    }

    private fun speakAndListen(text: String) {
        transcriptView.text = "Breezy: $text"
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "BREEZY_UTTERANCE")
        }
        
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                runOnUiThread { startListening() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                runOnUiThread { startListening() }
            }
        })
    }

    private fun startListening() {
        if (isListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer.startListening(intent)
    }

    private fun animateWaveform(active: Boolean) {
        waveformView.animate().apply {
            scaleX(if (active) 1.2f else 1.0f)
            scaleY(if (active) 1.2f else 1.0f)
            alpha(if (active) 1.0f else 0.6f)
            duration = 300
        }.start()
    }

    private fun switchToTypingMode() {
        val intent = Intent(this, FullChatActivity::class.java).apply {
            // Pass conversation history so it continues seamlessly
            val historyStrings = ArrayList<String>()
            conversationHistory.forEach { historyStrings.add("${it.first}|${it.second}") }
            putStringArrayListExtra("history_context", historyStrings)
        }
        startActivity(intent)
        finish()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        speechRecognizer.destroy()
        super.onDestroy()
    }
    
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
}
