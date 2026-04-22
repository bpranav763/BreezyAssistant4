package com.breezy.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class AppStartupNoteService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentOverlay: TextView? = null

    // Volume multi-press detection
    private var lastVolDownTime = 0L
    private var volDownCount    = 0
    private var lastVolUpTime   = 0L
    private val MULTI_PRESS_MS  = 500L
    private var longPressRunnable: Runnable? = null
    private val LONG_PRESS_MS = 700L

    private val executor by lazy { TriggerExecutor(this) }
    private val storage by lazy { TriggerStorage(this) }

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes        = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType      = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags             = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
    }

    // ── Volume button interception ───────────────────────────────────────────

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val now = System.currentTimeMillis()

        when (event.keyCode) {

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    // Schedule long-press: open voice input
                    longPressRunnable = Runnable { 
                        if (!fireTriggers(BreezyTrigger.TriggerType.VOLUME_LONG_DOWN)) {
                            openVoiceInput() 
                        }
                    }
                    handler.postDelayed(longPressRunnable!!, LONG_PRESS_MS)
                }
                if (event.action == KeyEvent.ACTION_UP) {
                    longPressRunnable?.let { handler.removeCallbacks(it) }

                    // Detect multi-press
                    if (now - lastVolDownTime < MULTI_PRESS_MS) {
                        volDownCount++
                        if (volDownCount >= 6) {
                            triggerForceEFlare()
                            volDownCount = 0
                            return true
                        }
                        if (volDownCount == 1) { // This means second press
                             // wait for more? or handle double now?
                        }
                    } else {
                        volDownCount = 0
                    }
                    
                    if (now - lastVolDownTime < MULTI_PRESS_MS && volDownCount == 1) {
                         // Double down detected
                         if (!fireTriggers(BreezyTrigger.TriggerType.VOLUME_DOUBLE_DOWN)) {
                             openBreezy()
                         }
                         lastVolDownTime = 0L
                         return true
                    }

                    lastVolDownTime = now
                }
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    if (now - lastVolUpTime < MULTI_PRESS_MS) {
                        if (!fireTriggers(BreezyTrigger.TriggerType.VOLUME_DOUBLE_UP)) {
                            val intent = Intent(this, SecurityActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                startActivity(Intent(this, ObserveActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        }
                        lastVolUpTime = 0L
                        return true
                    }
                    lastVolUpTime = now
                }
            }
        }

        return false   // don't consume by default
    }

    private fun fireTriggers(type: BreezyTrigger.TriggerType): Boolean {
        val triggers = storage.getTriggersForType(type)
        if (triggers.isEmpty()) return false
        triggers.forEach { executor.execute(it) }
        return true
    }

    private fun openBreezy() {
        startActivity(Intent(this, ChatBubbleActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun openVoiceInput() {
        // Flag to ChatBubbleActivity that it should auto-start voice
        startActivity(Intent(this, ChatBubbleActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("VOICE_TRIGGER", true)
        })
    }

    // ── App startup note overlay (unchanged logic) ───────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.breezy.assistant" || pkg == "android") return
        
        // 1. Run dynamic app-open triggers
        val appTriggers = storage.getTriggersForType(BreezyTrigger.TriggerType.APP_OPEN)
        appTriggers.forEach { trigger ->
            if (trigger.triggerParam.isNotBlank() && pkg.contains(trigger.triggerParam)) {
                executor.execute(trigger)
            }
        }

        // 2. Show startup note
        val memory = BreezyMemory(this)
        val note = memory.getFact("appnote_$pkg")
        if (note.isNotEmpty()) {
            showStartupNote(note)
        }
    }

    private fun showStartupNote(note: String) {
        removeCurrentOverlay()
        val overlay = TextView(this).apply {
            text = note
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setShadowLayer(8f, 0f, 0f, 0xFF000000.toInt())
            alpha = 0f
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER; y = -200 }

        try {
            windowManager.addView(overlay, params)
            currentOverlay = overlay
            overlay.animate().alpha(1f).setDuration(300).start()
            handler.postDelayed({
                overlay.animate().alpha(0f).setDuration(300)
                    .withEndAction { removeCurrentOverlay() }.start()
            }, 2500)
        } catch (_: Exception) {}
    }

    private fun removeCurrentOverlay() {
        currentOverlay?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        currentOverlay = null
    }

    private fun triggerForceEFlare() {
        val intent = Intent(this, EFlareService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    override fun onInterrupt() {}
}
