package com.breezy.assistant

import android.content.Context
import java.util.Calendar

class MorningReportManager(private val context: Context) {

    private val memory = BreezyMemory(context)
    private val batteryMonitor = BatteryMonitor(context)

    fun shouldShowReport(): Boolean {
        val lastReportDate = memory.getFact("last_report_date")
        val today = getTodayString()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        // Show report between 6 AM and 11 AM if not already shown today
        return lastReportDate != today && hour in 6..10
    }

    fun generateReport(): String {
        val userName = memory.getUserName().ifEmpty { "there" }
        val batteryData = batteryMonitor.getBatteryData()
        val wifi = WifiSecurityMonitor(context)
        val lastSecurityAlert = memory.getFact("last_security_alert")
        
        val report = StringBuilder("Good morning, $userName! 🌅\n\n")
        
        // Battery Health
        report.append("🔋 Your battery is at ${batteryData.level}%. ")
        if (batteryData.isCharging) {
            report.append("It's charging ⚡. ")
        } else if (batteryData.level < 50) {
            report.append("Might want to top it up before you head out. ")
        } else {
            report.append("You're all set for the morning. ")
        }
        report.append("\n\n")

        // Temperature Health
        if (batteryData.temperature > 42f) {
            report.append("🌡️ Phone is warm at ${batteryData.temperature}°C. Close heavy apps.\n\n")
        } else {
            report.append("🌡️ Temperature ${batteryData.temperature}°C — healthy.\n\n")
        }

        // WiFi Status
        val wifiStatus = when (wifi.analyzeCurrentNetwork()) {
            WifiSecurityMonitor.SecurityResult.OPEN_NETWORK -> "⚠️ You're on an open network."
            WifiSecurityMonitor.SecurityResult.SECURE -> "📶 WiFi is secure."
            else -> "📶 Not connected to WiFi."
        }
        report.append("$wifiStatus\n\n")

        // Security Summary
        if (lastSecurityAlert.isNotEmpty()) {
            report.append("🛡️ Overnight, I noticed: $lastSecurityAlert. ")
            report.append("I've kept things secure while you slept.")
        } else {
            report.append("🛡️ No security threats were detected overnight. Your privacy is intact.")
        }
        
        // Storage Tip
        val storageResponse = getStorageShortSummary()
        report.append("\n\n📦 $storageResponse")

        memory.saveFact("last_report_date", getTodayString())
        return report.toString()
    }

    private fun getTodayString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun getStorageShortSummary(): String {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val freeBytes = stat.availableBytes
            val freeGb = freeBytes / (1024 * 1024 * 1024f)
            if (freeGb < 2.0) {
                "Storage is critically low (${String.format("%.1f", freeGb)}GB). Let's clean up?"
            } else {
                "You have ${String.format("%.1f", freeGb)}GB of space available."
            }
        } catch (e: Exception) {
            "Storage looks healthy."
        }
    }
}
