package com.breezy.assistant

import android.content.Context

class CrisisDetector(private val context: Context) {

    private val batteryMonitor = BatteryMonitor(context)
    private val wifiMonitor = WifiSecurityMonitor(context)

    enum class CrisisLevel { NONE, WARNING, CRITICAL }

    data class CrisisState(
        val level: CrisisLevel,
        val message: String,
        val type: String
    )

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isMonitoring = false
    private var lastState = CrisisLevel.NONE

    fun startMonitoring(onCrisis: (String) -> Unit) {
        if (isMonitoring) return
        isMonitoring = true
        
        val runnable = object : Runnable {
            override fun run() {
                if (!isMonitoring) return
                val state = checkCurrentStatus()
                if (state.level != lastState && state.level != CrisisLevel.NONE) {
                    onCrisis(state.message)
                }
                lastState = state.level
                handler.postDelayed(this, 10000) // Check every 10 seconds
            }
        }
        handler.post(runnable)
    }

    fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacksAndMessages(null)
    }

    fun checkCurrentStatus(): CrisisState {
        val battery = batteryMonitor.getBatteryData()
        val wifi = wifiMonitor.analyzeCurrentNetwork()

        return when {
            // Temperature Crisis
            battery.temperature > 48f -> CrisisState(
                CrisisLevel.CRITICAL,
                "Your phone is dangerously hot (${battery.temperature}°C). I'm worried it might throttle or damage the battery.",
                "TEMP"
            )

            // Charging Safety Crisis
            battery.isCharging && (battery.voltage > 5500 || battery.voltage < 3000) -> CrisisState(
                CrisisLevel.CRITICAL,
                "The power coming from this charger is unstable. Unplug it now to stay safe.",
                "CHARGER"
            )

            // Critical Battery
            !battery.isCharging && battery.level <= 5 -> CrisisState(
                CrisisLevel.CRITICAL,
                "Battery is at ${battery.level}%. I'm going to sleep soon if we don't find a charger.",
                "BATTERY"
            )

            // Security Crisis
            wifi == WifiSecurityMonitor.SecurityResult.OPEN_NETWORK -> CrisisState(
                CrisisLevel.WARNING,
                "This WiFi has no password. Anyone can see what you're doing. Switch to mobile data?",
                "WIFI"
            )

            battery.temperature > 42f -> CrisisState(
                CrisisLevel.WARNING,
                "Phone's getting warm. Maybe we should take a break from heavy apps?",
                "TEMP"
            )

            else -> CrisisState(CrisisLevel.NONE, "", "")
        }
    }
}
