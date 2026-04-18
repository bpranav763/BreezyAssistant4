package com.breezy.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class MainTabActivity : BaseActivity() {

    private val battery by lazy { BatteryMonitor(this) }
    private val memory by lazy { BreezyMemory(this) }
    private lateinit var contentFrame: FrameLayout

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

        root.addView(buildTopBar())

        contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(contentFrame)

        root.addView(buildBottomNav())

        setContentView(root)
        applySystemBarInsets(root)
    }

    override fun onResume() {
        super.onResume()
        showHome()
    }

    private fun showHome() {
        try {
            contentFrame.removeAllViews()
            val scroll = ScrollView(this)
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(16), dp(24), dp(24))
            }

            val data = battery.getBatteryData()

            // 1. LIVE STATUS
            layout.addView(buildSectionLabel("LIVE STATUS"))
            val statsGrid = GridLayout(this).apply {
                columnCount = 2
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            statsGrid.addView(buildStatTile("🔋 Battery", "${data.level}%"))
            statsGrid.addView(buildStatTile("🌡️ Temp", "${data.temperature}°C"))
            layout.addView(statsGrid)

            // 2. QUICK ACTIONS
            layout.addView(buildSectionLabel("QUICK ACTIONS"))
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            actions.addView(buildAppTile("⚡", "Automation\nTriggers") {
                startActivity(Intent(this@MainTabActivity, TriggerActivity::class.java))
            })
            actions.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(12), 1) })
            actions.addView(buildAppTile("🚀", "Speed\nTest") {
                startActivity(Intent(this@MainTabActivity, SpeedTestActivity::class.java))
            })
            layout.addView(actions)

            // 3. INBUILT APPS
            layout.addView(buildSectionLabel("INBUILT APPS"))
            
            val grid1 = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            grid1.addView(buildAppTile("🔐", "Security\nVault") {
                startActivity(Intent(this@MainTabActivity, VaultActivity::class.java))
            })
            grid1.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(12), 1) })
            grid1.addView(buildAppTile("📱", "Startup\nNotes") {
                startActivity(Intent(this@MainTabActivity, AppNoteActivity::class.java))
            })
            layout.addView(grid1)

            layout.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(12)) })

            val grid2 = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            grid2.addView(buildAppTile("📝", "Private\nNotes") {
                startActivity(Intent(this@MainTabActivity, NotesActivity::class.java))
            })
            grid2.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(12), 1) })
            grid2.addView(buildAppTile("📡", "Hotspot\nBridge") {
                startActivity(Intent(this@MainTabActivity, HotspotBridgeActivity::class.java))
            })
            layout.addView(grid2)

            scroll.addView(layout)
            contentFrame.addView(scroll)
        } catch (e: Exception) {
            android.util.Log.e("BREEZY", "showHome Crash: ${e.message}")
            contentFrame.removeAllViews()
            contentFrame.addView(TextView(this).apply {
                text = "Error loading home: ${e.message}"
                setTextColor(Color.RED)
                setPadding(dp(24), dp(24), dp(24), dp(24))
            })
        }
    }

    private fun buildTopBar(): RelativeLayout {
        return RelativeLayout(this).apply {
            setPadding(dp(24), dp(16), dp(24), dp(16))
            setBackgroundColor(0xFF0A0F1E.toInt())

            val profile = TextView(this@MainTabActivity).apply {
                id = View.generateViewId()
                // Show initial or 🌬️ if no name set
                val userName = memory.getUserName()
                text = if (userName.isNotEmpty())
                    userName.first().uppercase()
                else "🌬"
                textSize = if (userName.isNotEmpty()) 16f else 18f
                setTextColor(android.graphics.Color.WHITE)
                gravity = android.view.Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFF1D4ED8.toInt())
                }
                val p = RelativeLayout.LayoutParams(dp(40), dp(40))
                p.addRule(RelativeLayout.ALIGN_PARENT_START)
                p.addRule(RelativeLayout.CENTER_VERTICAL)
                layoutParams = p
                // Clicking profile opens settings to change name
                setOnClickListener {
                    startActivity(android.content.Intent(
                        this@MainTabActivity, SettingsActivity::class.java
                    ))
                }
            }

            val title = TextView(this@MainTabActivity).apply {
                text = "Breezy"
                textSize = 18f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val p = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                p.addRule(RelativeLayout.CENTER_IN_PARENT)
                layoutParams = p
            }

            addView(profile)
            addView(title)
        }
    }

    private fun buildBottomNav(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF111827.toInt())
            setPadding(0, dp(12), 0, dp(36))

            val tabs = listOf("🏠" to "Home", "🛡️" to "Security", "👁️" to "Observe", "💬" to "Chat")

            tabs.forEachIndexed { index, (icon, label) ->
                val btn = LinearLayout(this@MainTabActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(0, dp(8), 0, dp(8))
                    setOnClickListener {
                        when (index) {
                            0 -> showHome()
                            1 -> startActivity(Intent(this@MainTabActivity, SecurityActivity::class.java))
                            2 -> startActivity(Intent(this@MainTabActivity, ObserveActivity::class.java))
                            3 -> startActivity(Intent(this@MainTabActivity, FullChatActivity::class.java))
                        }
                    }
                }

                btn.addView(TextView(this@MainTabActivity).apply {
                    text = icon
                    textSize = 20f
                    gravity = Gravity.CENTER
                })

                btn.addView(TextView(this@MainTabActivity).apply {
                    text = label
                    textSize = 10f
                    setTextColor(0xFF6B7280.toInt())
                    gravity = Gravity.CENTER
                })

                addView(btn)
            }
        }
    }

    private fun buildStatTile(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            
            addView(TextView(this@MainTabActivity).apply {
                text = label
                textSize = 12f
                setTextColor(0xFF9CA3AF.toInt())
            })
            addView(TextView(this@MainTabActivity).apply {
                text = value
                textSize = 18f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun buildAppTile(icon: String, title: String, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt())
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClick() }
            
            addView(TextView(this@MainTabActivity).apply {
                text = icon
                textSize = 24f
                setPadding(0, 0, 0, dp(8))
            })
            addView(TextView(this@MainTabActivity).apply {
                text = title
                textSize = 13f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
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
}
