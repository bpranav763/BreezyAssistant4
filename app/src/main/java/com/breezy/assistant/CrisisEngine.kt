package com.breezy.assistant

import android.content.Context
import android.util.Log

class CrisisEngine(private val context: Context) {

    private val batteryMonitor = BatteryMonitor(context)
    private val wifiMonitor = WifiSecurityMonitor(context)
    private val memory = BreezyMemory(context)

    interface CrisisListener {
        fun onCrisisDetected(type: CrisisType, message: String)
    }

    enum class CrisisType {
        THERMAL, VOLTAGE, UNSECURE_WIFI, STALKERWARE
    }

    fun checkAllSystems(listener: CrisisListener) {
        val batteryData = batteryMonitor.getBatteryData()
        
        // 1. Critical Thermal Check: > 48°C (Not just warm, but dangerous)
        if (batteryData.temperature > 48f) {
            listener.onCrisisDetected(CrisisType.THERMAL, "Phone is dangerously hot (${batteryData.temperature}°C). I'll stay red until it cools down.")
            return
        }

        // 2. Critical Voltage Check (Charger Safety)
        if (batteryData.isCharging && (batteryData.voltage < 3500 || batteryData.voltage > 5500)) {
            listener.onCrisisDetected(CrisisType.VOLTAGE, "Unsafe charger voltage detected (${batteryData.voltage}mV). Unplug immediately!")
            return
        }

        // 3. Warning WiFi Security (No visual red alert, just log)
        if (wifiMonitor.analyzeCurrentNetwork() == WifiSecurityMonitor.SecurityResult.OPEN_NETWORK) {
            val lastWifiAlert = memory.getFact("last_wifi_alert_time").toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            // Log once every 4 hours
            if (now - lastWifiAlert > 4 * 60 * 60 * 1000) {
                listener.onCrisisDetected(CrisisType.UNSECURE_WIFI, "Connected to an open WiFi network.")
                memory.saveFact("last_wifi_alert_time", now.toString())
            }
        }
    }

    private fun String.toLongOrDefault(default: Long): Long = this.toLongOrNull() ?: default
}
