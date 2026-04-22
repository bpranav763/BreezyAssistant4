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
import android.speech.RecognizerIntent
import androidx.core.app.NotificationCompat
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException
import java.util.*

class VoiceWakeService : Service(), RecognitionListener {
    private var speechService: SpeechService? = null
    private var model: Model? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(200, buildNotification())
        initVosk()
    }

    private fun initVosk() {
        StorageService.unpack(this, "vosk-model-small-en-us-0.15", "model",
            { model: Model ->
                this.model = model
                startVoskService()
            },
            { exception: IOException ->
                // Log or handle error
            })
    }

    private fun startVoskService() {
        if (speechService != null) return
        try {
            val rec = Recognizer(model, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
        } catch (e: IOException) {
            // Handle error
        }
    }

    override fun onResult(hypothesis: String) {
        processHypothesis(hypothesis)
    }

    override fun onPartialResult(hypothesis: String) {
        processHypothesis(hypothesis)
    }

    override fun onFinalResult(hypothesis: String) {
        processHypothesis(hypothesis)
    }

    override fun onError(exception: Exception) {
        // Handle error
    }

    override fun onTimeout() {
        // Handle timeout
    }

    private fun processHypothesis(hypothesis: String) {
        val text = hypothesis.lowercase(Locale.getDefault())
        if (text.contains("breezy")) {
            if (text.contains("lost") || text.contains("find")) {
                triggerFindMyPhone()
            } else if (text.contains("hey") || text.contains("okay")) {
                openVoiceConversation()
            }
        }
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
        speechService?.let {
            it.stop()
            it.shutdown()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
