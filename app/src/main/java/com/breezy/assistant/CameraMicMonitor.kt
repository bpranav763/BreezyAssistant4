package com.breezy.assistant

import android.app.AppOpsManager
import android.content.Context
import android.os.Build

class CameraMicMonitor(private val context: Context) {

  data class AccessLog(val appName: String, val type: String, val time: Long)
  private val logs = mutableListOf<AccessLog>()

  fun startMonitoring(onAlert: (String) -> Unit) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          val appOps = context.getSystemService(AppOpsManager::class.java)
        
          appOps.startWatchingActive(
              arrayOf(AppOpsManager.OPSTR_RECORD_AUDIO),
              context.mainExecutor
          ) { _, _, packageName, active ->
              if (active) {
                  val name = getAppName(packageName)
                  logs.add(AccessLog(name, "Microphone", System.currentTimeMillis()))
                  onAlert("$name just turned on your microphone in the background.")
              }
          }

          appOps.startWatchingActive(
              arrayOf(AppOpsManager.OPSTR_CAMERA),
              context.mainExecutor
          ) { _, _, packageName, active ->
              if (active) {
                  val name = getAppName(packageName)
                  logs.add(AccessLog(name, "Camera", System.currentTimeMillis()))
                  onAlert("$name just turned on your camera.")
              }
          }
      }
  }

  fun getRecentLogs(): List<AccessLog> = logs.takeLast(10)

  private fun getAppName(packageName: String): String {
      return try {
          val pm = context.packageManager
          pm.getApplicationLabel(
              pm.getApplicationInfo(packageName, 0)
          ).toString()
      } catch (e: Exception) { packageName }
  }
}
