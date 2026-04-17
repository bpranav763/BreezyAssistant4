package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.text.DecimalFormat
import kotlin.concurrent.thread

class StorageAnalysisActivity : BaseActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var resultsContainer: LinearLayout

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

        root.addView(buildHeader("🧹 Storage Analyzer") { finish() })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(32))
        }

        statusText = TextView(this).apply {
            text = "Analyzing storage..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(16))
        }
        container.addView(statusText)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(4)
            ).also { it.setMargins(0, 0, 0, dp(24)) }
        }
        container.addView(progressBar)

        resultsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(resultsContainer)

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)

        startAnalysis()
    }

    private fun startAnalysis() {
        thread {
            val rootFile = Environment.getExternalStorageDirectory()
            val largeFiles = mutableListOf<File>()
            
            scanDirectory(rootFile, largeFiles)
            
            val sortedFiles = largeFiles.sortedByDescending { it.length() }.take(20)

            runOnUiThread {
                statusText.text = "Analysis complete. Found ${largeFiles.size} large files."
                progressBar.visibility = android.view.View.GONE
                displayResults(sortedFiles)
            }
        }
    }

    private fun scanDirectory(dir: File, largeFiles: MutableList<File>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanDirectory(file, largeFiles)
            } else {
                if (file.length() > 10 * 1024 * 1024) { // > 10MB
                    largeFiles.add(file)
                }
            }
        }
    }

    private fun displayResults(files: List<File>) {
        resultsContainer.removeAllViews()
        
        if (files.isEmpty()) {
            resultsContainer.addView(TextView(this).apply {
                text = "No large files found."
                setTextColor(0xFF9CA3AF.toInt())
                textSize = 14f
            })
            return
        }

        for (file in files) {
            val fileItem = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(0xFF111827.toInt())
                    cornerRadius = dp(12).toFloat()
                }
                setPadding(dp(16), dp(16), dp(16), dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, dp(12)) }
            }

            fileItem.addView(TextView(this).apply {
                text = file.name
                textSize = 15f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })

            fileItem.addView(TextView(this).apply {
                text = "${formatSize(file.length())} • ${file.parent}"
                textSize = 12f
                setTextColor(0xFF6B7280.toInt())
                setPadding(0, dp(4), 0, 0)
            })

            resultsContainer.addView(fileItem)
        }
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
