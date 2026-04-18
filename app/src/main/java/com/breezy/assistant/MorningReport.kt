package com.breezy.assistant

import android.content.Context
import java.util.Calendar

class MorningReport(private val context: Context) {

    private val battery = BatteryMonitor(context)
    private val wifi    = WifiSecurityMonitor(context)
    private val memory  = BreezyMemory(context)

    fun generateReport(): String {
        val name    = memory.getUserName().ifEmpty { "there" }
        val hour    = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val data    = battery.getBatteryData()
        val net     = wifi.getCurrentNetworkDetails()
        val storage = getStorageSummary()

        val greeting = when {
            hour < 6  -> "Still up late"
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            hour < 21 -> "Good evening"
            else      -> "Still going"
        }

        return buildString {
            appendLine("$greeting, $name! Here's your phone report:")
            appendLine()

            // Battery
            append("🔋 Battery at ${data.level}%")
            when {
                data.isCharging && data.level >= 80 ->
                    appendLine(" — fully charged, consider unplugging to protect battery life.")
                data.isCharging ->
                    appendLine(" and charging ⚡. Temperature: ${data.temperature}°C.")
                data.level <= 15 ->
                    appendLine(" — critically low. Plug in soon.")
                data.level <= 30 ->
                    appendLine(" — getting low. Worth finding a charger.")
                data.temperature > 42f ->
                    appendLine(" — running warm at ${data.temperature}°C. Close heavy apps.")
                else ->
                    appendLine(" — healthy. ${data.temperature}°C, ${data.voltage}mV.")
            }

            // WiFi
            if (net.isConnected) {
                when (wifi.analyzeCurrentNetwork()) {
                    WifiSecurityMonitor.SecurityResult.OPEN_NETWORK ->
                        appendLine("📶 ⚠️ On open WiFi (${net.ssid}) — no encryption. Be careful.")
                    WifiSecurityMonitor.SecurityResult.SECURE ->
                        appendLine("📶 ${net.ssid} — ${net.signalLevel} signal, ${net.band}.")
                    else ->
                        appendLine("📶 Connected but WiFi status unclear.")
                }
            } else {
                appendLine("📶 Not connected to WiFi.")
            }

            // Storage
            appendLine("📦 $storage")

            // Security alerts
            val lastAlert = memory.getFact("last_security_alert")
            if (lastAlert.isNotEmpty()) {
                appendLine("🛡️ Alert since last check: $lastAlert")
            } else {
                appendLine("🛡️ No threats detected. Your privacy is intact.")
            }

            appendLine()
            append("Say 'help' to see what I can do, or just ask me anything.")
        }.trim()
    }

    private fun getStorageSummary(): String {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val freeGb  = stat.availableBytes / (1024f * 1024f * 1024f)
            val totalGb = stat.totalBytes      / (1024f * 1024f * 1024f)
            val usedPct = ((totalGb - freeGb) / totalGb * 100).toInt()
            when {
                usedPct >= 90 ->
                    "Storage critically full at $usedPct%! Only ${String.format("%.1f", freeGb)}GB left."
                usedPct >= 75 ->
                    "Storage at $usedPct% — ${String.format("%.1f", freeGb)}GB free. Keep an eye on it."
                else ->
                    "${String.format("%.1f", freeGb)}GB free of ${String.format("%.1f", totalGb)}GB."
            }
        } catch (_: Exception) { "Storage looks healthy." }
    }
}
