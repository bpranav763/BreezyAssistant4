package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ModelDownloadActivity : BaseActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var downloadBtn: TextView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Replace with your actual GitHub release URL when ready
    private val MODEL_URL = "https://github.com/yourusername/breezy-assistant/releases/download/v1.0/mobilellm-125m-q4.gguf"
    private val MODEL_FILENAME = "breezy_brain.gguf"

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
            text = "Download the on-device AI model (~60MB)\nNo data leaves your phone. Ever."
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
        val modelFile = File(filesDir, MODEL_FILENAME)
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
        downloadBtn.isEnabled = false
        downloadBtn.text = "Downloading..."
        progressBar.visibility = android.view.View.VISIBLE

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val url = URL(MODEL_URL)
                    val connection = url.openConnection()
                    connection.connect()
                    val totalSize = connection.contentLength
                    val modelFile = File(filesDir, MODEL_FILENAME)

                    connection.getInputStream().use { input ->
                        FileOutputStream(modelFile).use { output ->
                            val buffer = ByteArray(8192)
                            var downloaded = 0L
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                val percent = if (totalSize > 0)
                                    ((downloaded * 100) / totalSize).toInt() else 0
                                withContext(Dispatchers.Main) {
                                    progressBar.progress = percent
                                    statusText.text = "Downloading... $percent% (${downloaded / (1024 * 1024)}MB)"
                                }
                            }
                        }
                    }
                }
                progressBar.progress = 100
                statusText.text = "✅ AI Brain downloaded and ready!"
                downloadBtn.text = "✅ Download Complete"
                downloadBtn.background = GradientDrawable().apply {
                    setColor(0xFF065F46.toInt()); cornerRadius = dp(14).toFloat()
                }
                BreezyMemory(this@ModelDownloadActivity).saveFact("ai_model_ready", "true")
            } catch (e: Exception) {
                statusText.text = "❌ Download failed. Check connection.\n${e.message}"
                downloadBtn.isEnabled = true
                downloadBtn.text = "⬇️  Try Again"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
