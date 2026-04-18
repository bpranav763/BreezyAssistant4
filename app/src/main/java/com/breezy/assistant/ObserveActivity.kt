package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.Timer
import java.util.TimerTask
import java.util.Locale

class ObserveActivity : BaseActivity() {

    private val inspector by lazy { HardwareInspector(this) }
    private var refreshTimer: Timer? = null
    private var lastRx = 0L; private var lastTx = 0L; private var lastTime = 0L

    // Live value refs
    private var tvRam: TextView? = null
    private var tvStorage: TextView? = null
    private var tvBattery: TextView? = null
    private var tvTemp: TextView? = null
    private var tvVoltage: TextView? = null
    private var tvHealth: TextView? = null
    private var tvNetDown: TextView? = null
    private var tvNetUp: TextView? = null
    private var tvLlm: TextView? = null
    private var tvCpuFreq: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        root.addView(buildHeader("📊 System Observer") { finish() })

        // Tab bar
        val tabs = listOf("Live", "CPU", "GPU", "Storage", "Network", "Sensors", "Cameras")
        val tabBar = buildTabBar(tabs)
        root.addView(tabBar.first)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(40))
        }
        scroll.addView(content)
        root.addView(scroll)

        setContentView(root)
        applySystemBarInsets(root)

        // Show first tab
        showTab(0, content, tabBar.second)

        lastRx = android.net.TrafficStats.getTotalRxBytes()
        lastTx = android.net.TrafficStats.getTotalTxBytes()
        lastTime = System.currentTimeMillis()

        startLiveRefresh()
    }

    private fun buildTabBar(labels: List<String>): Pair<HorizontalScrollView, List<TextView>> {
        val scrollView = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(0xFF111827.toInt())
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val tabViews = labels.mapIndexed { i, label ->
            TextView(this).apply {
                text = label; textSize = 13f
                setTextColor(if (i == 0) Color.WHITE else 0xFF4B5563.toInt())
                background = if (i == 0) GradientDrawable().apply {
                    setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(20).toFloat()
                } else null
                setPadding(dp(16), dp(8), dp(16), dp(8))
                layoutParams = LinearLayout.LayoutParams(-2, -2).also { it.setMargins(dp(4), 0, dp(4), 0) }
            }.also { row.addView(it) }
        }
        scrollView.addView(row)
        return Pair(scrollView, tabViews)
    }

    private fun showTab(index: Int, content: LinearLayout, tabViews: List<TextView>) {
        tabViews.forEachIndexed { i, tv ->
            tv.setTextColor(if (i == index) Color.WHITE else 0xFF4B5563.toInt())
            tv.background = if (i == index) GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(20).toFloat()
            } else null
            tv.setOnClickListener { showTab(i, content, tabViews) }
        }
        content.removeAllViews()
        when (index) {
            0 -> buildLiveTab(content)
            1 -> buildCpuTab(content)
            2 -> buildGpuTab(content)
            3 -> buildStorageTab(content)
            4 -> buildNetworkTab(content)
            5 -> buildSensorsTab(content)
            6 -> buildCamerasTab(content)
        }
    }

    // ── LIVE tab ────────────────────────────────────────────────────────────

    private fun buildLiveTab(c: LinearLayout) {
        c.addView(sectionLabel("LIVE METRICS"))
        val card = card()

        tvRam     = metricRow(card, "🧠  Available RAM", "…")
        tvStorage = metricRow(card, "📂  Free Storage",  "…")
        tvBattery = metricRow(card, "🔋  Battery",       "…")
        tvTemp    = metricRow(card, "🌡️  Temperature",   "…")
        tvVoltage = metricRow(card, "🔌  Voltage",       "…")
        tvHealth  = metricRow(card, "🏥  Health",        "…")
        c.addView(card)

        c.addView(sectionLabel("NETWORK THROUGHPUT"))
        val netCard = card()
        tvNetDown = metricRow(netCard, "⬇️  Download",  "…")
        tvNetUp   = metricRow(netCard, "⬆️  Upload",    "…")
        c.addView(netCard)

        c.addView(sectionLabel("AI BRAIN"))
        val aiCard = card()
        tvLlm     = metricRow(aiCard, "🧠  Status", "Checking…")
        tvCpuFreq = metricRow(aiCard, "⚡  CPU Speed", "…")
        c.addView(aiCard)

        c.addView(sectionLabel("MONITORING TOOLS"))
        listOf(
            Triple("📊", "App Usage Today", "Which apps consumed your time") to AppUsageActivity::class.java,
            Triple("🧹", "Storage Analyzer", "Find what\u0027s eating your space") to StorageAnalysisActivity::class.java,
            Triple("🌐", "Network Speed Test", "Real Mbps via Cloudflare") to SpeedTestActivity::class.java,
        ).forEach { (info, cls) ->
            c.addView(toolTile(info.first, info.second, info.third) {
                startActivity(android.content.Intent(this, cls))
            })
        }
        c.addView(toolTile("🤖", "AI Stress Test", "Tokens/sec your phone can handle") {
            runAiStressTest()
        })
    }

    // ── CPU tab ─────────────────────────────────────────────────────────────

    private fun buildCpuTab(c: LinearLayout) {
        c.addView(sectionLabel("PROCESSOR"))
        val cpu = inspector.getCpuInfo()
        val card = card()
        metricRow(card, "Hardware",       cpu.hardware)
        metricRow(card, "Processor",      cpu.processor)
        metricRow(card, "Cores",          "${cpu.coreCount} cores")
        metricRow(card, "ABI",            cpu.abi)
        metricRow(card, "Max Frequency",  cpu.maxFreqMHz)
        metricRow(card, "Current Freq",   cpu.currentFreqMHz)
        metricRow(card, "Governor",       cpu.governor)
        c.addView(card)

        c.addView(sectionLabel("DEVICE"))
        val devCard = card()
        metricRow(devCard, "Manufacturer", android.os.Build.MANUFACTURER)
        metricRow(devCard, "Model",        android.os.Build.MODEL)
        metricRow(devCard, "Device",       android.os.Build.DEVICE)
        metricRow(devCard, "Board",        android.os.Build.BOARD)
        metricRow(devCard, "Android",      "v${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        metricRow(devCard, "Bootloader",   android.os.Build.BOOTLOADER)
        metricRow(devCard, "Fingerprint",  android.os.Build.FINGERPRINT.takeLast(40))
        c.addView(devCard)
    }

    // ── GPU tab ─────────────────────────────────────────────────────────────

    private fun buildGpuTab(c: LinearLayout) {
        c.addView(sectionLabel("GRAPHICS"))
        val gpu = inspector.getGpuInfo()
        val display = inspector.getDisplayInfo()
        val card = card()
        metricRow(card, "GPU Vendor",     gpu.vendor.ifEmpty { "Unknown" })
        metricRow(card, "GPU Renderer",   gpu.renderer.ifEmpty { "Unknown" })
        metricRow(card, "OpenGL ES",      gpu.openGlVersion)
        metricRow(card, "Driver Version", gpu.driverVersion)
        c.addView(card)

        c.addView(sectionLabel("DISPLAY"))
        val dispCard = card()
        metricRow(dispCard, "Resolution",    "${display.widthPx}×${display.heightPx}px")
        metricRow(dispCard, "Refresh Rate",  "${String.format(Locale.US, "%.0f", display.refreshRateHz)}Hz")
        metricRow(dispCard, "Density",       "${display.densityDpi}dpi (${display.densityClass})")
        c.addView(dispCard)
    }

    // ── Storage tab ─────────────────────────────────────────────────────────

    private fun buildStorageTab(c: LinearLayout) {
        c.addView(sectionLabel("INTERNAL STORAGE"))
        val s = inspector.getStorageInfo()
        val card = card()
        metricRow(card, "Total",    "${String.format(Locale.US, "%.1f", s.totalGb)}GB")
        metricRow(card, "Used",     "${String.format(Locale.US, "%.1f", s.usedGb)}GB (${s.usedPercent}%)")
        metricRow(card, "Free",     "${String.format(Locale.US, "%.1f", s.freeGb)}GB")
        metricRow(card, "External", if (s.externalAvailable) "Available" else "Not mounted")
        c.addView(card)

        // Usage bar
        val bar = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progress = s.usedPercent
            layoutParams = LinearLayout.LayoutParams(-1, dp(6)).also { it.setMargins(0, dp(4), 0, dp(16)) }
            progressDrawable = GradientDrawable().apply {
                setColor(if (s.usedPercent > 85) 0xFFEF4444.toInt() else 0xFF1D4ED8.toInt())
                cornerRadius = dp(3).toFloat()
            }
        }
        c.addView(bar)
    }

    // ── Network tab ─────────────────────────────────────────────────────────

    private fun buildNetworkTab(c: LinearLayout) {
        c.addView(sectionLabel("WIFI"))
        val net = inspector.getNetworkInfo()
        val card = card()
        metricRow(card, "SSID",         net.wifiSsid)
        metricRow(card, "BSSID",        net.wifiBssid)
        metricRow(card, "Frequency",    "${net.wifiFrequencyMHz}MHz (${if (net.wifiFrequencyMHz > 4000) "5GHz" else "2.4GHz"})")
        metricRow(card, "Signal",       "${net.wifiSignalDbm}dBm")
        metricRow(card, "Link Speed",   "${net.wifiLinkSpeedMbps}Mbps")
        metricRow(card, "IP Address",   net.ipAddress)
        c.addView(card)

        c.addView(sectionLabel("CELLULAR"))
        val cellCard = card()
        metricRow(cellCard, "Carrier",      net.carrier)
        metricRow(cellCard, "Network Type", net.networkType)
        c.addView(cellCard)
    }

    // ── Sensors tab ─────────────────────────────────────────────────────────

    private fun buildSensorsTab(c: LinearLayout) {
        c.addView(sectionLabel("HARDWARE SENSORS"))
        val sensors = inspector.getSensorInfo()
        c.addView(infoChip("${sensors.count} sensors detected"))
        val card = card()
        sensors.names.forEach { name ->
            card.addView(TextView(this).apply {
                text = "• $name"; setTextColor(0xFF9CA3AF.toInt()); textSize = 13f
                setPadding(0, dp(4), 0, dp(4))
            })
        }
        c.addView(card)
    }

    // ── Cameras tab ─────────────────────────────────────────────────────────

    private fun buildCamerasTab(c: LinearLayout) {
        c.addView(sectionLabel("CAMERAS"))
        val cameras = inspector.getCameraInfo()
        if (cameras.isEmpty()) {
            c.addView(infoChip("No camera info available"))
            return
        }
        cameras.forEach { cam ->
            c.addView(TextView(this).apply {
                text = "${cam.facing} Camera (ID ${cam.id})"
                textSize = 13f; setTextColor(0xFF4B5563.toInt()); letterSpacing = 0.15f
                setPadding(0, dp(20), 0, dp(8))
            })
            val card = card()
            metricRow(card, "Resolution",   "${String.format(Locale.US, "%.1f", cam.megapixels)}MP")
            metricRow(card, "Max Zoom",     "${cam.maxZoom}×")
            metricRow(card, "Flash",        if (cam.flashSupported) "Supported" else "No")
            metricRow(card, "Focal Length", cam.focalLengths)
            c.addView(card)
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply { setColor(0xFF111827.toInt()); cornerRadius = dp(14).toFloat() }
        setPadding(dp(18), dp(14), dp(18), dp(14))
        layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(12)) }
    }

    private fun metricRow(parent: LinearLayout, label: String, value: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 13f; setTextColor(0xFF6B7280.toInt())
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        val valTv = TextView(this).apply {
            text = value; textSize = 13f; setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD; gravity = android.view.Gravity.END
        }
        row.addView(valTv); parent.addView(row); return valTv
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text; textSize = 11f; setTextColor(0xFF4B5563.toInt()); letterSpacing = 0.15f
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun infoChip(text: String) = TextView(this).apply {
        this.text = text; textSize = 12f; setTextColor(0xFF38BDF8.toInt())
        background = GradientDrawable().apply { setColor(0xFF0F2235.toInt()); cornerRadius = dp(8).toFloat() }
        setPadding(dp(12), dp(6), dp(12), dp(6))
        layoutParams = LinearLayout.LayoutParams(-2, -2).also { it.setMargins(0, 0, 0, dp(8)) }
    }

    private fun toolTile(icon: String, title: String, desc: String, onClick: () -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply { setColor(0xFF111827.toInt()); cornerRadius = dp(12).toFloat() }
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(10)) }
            setOnClickListener { onClick() }
            addView(TextView(this@ObserveActivity).apply { text = icon; textSize = 22f; setPadding(0, 0, dp(14), 0) })
            val col = LinearLayout(this@ObserveActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                addView(TextView(this@ObserveActivity).apply { text = title; textSize = 14f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD })
                addView(TextView(this@ObserveActivity).apply { text = desc; textSize = 11f; setTextColor(0xFF6B7280.toInt()) })
            }
            addView(col)
            addView(TextView(this@ObserveActivity).apply { text = "→"; setTextColor(0xFF4B5563.toInt()); textSize = 16f })
        }

    // ── Live refresh ─────────────────────────────────────────────────────────

    private fun startLiveRefresh() {
        val llm = LLMInference(this)
        refreshTimer = Timer()
        refreshTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val ram     = inspector.getRamInfo()
                val storage = inspector.getStorageInfo()
                val battery = BatteryMonitor(this@ObserveActivity).getBatteryData()
                val cpu     = inspector.getCpuInfo()

                val nowTime = System.currentTimeMillis()
                val curRx = android.net.TrafficStats.getTotalRxBytes()
                val curTx = android.net.TrafficStats.getTotalTxBytes()
                val secs  = ((nowTime - lastTime) / 1000f).coerceAtLeast(0.1f)
                val rxKbps = (curRx - lastRx) * 8f / 1024f / secs
                val txKbps = (curTx - lastTx) * 8f / 1024f / secs
                lastRx = curRx; lastTx = curTx; lastTime = nowTime

                val health = when (battery.health) {
                    android.os.BatteryManager.BATTERY_HEALTH_GOOD     -> "Good ✓"
                    android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat ⚠️"
                    android.os.BatteryManager.BATTERY_HEALTH_DEAD      -> "Dead ✗"
                    else -> "Unknown"
                }

                runOnUiThread {
                    tvRam?.text     = "${ram.availableMb}MB free / ${ram.totalMb}MB (${ram.usedPercent}%)"
                    tvStorage?.text = "${String.format(Locale.US, "%.1f", storage.freeGb)}GB free"
                    tvBattery?.text = "${battery.level}%${if (battery.isCharging) " ⚡" else ""}"
                    tvTemp?.text    = "${battery.temperature}°C".also {
                        tvTemp?.setTextColor(if (battery.temperature > 43f) 0xFFFB923C.toInt() else Color.WHITE)
                    }
                    tvVoltage?.text = "${battery.voltage}mV"
                    tvHealth?.text  = health
                    tvNetDown?.text = formatSpeed(rxKbps)
                    tvNetUp?.text   = formatSpeed(txKbps)
                    tvCpuFreq?.text = cpu.currentFreqMHz
                    tvLlm?.text     = if (llm.isReady()) "Local AI active ✓" else "Downloading / unavailable"
                }
            }
        }, 0, 1500)
    }

    private fun formatSpeed(kbps: Float) = if (kbps > 1024)
        "${String.format(Locale.US, "%.1f", kbps / 1024f)} Mbps"
    else "${String.format(Locale.US, "%.0f", kbps)} kbps"

    private fun runAiStressTest() {
        tvLlm?.text = "Running…"; tvLlm?.setTextColor(0xFFFBBF24.toInt())
        lifecycleScope.launch(Dispatchers.IO) {
            val r = LLMInference(this@ObserveActivity).runStressTest()
            withContext(Dispatchers.Main) {
                tvLlm?.text = "Benchmark: $r"; tvLlm?.setTextColor(0xFF34D399.toInt())
                Toast.makeText(this@ObserveActivity, "AI Benchmark: $r", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); refreshTimer?.cancel() }
}
