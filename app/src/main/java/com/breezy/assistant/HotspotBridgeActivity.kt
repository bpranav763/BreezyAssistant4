package com.breezy.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
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

class HotspotBridgeActivity : BaseActivity() {

    private lateinit var statusText: TextView
    private lateinit var detailText: TextView
    private lateinit var bridgeBtn: TextView
    private lateinit var infoCard: LinearLayout
    
    private var wifiManager: WifiManager? = null
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        root.addView(buildHeader("📡 Hotspot Bridge") { finish() })

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            setPadding(dp(32), dp(24), dp(32), 0)
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
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        })

        statusText = TextView(this).apply {
            text = "Share your current WiFi connection as a hotspot.\nUseful for hotels and airplanes."
            textSize = 13f
            setTextColor(0xFF6B7280.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(40))
        }
        main.addView(statusText)

        // Info Card (Hidden until active)
        infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0xFF1F2937.toInt())
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(40)) }
            visibility = View.GONE
            
            addView(TextView(context).apply {
                text = "HOTSPOT DETAILS"
                textSize = 11f
                setTextColor(0xFF9CA3AF.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(12))
            })
            
            detailText = TextView(context).apply {
                text = "Starting..."
                textSize = 15f
                setTextColor(Color.WHITE)
                setLineSpacing(0f, 1.3f)
            }
            addView(detailText)
        }
        main.addView(infoCard)

        bridgeBtn = TextView(this).apply {
            text = "🚀  Start Bridge"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(14).toFloat()
            }
            setPadding(0, dp(18), 0, dp(18))
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            setOnClickListener { toggleBridge() }
        }
        main.addView(bridgeBtn)

        root.addView(main)
        setContentView(root)
        applySystemBarInsets(root)
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

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        // API 33+ — use new SoftApConfiguration
                        val config = reservation.softApConfiguration
                        ssid = config.ssid ?: "Breezy Hotspot"
                        password = config.passphrase ?: "No password"
                    } else {
                        // API < 33 — use deprecated WifiConfiguration
                        @Suppress("DEPRECATION")
                        val config = reservation.wifiConfiguration
                        ssid = config?.SSID ?: "Breezy Hotspot"
                        password = config?.preSharedKey ?: "No password"
                    }

                    runOnUiThread {
                        bridgeBtn.isEnabled = true
                        bridgeBtn.alpha = 1.0f
                        bridgeBtn.text = "⏹️  Stop Bridge"
                        bridgeBtn.background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(0xFF1F2937.toInt())
                            cornerRadius = dp(14).toFloat()
                        }
                        infoCard.visibility = android.view.View.VISIBLE
                        detailText.text = "SSID: $ssid\nPassword: $password"
                        statusText.text = "Bridge active. Devices can connect to your hotspot."
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
        
        runOnUiThread {
            bridgeBtn.text = "🚀  Start Bridge"
            bridgeBtn.background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(14).toFloat()
            }
            infoCard.visibility = View.GONE
            statusText.text = "Share your current WiFi connection as a hotspot.\nUseful for hotels and airplanes."
            if (!isFinishing) Toast.makeText(this, "Hotspot Bridge Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHotspot()
    }
}
