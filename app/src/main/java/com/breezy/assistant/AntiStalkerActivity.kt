package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class AntiStalkerActivity : BaseActivity() {

    private lateinit var scanner: AntiStalkerScanner
    private lateinit var resultsContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var scanBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = AntiStalkerScanner(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(buildHeader("🕵️ Stalkerware Scan") { finish() })

        statusText = TextView(this).apply {
            text = "Tap scan to check for hidden monitoring apps"
            textSize = 13f
            setTextColor(0xFF6B7280.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }
        root.addView(statusText)

        scanBtn = TextView(this).apply {
            text = "▶  Start Scan"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt())
                cornerRadius = dp(14).toFloat()
            }
            setPadding(0, dp(18), 0, dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(dp(24), dp(8), dp(24), dp(16)) }
            setOnClickListener { runScan() }
        }
        root.addView(scanBtn)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        resultsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), 0, dp(24), dp(32))
        }
        scroll.addView(resultsContainer)
        root.addView(scroll)

        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun runScan() {
        scanBtn.isEnabled = false
        scanBtn.text = "Scanning..."
        statusText.text = "Checking installed apps..."
        resultsContainer.removeAllViews()

        // Add progress indicator
        val progress = android.widget.ProgressBar(this).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.gravity = android.view.Gravity.CENTER
            }
        }
        resultsContainer.addView(progress)

        Thread {
            try {
                val results = scanner.scanForThreats()
                runOnUiThread {
                    resultsContainer.removeAllViews()
                    scanBtn.isEnabled = true
                    scanBtn.text = "▶  Scan Again"
                    if (results.isEmpty()) {
                        statusText.text = "✅ No threats found."
                        resultsContainer.addView(buildCleanCard())
                    } else {
                        statusText.text = "⚠️ Found ${results.size} suspicious apps"
                        results.forEach { resultsContainer.addView(buildResultCard(it)) }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    scanBtn.isEnabled = true
                    scanBtn.text = "▶  Try Again"
                    statusText.text = "Scan failed: ${e.message}"
                }
            }
        }.start()
    }

    private fun buildCleanCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            setColor(0xFF0A1F0A.toInt()); cornerRadius = dp(16).toFloat()
        }
        setPadding(dp(24), dp(32), dp(24), dp(32))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        addView(TextView(this@AntiStalkerActivity).apply {
            text = "✅"; textSize = 48f; gravity = Gravity.CENTER
        })
        addView(TextView(this@AntiStalkerActivity).apply {
            text = "No stalkerware detected"
            textSize = 18f; setTextColor(0xFF6EE7B7.toInt())
            gravity = Gravity.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, dp(8))
        })
        addView(TextView(this@AntiStalkerActivity).apply {
            text = "Breezy found no suspicious apps. Stay safe."
            textSize = 13f; setTextColor(0xFF9CA3AF.toInt()); gravity = Gravity.CENTER
        })
    }

    private fun buildResultCard(result: AntiStalkerScanner.ScanResult) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val isHigh = result.riskScore >= 60
        background = GradientDrawable().apply {
            setColor(if (isHigh) 0xFF1F0A0A.toInt() else 0xFF1A1810.toInt())
            cornerRadius = dp(14).toFloat()
            if (isHigh) setStroke(1, 0xFFEF4444.toInt())
        }
        setPadding(dp(20), dp(16), dp(20), dp(16))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.setMargins(0, 0, 0, dp(12)) }

        val topRow = LinearLayout(this@AntiStalkerActivity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }

        try {
            topRow.addView(android.widget.ImageView(this@AntiStalkerActivity).apply {
                setImageDrawable(packageManager.getApplicationIcon(result.packageName))
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).also {
                    it.setMargins(0, 0, dp(12), 0)
                }
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            })
        } catch (_: Exception) {}

        val nameCol = LinearLayout(this@AntiStalkerActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        nameCol.addView(TextView(this@AntiStalkerActivity).apply {
            text = result.appName; setTextColor(Color.WHITE)
            textSize = 14f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        nameCol.addView(TextView(this@AntiStalkerActivity).apply {
            text = result.packageName; setTextColor(0xFF4B5563.toInt()); textSize = 10f
        })
        topRow.addView(nameCol)

        topRow.addView(TextView(this@AntiStalkerActivity).apply {
            text = "Risk: ${result.riskScore}"
            textSize = 12f
            setTextColor(if (isHigh) 0xFFFCA5A5.toInt() else 0xFFFCD34D.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(0xFF1A2235.toInt()); cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
        })

        addView(topRow)

        result.reasons.forEach { reason ->
            addView(TextView(this@AntiStalkerActivity).apply {
                text = "• $reason"; setTextColor(0xFF9CA3AF.toInt())
                textSize = 12f; setPadding(0, dp(4), 0, 0)
            })
        }

        if (result.isSystem) {
            addView(TextView(this@AntiStalkerActivity).apply {
                text = "ℹ️ System app — likely safe"
                setTextColor(0xFF6B7280.toInt()); textSize = 11f; setPadding(0, dp(8), 0, 0)
            })
        }
    }
}
