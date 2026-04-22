package com.breezy.assistant

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class EFlareService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val memory by lazy { BreezyMemory(this) }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(100, NotificationCompat.Builder(this, "eflare")
            .setContentTitle("E-Flare Active")
            .setContentText("Emergency location service running")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sendEmergencySms()
        stopSelf()
        return START_NOT_STICKY
    }

    private fun sendEmergencySms() {
        val contacts = memory.getFact("emergency_contacts").split(",").filter { it.isNotBlank() }
        if (contacts.isEmpty()) return

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val message = buildFlareMessage(location)
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            contacts.forEach { number ->
                try {
                    smsManager.sendTextMessage(number, null, message, null, null)
                } catch (e: Exception) {
                    // Log error
                }
            }
            memory.saveFact("eflare_last_trigger", System.currentTimeMillis().toString())
        }
    }

    private fun buildFlareMessage(location: Location?): String {
        val locStr = location?.let {
            "https://maps.google.com/?q=${it.latitude},${it.longitude}"
        } ?: "Location unavailable"
        return "Breezy E-Flare: Battery critical. Last known location: $locStr"
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
