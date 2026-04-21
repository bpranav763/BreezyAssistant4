package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainTabActivity : BaseActivity() {

    private val batteryMonitor by lazy { BatteryMonitor(this) }
    private val memory by lazy { BreezyMemory(this) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var contentFrame: FrameLayout
    private val statsViews = mutableMapOf<String, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@MainTabActivity))
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

    private fun buildTopBar(): RelativeLayout {
        return RelativeLayout(this).apply {
            setPadding(dp(20), dp(12), dp(20), dp(12))
            setBackgroundColor(ThemeManager.getBackgroundColor(this@MainTabActivity))

            // Profile avatar (left)
            val profile = TextView(context).apply {
                id = View.generateViewId()
                val userName = memory.getUserName()
                text = if (userName.isNotEmpty()) userName.first().uppercase() else "B"
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ThemeManager.getAccentColor(this@MainTabActivity))
                }
                layoutParams = RelativeLayout.LayoutParams(dp(40), dp(40)).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
                setOnClickListener {
                    startActivity(Intent(context, ProfileActivity::class.java))
                }
            }
            addView(profile)

            // Title (center)
            addView(TextView(context).apply {
                text = "BREEZY"
                textSize = 15f
                setTextColor(ThemeManager.getTextPrimary(this@MainTabActivity))
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.1f
                layoutParams = RelativeLayout.LayoutParams(-2, -2).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
            })

            // Three-dots menu (right)
            val menuBtn = TextView(context).apply {
                text = "⋮"
                textSize = 24f
                setTextColor(ThemeManager.getTextPrimary(this@MainTabActivity))
                gravity = Gravity.CENTER
                layoutParams = RelativeLayout.LayoutParams(dp(28), dp(28)).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
                setOnClickListener { showMenu() }
            }
            addView(menuBtn)
        }
    }

    private fun showMenu() {
        val popup = PopupMenu(this, findViewById(android.R.id.content), Gravity.END)
        popup.menu.add("Settings")
        popup.menu.add("About")
        popup.setOnMenuItemClickListener {
            when (it.title) {
                "Settings" -> startActivity(Intent(this, BreezySettingsActivity::class.java))
                "About" -> Toast.makeText(this, "Breezy V4 Midnight", Toast.LENGTH_SHORT).show()
            }
            true
        }
        popup.show()
    }

    private fun showHome() {
        contentFrame.removeAllViews()
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }

        // Box 1: Live Status
        container.addView(buildLiveStatusCard())

        // Box 2: Daily Report
        container.addView(buildDailyReportCard())

        // Box 3: Smart Features
        container.addView(buildSmartFeaturesRow())

        scroll.addView(container)
        contentFrame.addView(scroll)
        startLiveUpdates()
    }

    private fun buildLiveStatusCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@MainTabActivity))
                cornerRadius = cornerRadius().toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }

        val grid = GridLayout(this).apply {
            columnCount = 2
        }

        val cells = listOf(
            "🔋 Battery" to "",
            "🌡️ Temp" to "",
            "🧠 RAM" to "",
            "📶 Network" to "",
            "💾 Storage" to "",
            "⚡ Charging" to ""
        )

        cells.forEach { (label, _) ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(8), dp(8), dp(8))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                addView(TextView(this@MainTabActivity).apply {
                    text = label
                    textSize = 10f
                    setTextColor(ThemeManager.getTextSecondary(this@MainTabActivity))
                })
                val valueView = TextView(this@MainTabActivity).apply {
                    text = "—"
                    textSize = 18f
                    setTextColor(ThemeManager.getTextPrimary(this@MainTabActivity))
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(valueView)
                statsViews[label] = valueView
            }
            grid.addView(cell)
        }
        card.addView(grid)
        return card
    }

    private fun buildDailyReportCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@MainTabActivity))
                cornerRadius = cornerRadius().toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 0, 0, dp(12))
            }

            addView(TextView(this@MainTabActivity).apply {
                text = "DAILY REPORT"
                textSize = 11f
                setTextColor(ThemeManager.getAccentColor(this@MainTabActivity))
                letterSpacing = 0.15f
                setPadding(0, 0, 0, dp(12))
            })

            // Today's highlight
            addView(buildTodayHighlight())

            // History rows (simplified)
            for (i in 1..2) {
                addView(buildHistoryRow("Yesterday", "Battery: 78% • 4h screen time"))
            }

            // Ask AI button
            addView(TextView(this@MainTabActivity).apply {
                text = "Ask AI about today →"
                textSize = 14f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(ThemeManager.getAccentColor(this@MainTabActivity))
                    cornerRadius = dp(12).toFloat()
                }
                setPadding(0, dp(12), 0, dp(12))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
                setOnClickListener {
                    val intent = Intent(this@MainTabActivity, FullChatActivity::class.java)
                    intent.putExtra("prefill", "Summarize my phone usage today")
                    startActivity(intent)
                }
            })
        }
    }

    private fun buildTodayHighlight(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getAccentColor(this@MainTabActivity) and 0x1AFFFFFF.toInt()) // 10% tint
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }

            addView(TextView(this@MainTabActivity).apply {
                text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
                textSize = 14f
                setTextColor(ThemeManager.getAccentColor(this@MainTabActivity))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            addView(TextView(this@MainTabActivity).apply {
                text = "Battery: 82% • 3.2h screen"
                textSize = 12f
                setTextColor(ThemeManager.getTextSecondary(this@MainTabActivity))
            })
        }
    }

    private fun buildHistoryRow(date: String, summary: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
            addView(TextView(this@MainTabActivity).apply {
                text = date
                textSize = 13f
                setTextColor(ThemeManager.getTextPrimary(this@MainTabActivity))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            addView(TextView(this@MainTabActivity).apply {
                text = summary
                textSize = 12f
                setTextColor(ThemeManager.getTextSecondary(this@MainTabActivity))
            })
        }
    }

    private fun buildSmartFeaturesRow(): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(TextView(this@MainTabActivity).apply {
            text = "SMART FEATURES"
            textSize = 11f
            setTextColor(ThemeManager.getAccentColor(this@MainTabActivity))
            letterSpacing = 0.15f
            setPadding(0, dp(8), 0, dp(8))
        })

        val scroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            isHorizontalScrollBarEnabled = false
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val features = listOf(
            "🔍" to "Find My Phone",
            "🔄" to "Circle Search",
            "👁️" to "Live Screen",
            "📡" to "Hotspot Bridge",
            "📶" to "WiFi Bridge",
            "📁" to "Organiser",
            "🎵" to "Music Connect",
            "⚡" to "Quick Boost"
        )

        features.forEach { (icon, name) ->
            row.addView(buildFeatureCard(icon, name))
            row.addView(View(this@MainTabActivity).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) })
        }

        scroll.addView(row)
        container.addView(scroll)
        return container
    }

    private fun buildFeatureCard(icon: String, name: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@MainTabActivity))
                cornerRadius = cornerRadius().toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(dp(100), dp(90))

            addView(TextView(context).apply { text = icon; textSize = 24f })
            addView(TextView(context).apply {
                text = name
                textSize = 10f
                setTextColor(ThemeManager.getTextPrimary(this@MainTabActivity))
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            })
            setOnClickListener { onFeatureClick(name) }
        }
    }

    private fun onFeatureClick(name: String) {
        when (name) {
            "Find My Phone" -> SafeWordReceiver.Companion.triggerRing(this)
            "Hotspot Bridge" -> startActivity(Intent(this, HotspotBridgeActivity::class.java))
            "Quick Boost" -> {
                Toast.makeText(this, "Boosting...", Toast.LENGTH_SHORT).show()
                updateLiveStats()
            }
            else -> Toast.makeText(this, "$name coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLiveUpdates() {
        val runnable = object : Runnable {
            override fun run() {
                updateLiveStats()
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable)
    }

    private fun updateLiveStats() {
        val data = batteryMonitor.getBatteryData()
        val hardware = HardwareInspector(this)
        val ramInfo = hardware.getRamInfo()
        val storageInfo = hardware.getStorageInfo()

        statsViews["🔋 Battery"]?.text = "${data.level}%"
        statsViews["🌡️ Temp"]?.text = "${data.temperature}°C"
        statsViews["🧠 RAM"]?.text = "${ramInfo.availableMb}MB free"
        statsViews["💾 Storage"]?.text = "${String.format("%.1f", storageInfo.freeGb)}GB free"
        statsViews["⚡ Charging"]?.text = if (data.isCharging) "Yes ⚡" else "No"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun buildBottomNav(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(ThemeManager.getCardColor(this@MainTabActivity))
            setPadding(0, dp(8), 0, dp(8))
            gravity = Gravity.CENTER_VERTICAL

            val tabs = listOf(
                "🏠" to "Home",
                "👁️" to "Observe",
                "🛡️" to "Security",
                "📦" to "Apps",
                "💬" to "Chat"
            )

            tabs.forEach { (icon, name) ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    addView(TextView(context).apply { text = icon; textSize = 20f })
                    addView(TextView(context).apply {
                        text = name
                        textSize = 9f
                        setTextColor(if (name == "Home") ThemeManager.getAccentColor(this@MainTabActivity) else ThemeManager.getTextSecondary(this@MainTabActivity))
                    })
                    setOnClickListener {
                        when (name) {
                            "Observe" -> startActivity(Intent(context, ObserveActivity::class.java))
                            "Security" -> startActivity(Intent(context, SecurityActivity::class.java))
                            "Apps" -> startActivity(Intent(context, InbuiltAppsActivity::class.java))
                            "Chat" -> startActivity(Intent(context, FullChatActivity::class.java))
                        }
                    }
                })
            }
        }
    }
}
