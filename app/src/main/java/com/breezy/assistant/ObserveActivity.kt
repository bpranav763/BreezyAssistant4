package com.breezy.assistant

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask

class ObserveActivity : BaseActivity() {

    private var ramValue: TextView? = null
    private var storageValue: TextView? = null
    private var batteryValue: TextView? = null
    private var tempValue: TextView? = null
    private var voltageValue: TextView? = null
    private var healthValue: TextView? = null
    private var netDownValue: TextView? = null
    private var netUpValue: TextView? = null
    private var llmStatusValue: TextView? = null
    
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTime = 0L
    
    private var refreshTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(buildHeader("👁️ Observe System") { finish() })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(32))
        }

        // Real-time Stats Card
        container.addView(buildSectionLabel("REAL-TIME METRICS"))
        val statsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        ramValue = buildMetricRow(statsCard, "🧠 Available RAM", "0 MB")
        storageValue = buildMetricRow(statsCard, "📂 Free Storage", "0 GB")
        batteryValue = buildMetricRow(statsCard, "⚡ Battery Level", "0%")
        tempValue = buildMetricRow(statsCard, "🌡️ Temperature", "0°C")
        voltageValue = buildMetricRow(statsCard, "🔌 Voltage", "0 mV")
        healthValue = buildMetricRow(statsCard, "🏥 Battery Health", "Unknown")
        
        container.addView(buildSectionLabel("NETWORK THROUGHPUT"))
        val netCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        netDownValue = buildMetricRow(netCard, "⬇️ Download Speed", "0 kbps")
        netUpValue = buildMetricRow(netCard, "⬆️ Upload Speed", "0 kbps")
        container.addView(netCard)

        container.addView(buildSectionLabel("DEVICE IDENTITY"))
        val deviceCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        buildMetricRow(deviceCard, "📱 Model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        buildMetricRow(deviceCard, "🤖 Android Version", "v${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        buildMetricRow(deviceCard, "🏗️ Board/Hardware", "${android.os.Build.BOARD} / ${android.os.Build.HARDWARE}")
        llmStatusValue = buildMetricRow(deviceCard, "🧠 Breezy Brain", "Checking...")
        container.addView(deviceCard)

        // Tools Section
        container.addView(buildSectionLabel("MONITORING TOOLS"))
        
        container.addView(buildToolTile("📊", "App Usage Today", "See which apps are consuming your time.") {
            startActivity(Intent(this, AppUsageActivity::class.java))
        })
        
        container.addView(buildToolTile("🧹", "Storage Analyzer", "Identify large files and junk taking up space.") {
            startActivity(Intent(this, StorageAnalysisActivity::class.java))
        })

        container.addView(buildToolTile("🌐", "Network Speed", "Test your current connection speed.") {
            startActivity(Intent(this, SpeedTestActivity::class.java))
        })

        container.addView(buildToolTile("🧠", "AI Brain Stress Test", "Measure how many tokens/sec your CPU can handle.") {
            runStressTest()
        })

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)
        
        startRefreshing()
    }

    private fun buildMetricRow(parent: LinearLayout, label: String, initialValue: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(0xFF9CA3AF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val valueTv = TextView(this).apply {
            text = initialValue
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        row.addView(valueTv)
        parent.addView(row)
        return valueTv
    }

    private fun buildToolTile(icon: String, title: String, desc: String, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dp(12)) }
            setOnClickListener { onClick() }

            addView(TextView(this@ObserveActivity).apply {
                text = icon
                textSize = 24f
                setPadding(0, 0, dp(16), 0)
            })

            val textLayout = LinearLayout(this@ObserveActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textLayout.addView(TextView(this@ObserveActivity).apply {
                text = title
                textSize = 15f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            textLayout.addView(TextView(this@ObserveActivity).apply {
                text = desc
                textSize = 12f
                setTextColor(0xFF6B7280.toInt())
            })
            addView(textLayout)
            
            addView(TextView(this@ObserveActivity).apply {
                text = "→"
                setTextColor(0xFF4B5563.toInt())
                textSize = 18f
            })
        }
    }

    private fun buildSectionLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(0xFF4B5563.toInt())
            letterSpacing = 0.15f
            setPadding(0, dp(24), 0, dp(12))
        }
    }

    private fun startRefreshing() {
        lastRxBytes = android.net.TrafficStats.getTotalRxBytes()
        lastTxBytes = android.net.TrafficStats.getTotalTxBytes()
        lastTime = System.currentTimeMillis()
        
        val llm = LLMInference(this)

        refreshTimer = Timer()
        refreshTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val ram = getAvailableRAM()
                val storage = getAvailableStorage()
                val batteryData = BatteryMonitor(this@ObserveActivity).getBatteryData()
                
                val currentTime = System.currentTimeMillis()
                val currentRx = android.net.TrafficStats.getTotalRxBytes()
                val currentTx = android.net.TrafficStats.getTotalTxBytes()
                
                val timeDiff = (currentTime - lastTime) / 1000f
                val rxSpeed = if (timeDiff > 0) (currentRx - lastRxBytes) / timeDiff else 0f
                val txSpeed = if (timeDiff > 0) (currentTx - lastTxBytes) / timeDiff else 0f
                
                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastTime = currentTime

                val downText = formatSpeed(rxSpeed)
                val upText = formatSpeed(txSpeed)
                val llmStatus = if (llm.isReady()) "Local AI Active" else "AI Initializing..."
                
                runOnUiThread {
                    ramValue?.text = ram + " MB"
                    storageValue?.text = storage + " GB"
                    batteryValue?.text = batteryData.level.toString() + "%"
                    tempValue?.text = batteryData.temperature.toString() + "°C"
                    voltageValue?.text = batteryData.voltage.toString() + " mV"
                    healthValue?.text = formatBatteryHealth(batteryData.health)
                    netDownValue?.text = downText
                    netUpValue?.text = upText
                    llmStatusValue?.text = llmStatus
                }
            }
        }, 0, 1500)
    }

    private fun formatBatteryHealth(health: Int): String {
        return when (health) {
            android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            android.os.BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }

    private fun formatSpeed(bytesPerSec: Float): String {
        val kbps = (bytesPerSec * 8) / 1024f
        return when {
            kbps > 1000 -> String.format(java.util.Locale.US, "%.1f Mbps", kbps / 1024f)
            else -> String.format(java.util.Locale.US, "%.0f kbps", kbps)
        }
    }

    private fun runStressTest() {
        val llm = LLMInference(this)
        llmStatusValue?.text = "Running Stress Test..."
        llmStatusValue?.setTextColor(0xFFFBBF24.toInt()) // Amber

        lifecycleScope.launch(Dispatchers.IO) {
            val result = llm.runStressTest()
            withContext(Dispatchers.Main) {
                llmStatusValue?.text = "Speed: $result"
                llmStatusValue?.setTextColor(0xFF34D399.toInt()) // Green
                Toast.makeText(this@ObserveActivity, "AI Benchmark Complete: $result", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getAvailableRAM(): String {
        return try {
            val mi = ActivityManager.MemoryInfo()
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(mi)
            (mi.availMem / (1024L * 1024L)).toString()
        } catch (e: Throwable) {
            "0"
        }
    }

    private fun getAvailableStorage(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val gb = bytesAvailable / (1024.0 * 1024.0 * 1024.0)
            ((gb * 10).toInt() / 10.0).toString()
        } catch (e: Throwable) {
            "0.0"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshTimer?.cancel()
    }
}
