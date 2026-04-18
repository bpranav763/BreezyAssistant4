package com.breezy.assistant

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build

class WifiSecurityMonitor(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    enum class SecurityResult { NONE, SECURE, OPEN_NETWORK }

    fun analyzeCurrentNetwork(): SecurityResult {
        try {
            val info = wifiManager.connectionInfo ?: return SecurityResult.NONE
            if (info.networkId == -1) return SecurityResult.NONE

            @Suppress("DEPRECATION")
            val scanResults = wifiManager.scanResults ?: return SecurityResult.NONE
            val currentScan = scanResults.find { it.BSSID == info.bssid } ?: return SecurityResult.NONE

            val capabilities = currentScan.capabilities
            val isOpen = !capabilities.contains("WEP") && !capabilities.contains("WPA") && !capabilities.contains("WPA2")

            return if (isOpen) SecurityResult.OPEN_NETWORK else SecurityResult.SECURE
        } catch (e: Exception) {
            return SecurityResult.NONE
        }
    }

    fun getWifiResponse(): String {
        val result = analyzeCurrentNetwork()
        val threats = scanForThreats()
        
        val base = when (result) {
            SecurityResult.OPEN_NETWORK -> "⚠️ This WiFi has no password. It's an open network, meaning anyone can see your traffic. I'd recommend switching to mobile data for sensitive things."
            SecurityResult.SECURE -> "✅ Your current WiFi is encrypted and looks safe. No immediate threats detected."
            SecurityResult.NONE -> "I can't see a WiFi connection right now. Are you on mobile data?"
        }
        
        if (threats.isNotEmpty()) {
            return base + "\n\nAlso, I detected ${threats.size} suspicious networks nearby. Be careful about where you connect."
        }
        
        return base
    }

    fun scanForThreats(): List<WifiThreat> {
        val threats = mutableListOf<WifiThreat>()
        
        try {
            @Suppress("DEPRECATION")
            val scanResults = wifiManager.scanResults ?: return emptyList()

            for (result in scanResults) {
                val threat = analyzeNetwork(result)
                if (threat != null) {
                    threats.add(threat)
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }

        return threats
    }

    private fun analyzeNetwork(result: ScanResult): WifiThreat? {
        val capabilities = result.capabilities
        val ssid = result.SSID
        val bssid = result.BSSID
        val level = result.level

        val reasons = mutableListOf<String>()
        var riskScore = 0

        // 1. Check for open networks
        val isOpen = !capabilities.contains("WEP") && !capabilities.contains("WPA") && !capabilities.contains("WPA2")
        if (isOpen) {
            riskScore += 40
            reasons.add("Open network (No encryption)")
        }

        // 2. Check for suspicious SSIDs
        val suspiciousKeywords = listOf("FREE", "GUEST", "PUBLIC", "HOTSPOT", "WIFI")
        if (suspiciousKeywords.any { ssid.uppercase().contains(it) }) {
            riskScore += 20
            reasons.add("Suspicious SSID name")
        }

        // 3. Known "Evil Twin" patterns (simplified for this example)
        if (ssid.isEmpty() || ssid == "<unknown ssid>") {
            riskScore += 30
            reasons.add("Hidden or empty SSID")
        }

        // 4. Signal strength (Too strong might be a nearby rogue AP)
        if (level > -30) {
            riskScore += 10
            reasons.add("Exceptionally strong signal (possible nearby rogue AP)")
        }

        return if (riskScore >= 40) {
            WifiThreat(ssid, bssid, riskScore, reasons)
        } else {
            null
        }
    }

    data class NetworkDetails(
        val isConnected: Boolean,
        val ssid: String,
        val signalLevel: String,
        val band: String
    )

    fun getCurrentNetworkDetails(): NetworkDetails {
        val info = wifiManager.connectionInfo
        if (info == null || info.networkId == -1) {
            return NetworkDetails(false, "", "", "")
        }
        val ssid = info.ssid.removeSurrounding("\"")
        val signal = when {
            info.rssi > -50 -> "Excellent"
            info.rssi > -60 -> "Good"
            info.rssi > -70 -> "Fair"
            else -> "Weak"
        }
        val band = if (info.frequency > 4000) "5GHz" else "2.4GHz"
        return NetworkDetails(true, ssid, signal, band)
    }

    data class WifiThreat(
        val ssid: String,
        val bssid: String,
        val riskScore: Int,
        val reasons: List<String>
    )
}
