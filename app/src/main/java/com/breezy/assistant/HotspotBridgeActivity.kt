package com.breezy.assistant

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class HotspotBridgeActivity : BaseActivity() {

    private lateinit var statusText: TextView
    private lateinit var detailText: TextView
    private lateinit var bridgeBtn: TextView
    private lateinit var infoCard: LinearLayout
    private lateinit var clientsContainer: LinearLayout
    
    private var wifiManager: WifiManager? = null
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    
    private val clientQuotas = mutableMapOf<String, Long>() // MAC -> bytes limit
    private val clientUsage = mutableMapOf<String, Long>()  // MAC -> bytes used
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@HotspotBridgeActivity))
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        root.addView(buildHeader("📡 Hotspot Bridge") { finish() })

        val scroll = ScrollView(this).apply { 
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), dp(24), dp(32), dp(40))
        }

        main.addView(TextView(this).apply {
            text = "📡"
            textSize = 64f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        })

        main.addView(TextView(this).apply {
            text = "WiFi Bridge"
            textSize = 24f
            setTextColor(ThemeManager.getTextPrimary(this@HotspotBridgeActivity))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        })

        statusText = TextView(this).apply {
            text = "Share your current WiFi connection as a hotspot.\nUseful for hotels and airplanes."
            textSize = 13f
            setTextColor(ThemeManager.getTextSecondary(this@HotspotBridgeActivity))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(40))
        }
        main.addView(statusText)

        // Info Card (Hidden until active)
        infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@HotspotBridgeActivity))
                setCornerRadius(dp(16).toFloat())
                setStroke(dp(1), ThemeManager.getTextSecondary(this@HotspotBridgeActivity) and 0x33FFFFFF)
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(24)) }
            visibility = View.GONE
            
            addView(TextView(context).apply {
                text = "HOTSPOT DETAILS"
                textSize = 11f
                setTextColor(ThemeManager.getAccentColor(this@HotspotBridgeActivity))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(12))
            })
            
            detailText = TextView(context).apply {
                text = "Starting..."
                textSize = 15f
                setTextColor(ThemeManager.getTextPrimary(this@HotspotBridgeActivity))
                setLineSpacing(0f, 1.3f)
            }
            addView(detailText)
        }
        main.addView(infoCard)

        // Clients Section
        clientsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        main.addView(clientsContainer)

        bridgeBtn = TextView(this).apply {
            text = "🚀  Start Bridge"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(ThemeManager.getAccentColor(this@HotspotBridgeActivity))
                setCornerRadius(dp(14).toFloat())
            }
            setPadding(0, dp(18), 0, dp(18))
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            setOnClickListener { toggleBridge() }
        }
        main.addView(bridgeBtn)

        scroll.addView(main)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (hotspotReservation != null) {
                updateConnectedClients()
                handler.postDelayed(this, 5000)
            }
        }
    }

    private fun updateConnectedClients() {
        val clients = loadConnectedClients()
        runOnUiThread {
            clientsContainer.removeAllViews()
            if (clients.isNotEmpty()) {
                clientsContainer.visibility = View.VISIBLE
                clientsContainer.addView(TextView(this).apply {
                    text = "CONNECTED DEVICES (${clients.size})"
                    textSize = 11f
                    setTextColor(ThemeManager.getAccentColor(this@HotspotBridgeActivity))
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dp(16), 0, dp(12))
                })
                clients.forEach { mac ->
                    clientsContainer.addView(buildClientRow(mac))
                }
            } else {
                clientsContainer.visibility = View.GONE
            }
        }
    }

    private fun buildClientRow(mac: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(12), dp(8), dp(12))
            
            addView(TextView(context).apply {
                text = "📱 $mac"
                textSize = 14f
                setTextColor(ThemeManager.getTextPrimary(this@HotspotBridgeActivity))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })

            val quota = clientQuotas[mac]
            val quotaStr = if (quota != null) "${quota / (1024 * 1024)}MB" else "No Limit"
            
            addView(TextView(context).apply {
                text = quotaStr
                textSize = 12f
                setTextColor(ThemeManager.getAccentColor(this@HotspotBridgeActivity))
                setPadding(dp(12), dp(4), dp(12), dp(4))
                background = GradientDrawable().apply {
                    setStroke(dp(1), ThemeManager.getAccentColor(this@HotspotBridgeActivity))
                    cornerRadius = dp(8).toFloat()
                }
                setOnClickListener { showClientQuotaDialog(mac) }
            })
        }
    }

    private fun showClientQuotaDialog(mac: String) {
        val input = EditText(this).apply { 
            hint = "Data limit in MB"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("Set Data Quota")
            .setMessage("Breezy will notify you when this device exceeds the limit.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val mb = input.text.toString().toLongOrNull() ?: 0
                if (mb > 0) {
                    clientQuotas[mac] = mb * 1024 * 1024
                } else {
                    clientQuotas.remove(mac)
                }
                updateConnectedClients()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadConnectedClients(): List<String> {
        val clients = mutableListOf<String>()
        try {
            val br = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                if (line?.contains("0x2") == true) { // Reachable
                    val parts = line?.trim()?.split(Regex("\\s+"))
                    if (parts != null && parts.size >= 4) {
                        val mac = parts[3]
                        if (mac != "00:00:00:00:00:00") clients.add(mac)
                    }
                }
            }
            br.close()
        } catch (e: Exception) { }
        return clients
    }

    private fun toggleBridge() {
        if (hotspotReservation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
                return
            }
            startHotspot()
        } else {
            stopHotspot()
        }
    }

    private fun startHotspot() {
        bridgeBtn.isEnabled = false
        bridgeBtn.text = "⌛  Starting..."
        bridgeBtn.alpha = 0.7f

        try {
            wifiManager?.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    super.onStarted(reservation)
                    hotspotReservation = reservation

                    val ssid: String
                    val password: String

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val config = reservation.softApConfiguration
                        ssid = config.ssid ?: "Breezy Hotspot"
                        password = config.passphrase ?: "No password"
                    } else {
                        @Suppress("DEPRECATION")
                        val config = reservation.wifiConfiguration
                        ssid = config?.SSID ?: "Breezy Hotspot"
                        password = config?.preSharedKey ?: "No password"
                    }

                    runOnUiThread {
                        bridgeBtn.isEnabled = true
                        bridgeBtn.alpha = 1.0f
                        bridgeBtn.text = "⏹️  Stop Bridge"
                        bridgeBtn.background = GradientDrawable().apply {
                            setColor(ThemeManager.getCardColor(this@HotspotBridgeActivity))
                            setCornerRadius(dp(14).toFloat())
                            setStroke(dp(1), ThemeManager.getAccentColor(this@HotspotBridgeActivity))
                        }
                        infoCard.visibility = View.VISIBLE
                        detailText.text = "SSID: $ssid\nPassword: $password"
                        statusText.text = "Bridge active. Devices can connect to your hotspot."
                        handler.post(monitorRunnable)
                    }
                }

                override fun onStopped() {
                    super.onStopped()
                    stopHotspot()
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    runOnUiThread {
                        bridgeBtn.isEnabled = true
                        bridgeBtn.alpha = 1.0f
                        bridgeBtn.text = "🚀  Start Bridge"
                        val errorMsg = when(reason) {
                            ERROR_NO_CHANNEL -> "No channel available"
                            ERROR_TETHERING_DISALLOWED -> "Tethering disallowed by system"
                            else -> "Unknown error (code $reason)"
                        }
                        Toast.makeText(this@HotspotBridgeActivity, "Failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: SecurityException) {
            bridgeBtn.isEnabled = true
            bridgeBtn.alpha = 1.0f
            Toast.makeText(this, "Permission denied: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            bridgeBtn.isEnabled = true
            bridgeBtn.alpha = 1.0f
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopHotspot() {
        hotspotReservation?.close()
        hotspotReservation = null
        handler.removeCallbacks(monitorRunnable)
        
        runOnUiThread {
            bridgeBtn.text = "🚀  Start Bridge"
            bridgeBtn.background = GradientDrawable().apply {
                setColor(ThemeManager.getAccentColor(this@HotspotBridgeActivity))
                setCornerRadius(dp(14).toFloat())
            }
            infoCard.visibility = View.GONE
            clientsContainer.visibility = View.GONE
            statusText.text = "Share your current WiFi connection as a hotspot.\nUseful for hotels and airplanes."
            if (!isFinishing) Toast.makeText(this, "Hotspot Bridge Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHotspot()
    }
}
