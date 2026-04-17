package com.breezy.assistant

import android.app.usage.UsageStatsManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AppUsageActivity : BaseActivity() {

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

        root.addView(buildHeader("📊 App Usage Today") { finish() })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(32))
        }

        if (!hasUsagePermission()) {
            container.addView(buildPermCard())
        } else {
            loadUsageStats(container)
        }

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun hasUsagePermission(): Boolean {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, -1)
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis()
        )
        return stats != null && stats.isNotEmpty()
    }

    private fun buildPermCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt()); cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(24), dp(24), dp(24), dp(24))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            addView(TextView(this@AppUsageActivity).apply {
                text = "📊 Permission Needed"
                textSize = 18f; setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, dp(12))
            })
            addView(TextView(this@AppUsageActivity).apply {
                text = "Breezy needs Usage Access permission to show which apps you've been using today."
                textSize = 13f; setTextColor(0xFF9CA3AF.toInt()); setPadding(0, 0, 0, dp(20))
            })
            addView(TextView(this@AppUsageActivity).apply {
                text = "Grant Permission"
                textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(10).toFloat()
                }
                setPadding(0, dp(16), 0, dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            })
        }
    }

    private fun loadUsageStats(container: LinearLayout) {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis, System.currentTimeMillis()
        )?.filter { it.totalTimeInForeground > 0 }
            ?.sortedByDescending { it.totalTimeInForeground }
            ?.take(15) ?: emptyList()

        if (stats.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No usage data yet today. Check back later."
                setTextColor(0xFF6B7280.toInt()); gravity = Gravity.CENTER
                setPadding(0, dp(48), 0, 0)
            })
            return
        }

        val totalMs = stats.sumOf { it.totalTimeInForeground }

        container.addView(TextView(this).apply {
            text = "Total screen time today: ${formatDuration(totalMs)}"
            textSize = 14f; setTextColor(0xFF9CA3AF.toInt())
            setPadding(0, 0, 0, dp(20))
        })

        stats.forEach { stat ->
            val appName = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(stat.packageName, 0)
                ).toString()
            } catch (_: Exception) { stat.packageName }

            val percent = (stat.totalTimeInForeground * 100f / totalMs).toInt()
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(0xFF111827.toInt()); cornerRadius = dp(12).toFloat()
                }
                setPadding(dp(20), dp(14), dp(20), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, dp(8)) }
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(TextView(this).apply {
                text = appName; textSize = 14f; setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            topRow.addView(TextView(this).apply {
                text = formatDuration(stat.totalTimeInForeground)
                textSize = 13f; setTextColor(0xFF6B7280.toInt())
            })

            val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100; progress = percent
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(4)
                ).also { it.setMargins(0, dp(8), 0, 0) }
                progressDrawable = GradientDrawable().apply {
                    setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(2).toFloat()
                }
            }

            card.addView(topRow)
            card.addView(bar)
            container.addView(card)
        }
    }

    private fun formatDuration(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
}
