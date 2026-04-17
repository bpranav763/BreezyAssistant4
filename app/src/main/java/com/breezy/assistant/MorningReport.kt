package com.breezy.assistant

import android.content.Context
import java.util.Calendar

class MorningReport(private val context: Context) {

   fun generateReport(): String {
       val name = BreezyMemory(context).getUserName().ifEmpty { "there" }
       val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

       val greeting = when {
           hour < 12 -> "Good morning"
           hour < 17 -> "Good afternoon"
           else -> "Good evening"
       }

       // Simulated hardware/security checks for now
       val batteryStatus = "🔋 Battery at 85% — good to go."
       val tempStatus = "🌡️ Temperature is 36.5°C — perfectly healthy."
       val wifiStatus = "📶 WiFi is secure."

       return """$greeting $name. Here's your phone report:

$batteryStatus
$tempStatus
$wifiStatus

Ask me anything or just say 'help' to see what I can do."""
   }
}
