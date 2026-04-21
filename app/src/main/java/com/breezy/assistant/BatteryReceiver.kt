package com.breezy.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.telephony.SmsManager
import android.os.Build
import com.google.android.gms.location.LocationServices

class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_CHANGED) return

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING

        if (level == 5 && !isCharging) {
            triggerEFlare(context)
        }
    }

    private fun triggerEFlare(context: Context) {
        val memory = BreezyMemory(context)
        val contacts = memory.getFact("emergency_contacts").split(",").filter { it.isNotBlank() }
        if (contacts.isEmpty()) return

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val locStr = location?.let {
                    "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                } ?: "Location unavailable"
                val message = "Breezy E-Flare: Battery critical (5%). Last known location: $locStr"
                
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                contacts.forEach { number ->
                    try {
                        smsManager.sendTextMessage(number, null, message, null, null)
                    } catch (e: Exception) {}
                }
            }
        } catch (e: SecurityException) {}
    }
}
