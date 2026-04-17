package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlinx.coroutines.*
import java.net.URL
import kotlin.system.measureTimeMillis

class SpeedTestActivity : BaseActivity() {

    private lateinit var speedText: TextView
    private lateinit var progressText: TextView
    private lateinit var startBtn: TextView
    private lateinit var progressBar: ProgressBar
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        root.addView(buildHeader("⚡ Speed Test") { finish() })

        val mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            setPadding(dp(32), 0, dp(32), 0)
        }

        speedText = TextView(this).apply {
            text = "0.0"
            textSize = 64f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        mainContent.addView(speedText)

        mainContent.addView(TextView(this).apply {
            text = "Mbps"
            textSize = 18f
            setTextColor(0xFF4B5563.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(32))
        })

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(-1, dp(4)).also { it.setMargins(0, 0, 0, dp(16)) }
            visibility = View.INVISIBLE
        }
        mainContent.addView(progressBar)

        progressText = TextView(this).apply {
            text = "Ready to test your connection"
            textSize = 13f
            setTextColor(0xFF9CA3AF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(48))
        }
        mainContent.addView(progressText)

        startBtn = TextView(this).apply {
            text = "Run Speed Test"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(14).toFloat()
            }
            setPadding(0, dp(18), 0, dp(18))
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            setOnClickListener { runTest() }
        }
        mainContent.addView(startBtn)

        root.addView(mainContent)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun runTest() {
        startBtn.isEnabled = false
        startBtn.alpha = 0.5f
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        speedText.text = "..."
        progressText.text = "Finding server..."

        scope.launch {
            try {
                // Testing with a small file first for latency/ramp up
                val testUrl = "https://speed.cloudflare.com/__down?bytes=10000000" // 10MB
                val totalExpected = 10000000
                var totalBytes = 0
                val startTime = System.currentTimeMillis()

                withContext(Dispatchers.IO) {
                    val url = URL(testUrl)
                    val connection = url.openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.connect()
                    val inputStream = connection.getInputStream()
                    val buffer = ByteArray(65536) // Larger buffer for speed
                    var bytesRead: Int
                    
                    var lastUpdate = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                        val now = System.currentTimeMillis()
                        
                        // Update UI every 100ms to avoid flooding main thread
                        if (now - lastUpdate > 100) {
                            val durationSeconds = (now - startTime) / 1000.0
                            if (durationSeconds > 0) {
                                val mbps = (totalBytes * 8.0 / (1024.0 * 1024.0)) / durationSeconds
                                val progress = ((totalBytes.toDouble() / totalExpected) * 100).toInt()
                                
                                withContext(Dispatchers.Main) {
                                    speedText.text = String.format("%.1f", mbps)
                                    progressBar.progress = minOf(100, progress)
                                    progressText.text = "Downloading test file... ${minOf(100, progress)}%"
                                }
                            }
                            lastUpdate = now
                        }
                        if (now - startTime > 10000) break // 10 second timeout for better UX
                    }
                    inputStream.close()
                }

                val finalDuration = (System.currentTimeMillis() - startTime) / 1000.0
                val finalMbps = (totalBytes * 8.0 / (1024 * 1024)) / finalDuration
                
                speedText.text = String.format("%.1f", finalMbps)
                progressBar.progress = 100
                progressText.text = "Test complete. (${(totalBytes / (1024 * 1024))} MB downloaded)"
                BreezyMemory(this@SpeedTestActivity).saveFact("last_speed_test", String.format("%.1f Mbps", finalMbps))

            } catch (e: Exception) {
                progressText.text = "Error: ${e.message}"
                speedText.text = "0.0"
            } finally {
                startBtn.isEnabled = true
                startBtn.alpha = 1.0f
                progressBar.visibility = View.INVISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
