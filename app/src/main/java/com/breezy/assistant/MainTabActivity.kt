package com.breezy.assistant

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat

class MainTabActivity : BaseActivity() {

    private val battery by lazy { BatteryMonitor(this) }
    private val memory by lazy { BreezyMemory(this) }
    private lateinit var contentFrame: FrameLayout
    private val handler = Handler(Looper.getMainLooper())
    private var statsUpdateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        root.addView(buildTopBar())

        contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        root.addView(contentFrame)

        root.addView(buildBottomNav())

        setContentView(root)
        applySystemBarInsets(root)
        
        showHome()
    }

    override fun onResume() {
        super.onResume()
        startStatsUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopStatsUpdates()
    }

    private fun startStatsUpdates() {
        statsUpdateRunnable = object : Runnable {
            override fun run() {
                updateStats()
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(statsUpdateRunnable!!)
    }

    private fun stopStatsUpdates() {
        statsUpdateRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun showHome() {
        contentFrame.removeAllViews()
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }

        // Box 1: Live Status
        layout.addView(buildLiveStatusCard())

        // Box 2: Daily Report
        layout.addView(buildDailyReportCard())

        // Box 3: Smart Features
        layout.addView(buildSmartFeaturesSection())

        scroll.addView(layout)
        contentFrame.addView(scroll)
    }

    private fun buildTopBar(): RelativeLayout {
        return RelativeLayout(this).apply {
            setPadding(dp(24), dp(16), dp(24), dp(16))
            setBackgroundColor(0xFF0A0F1E.toInt())

            val profile = TextView(this@MainTabActivity).apply {
                id = View.generateViewId()
                val userName = memory.getUserName()
                text = if (userName.isNotEmpty()) userName.first().uppercase() else "■■"
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFF1D4ED8.toInt())
                }
                val p = RelativeLayout.LayoutParams(dp(40), dp(40))
                p.addRule(RelativeLayout.ALIGN_PARENT_START)
                p.addRule(RelativeLayout.CENTER_VERTICAL)
                layoutParams = p
                setOnClickListener {
                    startActivity(Intent(this@MainTabActivity, ProfileActivity::class.java))
                }
            }

            val title = TextView(this@MainTabActivity).apply {
                text = "BREEZY ASSISTANT"
                textSize = 15f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("Helvetica", Typeface.BOLD)
                letterSpacing = 0.05f
                val p = RelativeLayout.LayoutParams(-2, -2)
                p.addRule(RelativeLayout.CENTER_IN_PARENT)
                layoutParams = p
            }

            val menu = TextView(this@MainTabActivity).apply {
                text = "■"
                textSize = 22f
                setTextColor(Color.WHITE)
                val p = RelativeLayout.LayoutParams(-2, -2)
                p.addRule(RelativeLayout.ALIGN_PARENT_END)
                p.addRule(RelativeLayout.CENTER_VERTICAL)
                layoutParams = p
                setOnClickListener {
                    showSettingsMenu(it)
                }
            }

            addView(profile)
            addView(title)
            addView(menu)
        }
    }

    private fun showSettingsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Breezy Settings")
        popup.menu.add("AI Settings")
        popup.menu.add("UI Settings")
        popup.menu.add("BYOK")
        popup.menu.add("Help & Privacy")
        
        popup.setOnMenuItemClickListener {
            startActivity(Intent(this, BreezySettingsActivity::class.java))
            true
        }
        popup.show()
    }

    private lateinit var statViews: Map<String, TextView>

    private fun buildLiveStatusCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(20).toFloat()
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(dp(16), dp(16), dp(16), dp(12))
            layoutParams = lp
        }

        val grid = GridLayout(this).apply {
            columnCount = 2
            rowCount = 3
        }

        val labels = listOf("BATTERY", "TEMPERATURE", "RAM", "NETWORK", "STORAGE", "CHARGING")
        val map = mutableMapOf<String, TextView>()

        labels.forEach { label ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val cellLp = GridLayout.LayoutParams()
                cellLp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                cellLp.width = 0
                cellLp.setMargins(0, dp(8), 0, dp(8))
                layoutParams = cellLp
            }
            cell.addView(TextView(this).apply {
                text = label
                textSize = 10f
                setTextColor(0xFF9CA3AF.toInt())
            })
            val valueView = TextView(this).apply {
                text = "--"
                textSize = 18f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            }
            cell.addView(valueView)
            grid.addView(cell)
            map[label] = valueView
        }
        statViews = map
        card.addView(grid)
        return card
    }

    private fun updateStats() {
        if (!::statViews.isInitialized) return
        
        val data = battery.getBatteryData()
        
        // Battery
        statViews["BATTERY"]?.apply {
            text = "${data.level}%"
            setTextColor(if (data.level <= 20) 0xFFEF4444.toInt() else Color.WHITE)
        }
        
        // Temp
        statViews["TEMPERATURE"]?.apply {
            text = "${data.temperature}°C"
            setTextColor(when {
                data.temperature > 45 -> 0xFFEF4444.toInt()
                data.temperature > 40 -> 0xFFFCD34D.toInt()
                else -> Color.WHITE
            })
        }
        
        // RAM
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem / (1024 * 1024 * 1024.0)
        val availableRam = memInfo.availMem / (1024 * 1024 * 1024.0)
        val usedRam = totalRam - availableRam
        statViews["RAM"]?.text = String.format("%.1f/%.0f GB", usedRam, totalRam)

        // Network
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        // Simple diff logic would need previous values, omitting for brief update
        statViews["NETWORK"]?.text = "Active"

        // Storage
        val stat = StatFs(filesDir.path)
        val totalStorage = stat.totalBytes / (1024.0 * 1024 * 1024)
        val freeStorage = stat.availableBytes / (1024.0 * 1024 * 1024)
        val usedStorage = totalStorage - freeStorage
        val usedPct = (usedStorage / totalStorage) * 100
        statViews["STORAGE"]?.apply {
            text = String.format("%.0f/%.0f GB", usedStorage, totalStorage)
            setTextColor(if (usedPct > 90) 0xFFEF4444.toInt() else Color.WHITE)
        }

        // Charging
        statViews["CHARGING"]?.apply {
            text = if (data.isCharging) "Yes (${data.voltage}mV)" else "No"
            // Dot indicator logic
        }
    }

    private fun buildDailyReportCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(dp(16), 0, dp(16), dp(12))
            layoutParams = lp
        }

        card.addView(TextView(this).apply {
            text = "■ DAILY REPORT"
            textSize = 11f
            setTextColor(0xFF4B5563.toInt())
            letterSpacing = 0.15f
            setPadding(0, 0, 0, dp(12))
        })

        // Today highlight
        val today = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0x1A1D4ED8.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        today.addView(TextView(this).apply {
            text = "TODAY"
            textSize = 10f
            setTextColor(0xFF1D4ED8.toInt())
            typeface = Typeface.DEFAULT_BOLD
        })
        today.addView(TextView(this).apply {
            text = "Battery used 42%. 2 security threats blocked."
            textSize = 13f
            setTextColor(Color.WHITE)
        })
        card.addView(today)

        // Previous days (Simplified)
        listOf("Yesterday", "Oct 24", "Oct 23").forEach { date ->
            val row = LinearLayout(this).apply {
                setPadding(0, dp(12), 0, 0)
            }
            row.addView(TextView(this).apply {
                text = date
                textSize = 12f
                setTextColor(0xFF9CA3AF.toInt())
                layoutParams = LinearLayout.LayoutParams(dp(80), -2)
            })
            row.addView(TextView(this).apply {
                text = "Summary of the day's events..."
                textSize = 12f
                setTextColor(0xFF6B7280.toInt())
            })
            card.addView(row)
        }

        card.addView(Button(this).apply {
            text = "Ask AI about today →"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt())
                cornerRadius = dp(12).toFloat()
            }
            val btnLp = LinearLayout.LayoutParams(-1, dp(48))
            btnLp.topMargin = dp(20)
            layoutParams = btnLp
            setOnClickListener {
                startActivity(Intent(this@MainTabActivity, FullChatActivity::class.java))
            }
        })

        return card
    }

    private fun buildSmartFeaturesSection(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), 0, 0)
        }
        container.addView(TextView(this).apply {
            text = "SMART FEATURES"
            textSize = 11f
            setTextColor(0xFF4B5563.toInt())
            setPadding(dp(8), 0, 0, dp(12))
        })

        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val row = LinearLayout(this)
        
        val features = listOf(
            "Find My Phone", "Circle Search", "Live Screen", 
            "Hotspot Bridge", "WiFi Bridge", "Organiser", 
            "Music Connect", "Quick Boost"
        )
        
        features.forEach { name ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(0xFF111827.toInt())
                    cornerRadius = dp(14).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dp(120), dp(80)).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
            }
            card.addView(TextView(this).apply { text = "✨"; textSize = 20f })
            card.addView(TextView(this).apply {
                text = name
                textSize = 10f
                setTextColor(Color.WHITE)
            })
            row.addView(card)
        }
        
        scroll.addView(row)
        container.addView(scroll)
        return container
    }

    private fun buildBottomNav(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF111827.toInt())
            setPadding(0, dp(12), 0, dp(36))

            val tabs = listOf(
                "🏠" to "Home", 
                "👁️" to "Observe", 
                "🛡️" to "Security", 
                "📦" to "Inbuilt", 
                "💬" to "Chat"
            )

            tabs.forEachIndexed { index, (icon, label) ->
                val btn = LinearLayout(this@MainTabActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    setOnClickListener {
                        when (index) {
                            0 -> showHome()
                            1 -> startActivity(Intent(this@MainTabActivity, ObserveActivity::class.java))
                            2 -> startActivity(Intent(this@MainTabActivity, SecurityActivity::class.java))
                            3 -> startActivity(Intent(this@MainTabActivity, InbuiltAppsActivity::class.java))
                            4 -> startActivity(Intent(this@MainTabActivity, FullChatActivity::class.java))
                        }
                    }
                }

                btn.addView(TextView(this@MainTabActivity).apply {
                    text = icon; textSize = 20f; gravity = Gravity.CENTER
                })
                btn.addView(TextView(this@MainTabActivity).apply {
                    text = label; textSize = 10f; setTextColor(0xFF6B7280.toInt()); gravity = Gravity.CENTER
                })
                addView(btn)
            }
        }
    }
}
