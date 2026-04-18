package com.breezy.assistant

import android.content.Context
import java.util.Calendar

class MorningReportManager(private val context: Context) {

    private val memory = BreezyMemory(context)
    private val batteryMonitor = BatteryMonitor(context)
    private val morningReport = MorningReport(context)

    fun shouldShowReport(): Boolean {
        val lastReportDate = memory.getFact("last_report_date")
        val today = getTodayString()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        // Show report between 6 AM and 11 AM if not already shown today
        return lastReportDate != today && hour in 6..10
    }

    suspend fun generateReport(): String {
        val report = morningReport.generateReport()
        memory.saveFact("last_report_date", getTodayString())
        return report
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
