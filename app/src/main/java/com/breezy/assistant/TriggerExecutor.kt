package com.breezy.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat

class TriggerExecutor(private val context: Context) {

    private val controls = SystemControls(context)
    private val memory = BreezyMemory(context)

    fun execute(trigger: BreezyTrigger) {
        Log.d("TriggerExecutor", "Executing: ${trigger.name} → ${trigger.actionType}")
        when (trigger.actionType) {
            BreezyTrigger.ActionType.OPEN_BREEZY -> openBreezy()
            BreezyTrigger.ActionType.OPEN_APP    -> openApp(trigger.actionParam)
            BreezyTrigger.ActionType.TOGGLE_WIFI -> toggleWifi()
            BreezyTrigger.ActionType.TOGGLE_DND  -> toggleDnd()
            BreezyTrigger.ActionType.SET_VOLUME  -> setVolume(trigger.actionParam.toIntOrNull() ?: 50)
            BreezyTrigger.ActionType.SET_BRIGHTNESS -> setBrightness(trigger.actionParam.toIntOrNull() ?: 50)
            BreezyTrigger.ActionType.SECURITY_SCAN -> scheduleSecurityScan()
            BreezyTrigger.ActionType.RING_PHONE  -> ringPhone()
            BreezyTrigger.ActionType.SEND_SMS    -> sendSms(trigger.actionParam)
            BreezyTrigger.ActionType.SHOW_NOTIFICATION -> showNotification(trigger.name, trigger.actionParam)
            BreezyTrigger.ActionType.VOICE_INPUT -> openVoiceInput()
        }
        memory.saveFact("last_trigger_run", "${trigger.name} at ${System.currentTimeMillis()}")
    }

    private fun openBreezy() {
        context.startActivity(Intent(context, ChatBubbleActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun openApp(packageName: String) {
        if (packageName.isBlank()) {
            openBreezy()
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            showNotification("Breezy", "App not found: $packageName")
        }
    }

    private fun toggleWifi() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun toggleDnd() {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.isNotificationPolicyAccessGranted) {
            val current = nm.currentInterruptionFilter
            val next = if (current == NotificationManager.INTERRUPTION_FILTER_NONE)
                NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_NONE
            nm.setInterruptionFilter(next)
        }
    }

    private fun setVolume(percent: Int) {
        val audio = context.getSystemService(AudioManager::class.java)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (max * percent / 100f).toInt().coerceIn(0, max), 0)
    }

    private fun setBrightness(percent: Int) {
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (255 * percent / 100f).toInt().coerceIn(0, 255)
            )
        }
    }

    private fun scheduleSecurityScan() {
        Thread {
            val scanner = AntiStalkerScanner(context)
            val threats = scanner.scanForThreats()
            val msg = if (threats.isEmpty()) "No threats found." else "⚠️ ${threats.size} threats detected!"
            showNotification("Breezy Security Scan", msg)
        }.start()
    }

    private fun ringPhone() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_RING, audio.getStreamMaxVolume(AudioManager.STREAM_RING), 0)
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        try {
            val mp = MediaPlayer().apply {
                setDataSource(context, uri)
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_RING)
                isLooping = true
                prepare()
                start()
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ mp.release() }, 30_000)
        } catch (_: Exception) {}
    }

    private fun sendSms(param: String) {
        val parts = param.split("::", limit = 2)
        if (parts.size != 2) return
        val number = parts[0].trim()
        val message = parts[1].trim()
        try {
            @Suppress("DEPRECATION")
            SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
        } catch (_: Exception) {}
    }

    private fun showNotification(title: String, text: String) {
        val channelId = "breezy_triggers"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Breezy Triggers", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun openVoiceInput() {
        context.startActivity(Intent(context, ChatBubbleActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("VOICE_TRIGGER", true)
        })
    }
}
