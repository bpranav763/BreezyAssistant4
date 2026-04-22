package com.breezy.assistant

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class SecurityActivity : BaseActivity() {

    private val battery by lazy { BatteryMonitor(this) }
    private val wifi    by lazy { WifiSecurityMonitor(this) }
    private val memory  by lazy { BreezyMemory(this) }
    private val controls by lazy { SystemControls(this) }

    private lateinit var dndBtn: LinearLayout
    private lateinit var wifiBtn: LinearLayout
    private lateinit var btBtn: LinearLayout
    private lateinit var muteBtn: LinearLayout
    private lateinit var brightnessLowBtn: LinearLayout
    private lateinit var brightnessHighBtn: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@SecurityActivity))
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        root.addView(buildHeader("🛡️ Security") { finish() })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(32))
        }

        // ── THREAT STATUS ─────────────────────────────────────────────────
        container.addView(sectionLabel("THREAT STATUS"))
        container.addView(buildThreatCard())

        // ── QUICK TOGGLES ─────────────────────────────────────────────────
        container.addView(sectionLabel("QUICK CONTROLS"))

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        dndBtn    = buildToggleBtn("🔕", "DND",     isDndOn())      { toggleDnd() }
        wifiBtn   = buildToggleBtn("📶", "WiFi",    isWifiOn())     { toggleWifi() }
        btBtn     = buildToggleBtn("🔵", "Bluetooth", isBtOn())     { toggleBluetooth() }
        row1.addView(dndBtn);  row1.addView(spacer()); row1.addView(wifiBtn)
        row1.addView(spacer()); row1.addView(btBtn)
        container.addView(row1)
        container.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(-1, dp(10)) })

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        muteBtn          = buildToggleBtn("🔇", "Mute",    isMuted())      { toggleMute() }
        brightnessLowBtn = buildToggleBtn("🌑", "Dim",     false)           { setBright(30) }
        brightnessHighBtn= buildToggleBtn("☀️", "Bright",  false)           { setBright(100) }
        row2.addView(muteBtn);  row2.addView(spacer()); row2.addView(brightnessLowBtn)
        row2.addView(spacer()); row2.addView(brightnessHighBtn)
        container.addView(row2)

        // ── SCANNER TILES ─────────────────────────────────────────────────
        container.addView(sectionLabel("ACTIVE PROTECTION"))

        container.addView(scannerTile("🕵️", "Anti-Stalkerware Scan",
            "Check all apps for hidden monitoring") {
            startActivity(Intent(this, AntiStalkerActivity::class.java))
        })
        container.addView(scannerTile("📞", "Caller ID & Spam Shield",
            "Offline number check — no data sent") {
            startActivity(Intent(this, CallerIdActivity::class.java))
        })
        container.addView(scannerTile("📶", "WiFi Threat Scan",
            "Detect evil twins and open networks") {
            runWifiScan(container)
        })
        container.addView(scannerTile("📦", "Storage Cleaner",
            "Find large files eating your space") {
            startActivity(Intent(this, StorageAnalysisActivity::class.java))
        })

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun isDndOn(): Boolean {
        val nm = getSystemService(NotificationManager::class.java)
        return nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    private fun isWifiOn(): Boolean {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wm.isWifiEnabled
    }

    private fun isBtOn(): Boolean {
        val bt = BluetoothAdapter.getDefaultAdapter()
        return bt?.isEnabled == true
    }

    private fun isMuted(): Boolean {
        val am = getSystemService(AudioManager::class.java)
        return am.getStreamVolume(AudioManager.STREAM_RING) == 0
    }

    private fun toggleDnd() {
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            Toast.makeText(this, "Grant DND permission to Breezy", Toast.LENGTH_SHORT).show()
            return
        }
        val on = isDndOn()
        nm.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_ALL
            else    NotificationManager.INTERRUPTION_FILTER_NONE
        )
        updateToggleState(dndBtn, !on)
        Toast.makeText(this, if (!on) "DND On" else "DND Off", Toast.LENGTH_SHORT).show()
    }

    private fun toggleWifi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val panel = Intent(android.provider.Settings.Panel.ACTION_WIFI)
            startActivity(panel)
        } else {
            @Suppress("DEPRECATION")
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val current = wm.isWifiEnabled
            @Suppress("DEPRECATION")
            wm.isWifiEnabled = !current
            updateToggleState(wifiBtn, !current)
        }
    }

    private fun toggleBluetooth() {
        val bt = BluetoothAdapter.getDefaultAdapter()
        if (bt == null) { Toast.makeText(this, "No Bluetooth", Toast.LENGTH_SHORT).show(); return }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } else {
            @Suppress("DEPRECATION")
            if (bt.isEnabled) bt.disable() else bt.enable()
            updateToggleState(btBtn, !bt.isEnabled)
        }
    }

    private fun toggleMute() {
        val am = getSystemService(AudioManager::class.java)
        val muted = isMuted()
        am.setStreamVolume(
            AudioManager.STREAM_RING,
            if (muted) am.getStreamMaxVolume(AudioManager.STREAM_RING) / 2 else 0, 0
        )
        updateToggleState(muteBtn, !muted)
    }

    private fun setBright(percent: Int) {
        val result = controls.setBrightness(percent)
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
    }

    private fun runWifiScan(container: LinearLayout) {
        Toast.makeText(this, "Scanning WiFi networks…", Toast.LENGTH_SHORT).show()
        Thread {
            val threats = wifi.scanForThreats()
            runOnUiThread {
                val msg = if (threats.isEmpty()) "No suspicious networks detected nearby."
                else "⚠️ ${threats.size} suspicious network(s):\n" +
                     threats.joinToString("\n") { "• ${it.ssid} (risk ${it.riskScore})" }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun buildThreatCard(): LinearLayout {
        val data = battery.getBatteryData()
        val wifiResult = wifi.analyzeCurrentNetwork()

        val items = listOf(
            Triple(
                if (wifiResult == WifiSecurityMonitor.SecurityResult.OPEN_NETWORK) "⚠️" else "✅",
                "WiFi",
                if (wifiResult == WifiSecurityMonitor.SecurityResult.SECURE) "Encrypted"
                else if (wifiResult == WifiSecurityMonitor.SecurityResult.OPEN_NETWORK) "Open network!"
                else "Not connected"
            ),
            Triple(
                if (battery.isChargerSafe()) "✅" else "⚠️",
                "Charger",
                if (battery.isChargerSafe()) "${data.voltage}mV — safe" else "Voltage unstable!"
            ),
            Triple(
                if (data.temperature < 42f) "✅" else "🔥",
                "Temperature",
                "${data.temperature}°C${if (data.temperature > 42f) " — hot!" else " — healthy"}"
            ),
            Triple("✅", "Privacy scan", memory.getFact("last_security_alert").ifEmpty { "No alerts" })
        )

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@SecurityActivity)); cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(20), dp(16), dp(20), dp(16))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(20)) }

            items.forEach { (icon, title, desc) ->
                val row = LinearLayout(this@SecurityActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(8), 0, dp(8))
                }
                row.addView(TextView(this@SecurityActivity).apply {
                    text = icon; textSize = 18f; setPadding(0, 0, dp(14), 0)
                })
                val col = LinearLayout(this@SecurityActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                }
                col.addView(TextView(this@SecurityActivity).apply {
                    text = title; textSize = 13f; setTextColor(ThemeManager.getTextPrimary(this@SecurityActivity))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
                col.addView(TextView(this@SecurityActivity).apply {
                    text = desc; textSize = 12f; setTextColor(ThemeManager.getTextSecondary(this@SecurityActivity))
                })
                row.addView(col)
                addView(row)
            }
        }
    }

    private fun buildToggleBtn(icon: String, label: String, active: Boolean,
                               onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(if (active) ThemeManager.getAccentColor(this@SecurityActivity) else ThemeManager.getCardColor(this@SecurityActivity))
                cornerRadius = dp(14).toFloat()
                if (active) setStroke(1, ThemeManager.getAccentColor(this@SecurityActivity) or 0x33000000)
            }
            setPadding(dp(12), dp(16), dp(12), dp(16))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            setOnClickListener { onClick() }

            addView(TextView(this@SecurityActivity).apply { text = icon; textSize = 22f })
            addView(TextView(this@SecurityActivity).apply {
                text = label; textSize = 11f
                setTextColor(if (active) Color.WHITE else ThemeManager.getTextSecondary(this@SecurityActivity))
            })
        }
    }

    private fun updateToggleState(btn: LinearLayout, active: Boolean) {
        btn.background = GradientDrawable().apply {
            setColor(if (active) ThemeManager.getAccentColor(this@SecurityActivity) else ThemeManager.getCardColor(this@SecurityActivity))
            cornerRadius = dp(14).toFloat()
            if (active) setStroke(1, ThemeManager.getAccentColor(this@SecurityActivity) or 0x33000000)
        }
        (btn.getChildAt(1) as? TextView)?.setTextColor(
            if (active) Color.WHITE else ThemeManager.getTextSecondary(this@SecurityActivity)
        )
    }

    private fun scannerTile(icon: String, title: String, desc: String,
                            onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@SecurityActivity)); cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(10)) }
            setOnClickListener { onClick() }
            addView(TextView(this@SecurityActivity).apply {
                text = icon; textSize = 22f; setPadding(0, 0, dp(14), 0)
            })
            val col = LinearLayout(this@SecurityActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                addView(TextView(this@SecurityActivity).apply {
                    text = title; textSize = 14f; setTextColor(ThemeManager.getTextPrimary(this@SecurityActivity))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
                addView(TextView(this@SecurityActivity).apply {
                    text = desc; textSize = 12f; setTextColor(ThemeManager.getTextSecondary(this@SecurityActivity))
                })
            }
            addView(col)
            addView(TextView(this@SecurityActivity).apply {
                text = "→"; textSize = 16f; setTextColor(ThemeManager.getTextSecondary(this@SecurityActivity))
            })
        }
    }

    private fun spacer() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(10), 1)
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text; textSize = 11f; setTextColor(ThemeManager.getTextSecondary(this@SecurityActivity))
        letterSpacing = 0.15f; setPadding(0, dp(20), 0, dp(10))
    }
}
