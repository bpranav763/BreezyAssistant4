package com.breezy.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class PrivacyScreenService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val prefs by lazy { getSharedPreferences("privacy_prefs", MODE_PRIVATE) }

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(99, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intensity = intent?.getIntExtra("intensity", 80) ?: prefs.getInt("intensity", 80)
        addOverlay(intensity)
        return START_STICKY
    }

    private fun addOverlay(intensity: Int) {
        overlayView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }

        val alpha = (intensity * 255 / 100).coerceIn(0, 255)
        val edgeColor = Color.argb(alpha, 0, 0, 0)

        val overlay = object : View(this) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val h = height.toFloat()
                val paint = Paint()

                // Left gradient
                paint.shader = LinearGradient(0f, 0f, w * 0.35f, 0f,
                    edgeColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w * 0.35f, h, paint)

                // Right gradient
                paint.shader = LinearGradient(w * 0.65f, 0f, w, 0f,
                    Color.TRANSPARENT, edgeColor, Shader.TileMode.CLAMP)
                canvas.drawRect(w * 0.65f, 0f, w, h, paint)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(overlay, params)
        overlayView = overlay
    }

    private fun buildNotification() = NotificationCompat.Builder(this, "privacy_screen")
        .setContentTitle("Privacy Screen Active")
        .setContentText("Side-view protection is on")
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel("privacy_screen", "Privacy Screen",
                NotificationManager.IMPORTANCE_LOW).apply {
                getSystemService(NotificationManager::class.java).createNotificationChannel(this)
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        overlayView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
