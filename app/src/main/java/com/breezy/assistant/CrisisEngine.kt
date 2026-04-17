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
        // 1. Thermal Check - Lowered to 40°C as requested for proactive protection
        val batteryData = batteryMonitor.getBatteryData()
        if (batteryData.temperature > 40f) {
            listener.onCrisisDetected(CrisisType.THERMAL, "Phone is getting warm (${batteryData.temperature}°C). I'll stay red until it cools down.")
        }

        // 2. Voltage Check (Charger Safety)
        if (batteryData.isCharging && (batteryData.voltage < 3500 || batteryData.voltage > 5500)) {
            listener.onCrisisDetected(CrisisType.VOLTAGE, "Unsafe charger voltage detected (${batteryData.voltage}mV). Unplug immediately!")
        }

        // 3. WiFi Security
        if (wifiMonitor.analyzeCurrentNetwork() == WifiSecurityMonitor.SecurityResult.OPEN_NETWORK) {
            val lastWifiAlert = memory.getFact("last_wifi_alert_time")
            val now = System.currentTimeMillis()
            // Alert once every 4 hours for the same open network to avoid spam
            if (now - lastWifiAlert.toLongOrDefault(0L) > 4 * 60 * 60 * 1000) {
                listener.onCrisisDetected(CrisisType.UNSECURE_WIFI, "You're on an open WiFi. Your data is exposed. Use a VPN or Mobile Data.")
                memory.saveFact("last_wifi_alert_time", now.toString())
            }
        }
    }

    private fun String.toLongOrDefault(default: Long): Long = this.toLongOrNull() ?: default
}
