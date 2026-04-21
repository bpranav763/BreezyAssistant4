package com.breezy.assistant

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings

class AntiStalkerScanner(private val context: Context) {

   data class ScanResult(
       val packageName: String,
       val appName: String,
       val riskScore: Int,
       val reasons: List<String>,
       val isSystem: Boolean
   )

   fun scanForThreats(): List<ScanResult> {
       val pm = context.packageManager
       val installedApps = pm.getInstalledApplications(0) // Fast, no meta-data
       val threats = mutableListOf<ScanResult>()

       for (app in installedApps) {
           val reasons = mutableListOf<String>()
           var score = 0

           // Check for suspicious permissions
           if (hasPermission(app.packageName, android.Manifest.permission.PROCESS_OUTGOING_CALLS)) {
               score += 30
               reasons.add("Can intercept outgoing calls")
           }
           if (hasPermission(app.packageName, android.Manifest.permission.RECORD_AUDIO)) {
               score += 20
               reasons.add("Can record audio")
           }
           if (hasPermission(app.packageName, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
               score += 20
               reasons.add("Can track location")
           }
           if (hasPermission(app.packageName, android.Manifest.permission.READ_SMS)) {
               score += 25
               reasons.add("Can read your messages")
           }
           if (hasPermission(app.packageName, android.Manifest.permission.SYSTEM_ALERT_WINDOW)) {
               score += 15
               reasons.add("Can draw over other apps (Overlay)")
           }

           // Check if it's an Accessibility Service
           if (isAccessibilityService(app.packageName)) {
               score += 40
               reasons.add("Active Accessibility Service (can read screen content)")
           }

           // Check for hidden apps (no launcher icon)
           if (pm.getLaunchIntentForPackage(app.packageName) == null && !isSystemApp(app)) {
               score += 35
               reasons.add("Hidden app (no launcher icon)")
           }

           val isSystem = isSystemApp(app)
           if (isSystem) score /= 2 // Reduce score for system apps as they are usually trusted

           if (score >= 40 || (score >= 20 && !isSystem)) {
               threats.add(
                   ScanResult(
                       packageName = app.packageName,
                       appName = pm.getApplicationLabel(app).toString(),
                       riskScore = score.coerceAtMost(100),
                       reasons = reasons,
                       isSystem = isSystem
                   )
               )
           }
       }

       return threats.sortedByDescending { it.riskScore }
   }

   private fun hasPermission(pkg: String, permission: String): Boolean {
       return try {
           context.packageManager.checkPermission(permission, pkg) == PackageManager.PERMISSION_GRANTED
       } catch (e: Exception) {
           false
       }
   }

   private fun isSystemApp(app: ApplicationInfo): Boolean {
       return (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
              (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
   }

   private fun isAccessibilityService(pkg: String): Boolean {
       val enabledServices = Settings.Secure.getString(
           context.contentResolver,
           Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
       ) ?: ""
       return enabledServices.contains(pkg)
   }
}
