package com.breezy.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import java.util.*

class VoiceWakeService : Service() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(200, buildNotification())
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { restartListening() }
            override fun onError(error: Int) { restartListening() }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
                
                when {
                    text.contains("breezy") && (text.contains("lost my phone") || text.contains("find my phone")) -> {
                        triggerFindMyPhone()
                    }
                    text.contains("hey breezy") || text.contains("hey breezy") || text.contains("okay breezy") -> {
                        openVoiceConversation()
                    }
                }
                restartListening()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.lowercase()
                if (partial != null) {
                    if (partial.contains("breezy") && (partial.contains("lost") || partial.contains("find"))) {
                        // Early ring trigger possible
                        triggerFindMyPhone()
                    } else if (partial.contains("hey breezy") || partial.contains("okay breezy")) {
                        // Early open possible
                        openVoiceConversation()
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        startListeningIfAllowed()
    }

    private fun shouldListen(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return isScreenOn || isCharging
    }

    private fun startListeningIfAllowed() {
        if (!shouldListen()) {
            handler.postDelayed({ startListeningIfAllowed() }, 30000)
            return
        }
        if (isListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        speechRecognizer.startListening(intent)
        isListening = true
    }

    private fun restartListening() {
        isListening = false
        handler.postDelayed({ startListeningIfAllowed() }, 2000)
    }

    private fun triggerFindMyPhone() {
        SafeWordReceiver.triggerRing(this)
        val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    private fun openVoiceConversation() {
        startActivity(Intent(this, VoiceConversationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun buildNotification() = NotificationCompat.Builder(this, "voice_wake")
        .setContentTitle("Voice Wake Active")
        .setContentText("Listening for 'Hey Breezy' or 'Breezy I lost my phone'")
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel("voice_wake", "Voice Wake", NotificationManager.IMPORTANCE_LOW).apply {
                getSystemService(NotificationManager::class.java).createNotificationChannel(this)
            }
        }
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
