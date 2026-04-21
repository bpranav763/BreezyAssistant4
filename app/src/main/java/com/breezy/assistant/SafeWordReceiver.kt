package com.breezy.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.telephony.SmsMessage

class SafeWordReceiver : BroadcastReceiver() {

    companion object {
        private var mediaPlayer: MediaPlayer? = null
        private val handler = Handler(Looper.getMainLooper())

        // Safe words via SMS — text any of these to your number
        private val SMS_SAFE_WORDS = listOf(
            "breezy ring", "find my phone", "breezy find",
            "ring phone", "breezy breezy"
        )

        fun stopRinging() {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        }

        fun triggerRing(context: Context) {
            // Max volume
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.setStreamVolume(
                AudioManager.STREAM_RING,
                audio.getStreamMaxVolume(AudioManager.STREAM_RING), 0
            )

            // Ring
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioStreamType(AudioManager.STREAM_RING)
                isLooping = true
                prepare()
                start()
            }

            // Stop after 30s automatically
            handler.postDelayed({ stopRinging() }, 30_000)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return

        val storage = TriggerStorage(context)
        val executor = TriggerExecutor(context)
        val smsTriggers = storage.getTriggersForType(BreezyTrigger.TriggerType.SMS_KEYWORD)

        pdus.forEach { pdu ->
            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
            val body = sms.messageBody.lowercase().trim()

            // 1. Run dynamic triggers
            smsTriggers.forEach { trigger ->
                if (trigger.triggerParam.isNotBlank() && body.contains(trigger.triggerParam.lowercase())) {
                    executor.execute(trigger)
                }
            }

            // 2. Fallback to hardcoded safe words
            if (SMS_SAFE_WORDS.any { body.contains(it) }) {
                if (body.contains("breezy breezy")) {
                    BreezyAccessibilityService.instance?.performPanicWipe()
                } else {
                    triggerRing(context)
                }
                return
            }
        }
    }
}
