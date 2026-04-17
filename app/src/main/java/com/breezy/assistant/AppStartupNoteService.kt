package com.breezy.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class AppStartupNoteService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentOverlay: TextView? = null

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.breezy.assistant" || pkg == "android") return

        val memory = BreezyMemory(this)
        val note = memory.getFact("appnote_$pkg")
        if (note.isEmpty()) return

        showStartupNote(note)
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
        ).apply {
            gravity = Gravity.CENTER
            y = -200
        }

        try {
            windowManager.addView(overlay, params)
            currentOverlay = overlay

            // Fade in
            overlay.animate().alpha(1f).setDuration(300).start()

            // Fade out after 2.5 seconds
            handler.postDelayed({
                overlay.animate().alpha(0f).setDuration(300).withEndAction {
                    removeCurrentOverlay()
                }.start()
            }, 2500)
        } catch (e: Exception) {
            // Window already removed or permission issue
        }
    }

    private fun removeCurrentOverlay() {
        currentOverlay?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        currentOverlay = null
    }

    override fun onInterrupt() {}
}
