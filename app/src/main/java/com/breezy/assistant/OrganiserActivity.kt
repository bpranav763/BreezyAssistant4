package com.breezy.assistant

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.*
import java.io.File

class OrganiserActivity : BaseActivity() {

    private lateinit var container: LinearLayout
    private val pm by lazy { packageManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@OrganiserActivity))
        }
        root.addView(buildHeader("📁 Organiser") { finish() })

        val scroll = ScrollView(this).apply { 
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }
        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)

        loadAppCategories()
        loadFileCategories()
    }

    private fun loadAppCategories() {
        container.addView(sectionHeader("📱 APPS BY CATEGORY"))
        val categories = mutableMapOf<String, MutableList<String>>()
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in apps) {
            // Only include non-system apps or important system apps
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || app.packageName.contains("google")) {
                val category = categoriseApp(app)
                val name = pm.getApplicationLabel(app).toString()
                categories.getOrPut(category) { mutableListOf() }.add(name)
            }
        }
        categories.forEach { (cat, apps) ->
            container.addView(categoryCard(cat, apps))
        }
    }

    private fun categoriseApp(app: ApplicationInfo): String {
        val pkg = app.packageName.lowercase()
        return when {
            pkg.contains("facebook") || pkg.contains("whatsapp") || 
            pkg.contains("instagram") || pkg.contains("telegram") ||
            pkg.contains("messenger") || pkg.contains("twitter") ||
            pkg.contains("snapchat") || pkg.contains("tiktok") -> "Social"
            
            pkg.contains("game") || pkg.contains("play") || 
            pkg.contains("steam") || pkg.contains("epic") -> "Games"
            
            pkg.contains("camera") || pkg.contains("photo") || 
            pkg.contains("gallery") || pkg.contains("snapseed") ||
            pkg.contains("lightroom") -> "Photography"
            
            pkg.contains("mail") || pkg.contains("calendar") || 
            pkg.contains("drive") || pkg.contains("docs") ||
            pkg.contains("sheet") || pkg.contains("notes") ||
            pkg.contains("office") || pkg.contains("slack") ||
            pkg.contains("notion") -> "Productivity"
            
            pkg.contains("music") || pkg.contains("video") || 
            pkg.contains("youtube") || pkg.contains("netflix") ||
            pkg.contains("spotify") || pkg.contains("disney") ||
            pkg.contains("prime") -> "Entertainment"
            
            pkg.contains("bank") || pkg.contains("pay") ||
            pkg.contains("wallet") || pkg.contains("crypto") -> "Finance"
            
            pkg.contains("shop") || pkg.contains("amazon") ||
            pkg.contains("ebay") || pkg.contains("flipkart") -> "Shopping"
            
            else -> "Other"
        }
    }

    private fun categoryCard(category: String, apps: List<String>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@OrganiserActivity))
                cornerRadius = ThemeManager.getCornerRadius(this@OrganiserActivity).dp(context).toFloat()
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }

            addView(TextView(context).apply {
                text = "$category (${apps.size})"
                textSize = 15f
                setTextColor(ThemeManager.getAccentColor(context))
                typeface = Typeface.DEFAULT_BOLD
            })
            apps.take(5).forEach { app ->
                addView(TextView(context).apply {
                    text = "• $app"
                    textSize = 13f
                    setTextColor(ThemeManager.getTextSecondary(context))
                    setPadding(0, dp(4), 0, 0)
                })
            }
            if (apps.size > 5) {
                addView(TextView(context).apply {
                    text = "... and ${apps.size - 5} more"
                    textSize = 12f
                    setTextColor(ThemeManager.getTextSecondary(context))
                    setPadding(0, dp(4), 0, 0)
                })
            }
        }
    }

    private fun loadFileCategories() {
        container.addView(sectionHeader("📂 FILES"))
        val storage = Environment.getExternalStorageDirectory()
        val fileCategories = mapOf(
            "Images" to listOf(".jpg", ".png", ".gif", ".jpeg", ".webp"),
            "Videos" to listOf(".mp4", ".mkv", ".avi", ".mov", ".3gp"),
            "Documents" to listOf(".pdf", ".doc", ".docx", ".txt", ".xls", ".xlsx", ".ppt", ".pptx"),
            "Audio" to listOf(".mp3", ".wav", ".flac", ".m4a", ".ogg"),
            "Archives" to listOf(".zip", ".rar", ".7z", ".tar", ".gz")
        )
        fileCategories.forEach { (cat, exts) ->
            val count = countFiles(storage, exts)
            container.addView(fileRow(cat, count))
        }
    }

    private fun countFiles(dir: File, extensions: List<String>): Int {
        if (!dir.exists()) return 0
        var count = 0
        val files = dir.listFiles()
        files?.forEach { file ->
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    count += countFiles(file, extensions)
                }
            } else if (extensions.any { file.name.endsWith(it, ignoreCase = true) }) {
                count++
            }
        }
        return count
    }

    private fun fileRow(label: String, count: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            addView(TextView(context).apply {
                text = label
                textSize = 14f
                setTextColor(ThemeManager.getTextPrimary(context))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            addView(TextView(context).apply {
                text = "$count files"
                textSize = 13f
                setTextColor(ThemeManager.getAccentColor(context))
            })
        }
    }

    private fun sectionHeader(text: String) = TextView(this).apply {
        this.text = text
        textSize = 11f
        setTextColor(ThemeManager.getTextSecondary(this@OrganiserActivity))
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.15f
        setPadding(0, dp(16), 0, dp(8))
    }
}
