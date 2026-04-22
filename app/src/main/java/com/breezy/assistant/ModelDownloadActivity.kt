package com.breezy.assistant

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import kotlinx.coroutines.*
import java.io.File

class ModelDownloadActivity : BaseActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var downloadBtn: TextView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Breezy Brain v1.0.0 - Local LLM Model
    private val MODEL_URL = LLMInference.MODEL_URL
    private val MODEL_FILENAME = LLMInference.MODEL_FILENAME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(32), 0, dp(32), 0)
        }

        root.addView(TextView(this).apply {
            text = "🧠"
            textSize = 64f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        })

        root.addView(TextView(this).apply {
            text = "Breezy AI Brain"
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        })

        root.addView(TextView(this).apply {
            text = "Download the on-device AI model (~90MB)\nNo data leaves your phone. Ever."
            textSize = 13f
            setTextColor(0xFF6B7280.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(32))
        })

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(8)
            ).also { it.setMargins(0, 0, 0, dp(16)) }
            visibility = android.view.View.GONE
        }
        root.addView(progressBar)

        statusText = TextView(this).apply {
            text = "AI model not downloaded yet"
            textSize = 13f
            setTextColor(0xFF6B7280.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        }
        root.addView(statusText)

        // Check if model already exists
        val modelFile = File(getExternalFilesDir(null), MODEL_FILENAME)
        if (modelFile.exists()) {
            statusText.text = "✅ AI Brain ready (${modelFile.length() / (1024 * 1024)}MB)"
        }

        downloadBtn = TextView(this).apply {
            text = if (modelFile.exists()) "✅ Already Downloaded" else "⬇️  Download AI Brain"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(if (modelFile.exists()) 0xFF065F46.toInt() else 0xFF1D4ED8.toInt())
                cornerRadius = dp(14).toFloat()
            }
            setPadding(0, dp(18), 0, dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dp(16)) }
            setOnClickListener {
                if (!modelFile.exists()) downloadModel()
            }
        }
        root.addView(downloadBtn)

        root.addView(TextView(this).apply {
            text = "← Skip for now"
            textSize = 13f
            setTextColor(0xFF4B5563.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
            setOnClickListener { finish() }
        })

        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun downloadModel() {
        val modelFile = File(getExternalFilesDir(null), MODEL_FILENAME)
        downloadBtn.isEnabled = false
        downloadBtn.text = "Starting..."
        progressBar.visibility = android.view.View.VISIBLE

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val url = java.net.URL(MODEL_URL)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connect()
                    
                    if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                        throw Exception("Server returned HTTP ${connection.responseCode}")
                    }

                    val fileLength = connection.contentLength
                    val input = connection.inputStream
                    val output = java.io.FileOutputStream(modelFile)

                    val data = ByteArray(8192)
                    var total = 0L
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            val progress = (total * 100 / fileLength).toInt()
                            withContext(Dispatchers.Main) {
                                progressBar.progress = progress
                                statusText.text = "Downloading... $progress%"
                            }
                        }
                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()
                }

                // Verify file size
                if (modelFile.length() < 80 * 1024 * 1024) {
                    throw Exception("Download incomplete (file too small)")
                }

                statusText.text = "✅ AI Brain ready!"
                downloadBtn.text = "✅ Download Complete"
                downloadBtn.background = GradientDrawable().apply {
                    setColor(0xFF065F46.toInt()); cornerRadius = dp(14).toFloat()
                }
                BreezyMemory(this@ModelDownloadActivity).saveFact("ai_model_ready", "true")
                
                // Refresh LLM
                LLMInference(this@ModelDownloadActivity).ensureLoaded()

            } catch (e: Exception) {
                statusText.text = "❌ Download failed: ${e.message}"
                downloadBtn.isEnabled = true
                downloadBtn.text = "⬇️  Retry Download"
                if (modelFile.exists()) modelFile.delete()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
