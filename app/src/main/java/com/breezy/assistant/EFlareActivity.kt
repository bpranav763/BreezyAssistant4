package com.breezy.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class EFlareActivity : BaseActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val contacts = mutableListOf<String>()
    private val memory by lazy { BreezyMemory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
        }
        root.addView(buildHeader("🆘 E-Flare") { finish() })

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(32))
        }

        content.addView(TextView(this).apply {
            text = "When your battery drops to 5%, Breezy will send your last known location to your emergency contacts."
            textSize = 13f
            setTextColor(0xFF9CA3AF.toInt())
            setPadding(0, 0, 0, dp(20))
        })

        // Emergency contacts section
        content.addView(TextView(this).apply {
            text = "EMERGENCY CONTACTS"
            textSize = 11f
            setTextColor(0xFF4B5563.toInt())
            letterSpacing = 0.15f
        })

        val contactsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(contactsContainer)

        fun refreshContactsList() {
            contactsContainer.removeAllViews()
            val savedContacts = memory.getFact("emergency_contacts").split(",").filter { it.isNotBlank() }
            contacts.clear()
            contacts.addAll(savedContacts)
            contacts.forEach { number ->
                contactsContainer.addView(contactChip(number) { 
                    removeContact(number)
                    refreshContactsList()
                })
            }
        }
        refreshContactsList()

        // Add contact button
        val addBtn = TextView(this).apply {
            text = "+ Add Contact"
            textSize = 14f
            setTextColor(0xFF1D4ED8.toInt())
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener { showAddContactDialog { refreshContactsList() } }
        }
        content.addView(addBtn)

        // Test button
        content.addView(TextView(this).apply {
            text = "🧪 Send Test E-Flare"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setPadding(0, dp(16), 0, dp(16))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(32) }
            setOnClickListener { sendTestFlare() }
        })

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)
        requestPermissions()
    }

    private fun contactChip(number: String, onRemove: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(0xFF1A2235.toInt())
                cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) }
            addView(TextView(context).apply {
                text = number
                textSize = 14f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            addView(TextView(context).apply {
                text = "✕"
                setTextColor(0xFFEF4444.toInt())
                setPadding(dp(12), 0, 0, 0)
                setOnClickListener { onRemove() }
            })
        }
    }

    private fun removeContact(number: String) {
        contacts.remove(number)
        memory.saveFact("emergency_contacts", contacts.joinToString(","))
    }

    private fun showAddContactDialog(onAdded: () -> Unit) {
        val input = EditText(this).apply {
            hint = "Phone number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        AlertDialog.Builder(this)
            .setTitle("Add Emergency Contact")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val number = input.text.toString().trim()
                if (number.isNotEmpty()) {
                    contacts.add(number)
                    memory.saveFact("emergency_contacts", contacts.joinToString(","))
                    onAdded()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendTestFlare() {
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Add at least one emergency contact", Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions()
            return
        }
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val message = buildFlareMessage(location, isTest = true)
                sendSmsToAll(message)
                Toast.makeText(this, "Test E-Flare sent", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildFlareMessage(location: Location?, isTest: Boolean): String {
        val prefix = if (isTest) "[TEST] " else ""
        val locStr = location?.let {
            "https://maps.google.com/?q=${it.latitude},${it.longitude}"
        } ?: "Location unavailable"
        return "${prefix}Breezy E-Flare: Battery critical. Last known location: $locStr"
    }

    private fun sendSmsToAll(message: String) {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS), 100)
    }
}
