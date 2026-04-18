package com.breezy.assistant

import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.app.NotificationCompat
import kotlin.math.cos
import kotlin.math.sin

class FloatingCircleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isHidden = false
    private var radialMenuShowing = false
    private val radialViews = mutableListOf<Pair<View, WindowManager.LayoutParams>>()
    private var activeRadialIndex = -1
    private var initialX = 0; private var initialY = 0
    private var initialTouchX = 0f; private var initialTouchY = 0f
    private var longPressRunnable: Runnable? = null
    private val LONG_PRESS_MS = 380L
    private val hideRunnable = Runnable { animateToSliver() }
    private lateinit var crisisEngine: CrisisEngine
    private lateinit var moduleManager: ModuleManager
    private val memory by lazy { BreezyMemory(this) }
    private val storage by lazy { TriggerStorage(this) }
    private val executor by lazy { TriggerExecutor(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundNow()
        setupFloatingCircle()
        resetHideTimer()
        startCameraMonitor()
        startCrisisMonitor()
        setupModuleManager()
        startSystemMonitoring()
    }

    private fun startSystemMonitoring() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkSystemTriggers()
                handler.postDelayed(this, 30_000) // Every 30s
            }
        }, 10_000)
    }

    private fun checkSystemTriggers() {
        val batteryData = BatteryMonitor(this).getBatteryData()
        
        // Battery Below
        storage.getTriggersForType(BreezyTrigger.TriggerType.BATTERY_BELOW).forEach {
            val threshold = it.triggerParam.toIntOrNull() ?: 20
            if (batteryData.level <= threshold) executor.execute(it)
        }

        // Temp Above
        storage.getTriggersForType(BreezyTrigger.TriggerType.TEMP_ABOVE).forEach {
            val threshold = it.triggerParam.toIntOrNull() ?: 45
            if (batteryData.temperature >= threshold) executor.execute(it)
        }

        // Charging
        if (batteryData.isCharging) {
            storage.getTriggersForType(BreezyTrigger.TriggerType.CHARGING_START).forEach { executor.execute(it) }
        } else {
            // This might fire too often if not careful, but for now simple
        }
    }

    private fun setupModuleManager() {
        moduleManager = ModuleManager(this)
        moduleManager.setAlertListener { alert ->
            handler.post {
                BreezyMemory(this).saveFact("last_security_alert", alert)
                alertVisuals()
            }
        }
        moduleManager.registerBuiltins()
    }

    private fun startCrisisMonitor() {
        crisisEngine = CrisisEngine(this)
        handler.postDelayed(object : Runnable {
            override fun run() {
                crisisEngine.checkAllSystems(object : CrisisEngine.CrisisListener {
                    override fun onCrisisDetected(type: CrisisEngine.CrisisType, message: String) {
                        BreezyMemory(this@FloatingCircleService).saveFact("last_security_alert", message)
                        alertVisuals()
                    }
                })
                handler.postDelayed(this, 10000) // Check every 10s
            }
        }, 5000)
    }

    private fun alertVisuals() {
        if (::floatingView.isInitialized) {
            floatingView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFEF4444.toInt()) // Red alert color
            }
            floatingView.backgroundTintList = null
            floatingView.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200)
                .withEndAction {
                    floatingView.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                }.start()
            
            // Revert back to blue after 3 seconds
            handler.postDelayed({
                if (!isHidden && !radialMenuShowing) {
                    floatingView.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL; setColor(memory.getBubbleColor())
                    }
                    floatingView.backgroundTintList = null
                }
            }, 3000)
        }
    }

    private fun startCameraMonitor() {
        CameraMicMonitor(this).startMonitoring { alert ->
            handler.post {
                if (::floatingView.isInitialized) {
                    floatingView.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150)
                        .withEndAction {
                            floatingView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                        }.start()
                }
            }
            BreezyMemory(this).saveFact("last_security_alert", alert)
        }
    }

    private fun resetHideTimer() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, memory.getBubbleIdleTime() * 1000L)
    }

    private fun animateToSliver() {
        if (radialMenuShowing) return
        isHidden = true
        val sw = resources.displayMetrics.widthPixels
        val isLeft = params.x < sw / 2
        val tw = dp(memory.getBubbleIdleSize()); val th = dp(48)
        val startW = params.width; val startH = params.height

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280; interpolator = DecelerateInterpolator()
            addUpdateListener {
                val p = it.animatedValue as Float
                params.width = (startW + (tw - startW) * p).toInt()
                params.height = (startH + (th - startH) * p).toInt()
                params.alpha = 1f - p * 0.4f
                params.x = if (isLeft) 0 else sw - params.width
                safeUpdate()
            }
            start()
        }
        floatingView.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            val baseColor = memory.getBubbleColor()
            val alphaColor = (0xCC shl 24) or (baseColor and 0x00FFFFFF)
            setColor(alphaColor)
            val r = dp(80).toFloat()
            cornerRadii = if (isLeft)
                floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
            else floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
        }
        floatingView.backgroundTintList = null
    }

    private fun showFully() {
        isHidden = false
        val sw = resources.displayMetrics.widthPixels
        val target = dp(memory.getBubbleSize()); val sw2 = params.width; val sh2 = params.height

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220; interpolator = OvershootInterpolator(1.5f)
            addUpdateListener {
                val p = it.animatedValue as Float
                params.width = (sw2 + (target - sw2) * p).toInt()
                params.height = (sh2 + (target - sh2) * p).toInt()
                params.alpha = minOf(1f, params.alpha + 0.1f)
                params.x = if (params.x < sw / 2) 0 else sw - params.width
                safeUpdate()
            }
            start()
        }
        floatingView.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(memory.getBubbleColor())
        }
        floatingView.backgroundTintList = null
        resetHideTimer()
    }

    private fun showRadialMenu() {
        if (radialMenuShowing) return
        radialMenuShowing = true
        resetHideTimer()

        val sw = resources.displayMetrics.widthPixels
        val btnSize = dp(56); val orbit = dp(90)
        val cx = params.x + params.width / 2
        val cy = params.y + params.height / 2

        val actions = listOf(
            "🛡️" to Runnable {
                startActivity(Intent(this, SecurityActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                hideRadialMenu()
            },
            "📝" to Runnable {
                startActivity(Intent(this, NotesActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                hideRadialMenu()
            },
            "👁️" to Runnable {
                startActivity(Intent(this, ObserveActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                hideRadialMenu()
            },
            "🔐" to Runnable {
                startActivity(Intent(this, VaultActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                hideRadialMenu()
            },
            "🏠" to Runnable {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                hideRadialMenu()
            }
        )

        val isRightSide = params.x > sw / 2
        val startAngle = if (isRightSide) 110.0 else -70.0
        val spread = if (isRightSide) 140.0 else 140.0

        actions.forEachIndexed { i, (icon, action) ->
            val angleDeg = startAngle + (spread * i / (actions.size - 1))
            val rad = Math.toRadians(angleDeg)
            val tx = (cx + orbit * cos(rad) - btnSize / 2).toInt()
            val ty = (cy + orbit * sin(rad) - btnSize / 2).toInt()

            val btn = android.widget.TextView(this).apply {
                text = icon; textSize = 22f; gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(memory.getBubbleColor())
                    setStroke(dp(2), (0x88 shl 24) or (memory.getBubbleColor() and 0x00FFFFFF))
                }
                elevation = dp(8).toFloat()
                alpha = 0f; scaleX = 0.1f; scaleY = 0.1f
                setOnClickListener { action.run() }
            }

            val bp = WindowManager.LayoutParams(
                btnSize, btnSize,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = cx - btnSize / 2; y = cy - btnSize / 2
            }

            try {
                windowManager.addView(btn, bp)
                radialViews.add(btn to bp)
                handler.postDelayed({
                    ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 280; interpolator = OvershootInterpolator(1.2f)
                        addUpdateListener { a ->
                            val p = a.animatedValue as Float
                            bp.x = (cx - btnSize / 2 + (tx - (cx - btnSize / 2)) * p).toInt()
                            bp.y = (cy - btnSize / 2 + (ty - (cy - btnSize / 2)) * p).toInt()
                            try { windowManager.updateViewLayout(btn, bp) } catch (_: Exception) {}
                        }
                        start()
                    }
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(250).setInterpolator(OvershootInterpolator(1.2f)).start()
                }, i * 55L)
            } catch (_: Exception) {}
        }
    }

    private fun updateJoystickSelection(rawX: Float, rawY: Float) {
        var closestIndex = -1
        var minDistance = dp(80).toFloat() // Slightly smaller threshold for better control

        radialViews.forEachIndexed { i, (view, vp) ->
            val centerX = vp.x + vp.width / 2f
            val centerY = vp.y + vp.height / 2f
            val dist = Math.sqrt(Math.pow((rawX - centerX).toDouble(), 2.0) + 
                                Math.pow((rawY - centerY).toDouble(), 2.0)).toFloat()
            
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = i
            }
        }

        if (closestIndex != activeRadialIndex) {
            // Unscale previous
            if (activeRadialIndex != -1 && activeRadialIndex < radialViews.size) {
                radialViews[activeRadialIndex].first.animate()
                    .scaleX(1f).scaleY(1f).setDuration(150).start()
            }
            
            // Scale new
            activeRadialIndex = closestIndex
            if (activeRadialIndex != -1) {
                radialViews[activeRadialIndex].first.animate()
                    .scaleX(1.3f).scaleY(1.3f).setDuration(150).start()
                
                // Haptic feedback if possible
                try {
                    floatingView.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                } catch (_: Exception) {}
            }
        }
    }

    private fun hideRadialMenu() {
        if (!radialMenuShowing) return
        radialMenuShowing = false
        activeRadialIndex = -1
        val cx = params.x + params.width / 2
        val cy = params.y + params.height / 2
        val btnSize = dp(56)

        radialViews.forEachIndexed { i, (view, vp) ->
            handler.postDelayed({
                view.animate().alpha(0f).scaleX(0.1f).scaleY(0.1f).setDuration(180)
                    .withEndAction { try { windowManager.removeView(view) } catch (_: Exception) {} }
                    .start()
            }, i * 30L)
        }
        radialViews.clear()
    }

    private fun setupFloatingCircle() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (!memory.isBubbleEnabled()) {
            stopSelf()
            return
        }
        
        val size = dp(memory.getBubbleSize())
        floatingView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(memory.getBubbleColor())
            }
            // Inner icon for the Gojo bubble
            addView(TextView(this@FloatingCircleService).apply {
                text = "🌬️"; textSize = 18f; gravity = Gravity.CENTER
            })
        }
        floatingView.backgroundTintList = null
        floatingView.elevation = dp(8).toFloat()

        params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0; y = dp(200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        windowManager.addView(floatingView, params)

        floatingView.setOnTouchListener { _, event ->
            resetHideTimer()
            if (isHidden && event.action == MotionEvent.ACTION_DOWN) {
                showFully(); return@setOnTouchListener true
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    floatingView.animate().scaleX(0.88f).scaleY(0.88f).setDuration(100).start()
                    longPressRunnable = Runnable { showRadialMenu() }
                    handler.postDelayed(longPressRunnable!!, LONG_PRESS_MS)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    
                    if (radialMenuShowing) {
                        updateJoystickSelection(event.rawX, event.rawY)
                    } else if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        longPressRunnable?.let { handler.removeCallbacks(it) }
                    }
                    
                    if (!radialMenuShowing) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        safeUpdate()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    floatingView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    
                    if (radialMenuShowing) {
                        if (activeRadialIndex != -1) {
                            radialViews[activeRadialIndex].first.performClick()
                        } else {
                            hideRadialMenu()
                        }
                    } else {
                        val clicked = Math.abs(event.rawX - initialTouchX) < 12 &&
                                Math.abs(event.rawY - initialTouchY) < 12
                        if (clicked) onCircleTapped() else snapToEdge()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge() {
        val sw = resources.displayMetrics.widthPixels
        val targetX = if (params.x + params.width / 2 < sw / 2) 0 else sw - params.width
        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 220; interpolator = DecelerateInterpolator()
            addUpdateListener { params.x = it.animatedValue as Int; safeUpdate() }
            start()
        }
    }

    private fun onCircleTapped() {
        startActivity(Intent(this, ChatBubbleActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("EXTRA_FULL_SCREEN", false)
        })
    }

    private fun safeUpdate() {
        if (::floatingView.isInitialized && floatingView.isAttachedToWindow) {
            try { windowManager.updateViewLayout(floatingView, params) } catch (_: Exception) {}
        }
    }

    private fun startForegroundNow() {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Breezy is active")
            .setContentText("Watching over your phone")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            startForeground(1, notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            NotificationChannel(CHANNEL_ID, "Breezy", NotificationManager.IMPORTANCE_LOW)
                .also { getSystemService(NotificationManager::class.java).createNotificationChannel(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        hideRadialMenu()
        if (::moduleManager.isInitialized) moduleManager.destroy()
        if (::floatingView.isInitialized && floatingView.isAttachedToWindow)
            try { windowManager.removeView(floatingView) } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    companion object { private const val CHANNEL_ID = "BreezyChannel" }
}
