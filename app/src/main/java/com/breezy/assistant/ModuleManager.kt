package com.breezy.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*

class ModuleManager(private val context: Context) {

    // Every module implements this
    interface BreezyModule {
        val id: String
        val wakeIntent: String?           // broadcast action that wakes this module (null = timer/manual only)
        val wakeIntervalMs: Long          // 0 = event-driven only, >0 = also wakes on timer
        fun onWake(intent: Intent?): String?  // returns alert text or null
        fun onSleep()
    }

    private data class ModuleSlot(
        val module: BreezyModule,
        var sleeping: Boolean = true,
        var receiver: BroadcastReceiver? = null
    )

    private val slots = mutableMapOf<String, ModuleSlot>()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var onAlert: ((String) -> Unit)? = null

    fun setAlertListener(listener: (String) -> Unit) {
        onAlert = listener
    }

    fun register(module: BreezyModule) {
        val slot = ModuleSlot(module)
        slots[module.id] = slot

        // If module has a wake intent, register a broadcast receiver
        module.wakeIntent?.let { action ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    wakeModule(module.id, intent)
                }
            }
            try {
                context.registerReceiver(receiver, IntentFilter(action))
                slot.receiver = receiver
            } catch (e: Exception) { /* some intents need special flags on API 34+ */ }
        }

        // If module also has a polling interval, schedule it
        if (module.wakeIntervalMs > 0) {
            scheduleTimer(module.id, module.wakeIntervalMs)
        }
    }

    private fun scheduleTimer(moduleId: String, intervalMs: Long) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                slots[moduleId]?.let { wakeModule(moduleId, null) }
                handler.postDelayed(this, intervalMs)
            }
        }, intervalMs)
    }

    fun wakeModule(id: String, intent: Intent?) {
        val slot = slots[id] ?: return
        slot.sleeping = false
        scope.launch {
            try {
                val alert = slot.module.onWake(intent)
                if (alert != null) {
                    withContext(Dispatchers.Main) { onAlert?.invoke(alert) }
                }
            } finally {
                slot.sleeping = true
                slot.module.onSleep()
            }
        }
    }

    fun sleepAll() {
        slots.values.forEach { it.sleeping = true; it.module.onSleep() }
    }

    fun destroy() {
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        slots.values.forEach { slot ->
            slot.receiver?.let { try { context.unregisterReceiver(it) } catch (_: Exception) {} }
            slot.module.onSleep()
        }
        slots.clear()
    }

    // --- Built-in modules ---

    inner class BatteryModule : BreezyModule {
        override val id = "battery"
        override val wakeIntent = Intent.ACTION_BATTERY_CHANGED
        override val wakeIntervalMs = 0L
        override fun onWake(intent: Intent?): String? {
            val monitor = BatteryMonitor(context)
            val data = monitor.getBatteryData()
            return when {
                data.level <= 10 && !data.isCharging ->
                    "Battery critical at ${data.level}%. Find a charger now."
                data.temperature > 47f ->
                    "Battery dangerously hot: ${data.temperature}°C. Stop charging."
                data.isCharging && data.voltage !in 3500..5500 ->
                    "Unsafe charger voltage (${data.voltage}mV). Unplug immediately."
                else -> null
            }
        }
        override fun onSleep() {}
    }

    inner class ThermalModule : BreezyModule {
        override val id = "thermal"
        override val wakeIntent = null
        override val wakeIntervalMs = 15_000L   // poll every 15s
        override fun onWake(intent: Intent?): String? {
            val temp = BatteryMonitor(context).getBatteryData().temperature
            return if (temp > 43f) "Phone is at ${temp}°C. That\u0027s getting hot — close heavy apps." else null
        }
        override fun onSleep() {}
    }

    inner class StorageModule : BreezyModule {
        override val id = "storage"
        override val wakeIntent = null
        override val wakeIntervalMs = 60 * 60 * 1000L  // hourly
        override fun onWake(intent: Intent?): String? {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val freeGb = stat.availableBytes / (1024f * 1024f * 1024f)
            val totalGb = stat.totalBytes / (1024f * 1024f * 1024f)
            val usedPct = ((totalGb - freeGb) / totalGb * 100).toInt()
            return if (usedPct >= 90)
                "Storage at $usedPct% — only ${String.format("%.1f", freeGb)}GB left. Let me help you clean up?"
            else null
        }
        override fun onSleep() {}
    }

    inner class NetworkModule : BreezyModule {
        override val id = "network"
        override val wakeIntent = android.net.ConnectivityManager.CONNECTIVITY_ACTION
        override val wakeIntervalMs = 0L
        override fun onWake(intent: Intent?): String? {
            val result = WifiSecurityMonitor(context).analyzeCurrentNetwork()
            return if (result == WifiSecurityMonitor.SecurityResult.OPEN_NETWORK)
                "You just connected to an open WiFi. No encryption. Use mobile data for anything sensitive."
            else null
        }
        override fun onSleep() {}
    }

    // Register all built-in modules
    fun registerBuiltins() {
        register(BatteryModule())
        register(ThermalModule())
        register(StorageModule())
        register(NetworkModule())
    }
}
