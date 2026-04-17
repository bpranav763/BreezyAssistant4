package com.breezy.assistant

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class AppNoteActivity : BaseActivity() {

   private lateinit var searchInput: EditText
   private lateinit var appListContainer: LinearLayout
   private val memory by lazy { BreezyMemory(this) }
   private val pm by lazy { packageManager }
   private var allApps = listOf<ApplicationInfo>()

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

       root.addView(buildHeader("📱 Startup Notes") { finish() })

       if (!isAccessibilityServiceEnabled()) {
           showAccessibilityPrompt(root)
       }

       root.addView(buildSearchSection())

       val scroll = ScrollView(this).apply {
           layoutParams = LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
           isFillViewport = true
       }
       appListContainer = LinearLayout(this).apply {
           orientation = LinearLayout.VERTICAL
           setPadding(dp(24), dp(8), dp(24), dp(40))
       }
       scroll.addView(appListContainer)
       root.addView(scroll)

       setContentView(root)
       applySystemBarInsets(root)

       loadAllApps()
   }

   private fun buildSearchSection(): LinearLayout {
       return LinearLayout(this).apply {
           orientation = LinearLayout.VERTICAL
           setPadding(dp(24), dp(8), dp(24), dp(16))
          
           searchInput = EditText(this@AppNoteActivity).apply {
               hint = "Search apps..."
               setHintTextColor(0xFF6B7280.toInt())
               setTextColor(Color.WHITE)
               textSize = 14f
               setPadding(dp(32), dp(24), dp(32), dp(24))
               background = GradientDrawable().apply {
                   setColor(0xFF111827.toInt())
                   cornerRadius = dp(24).toFloat()
               }
               addTextChangedListener(object : android.text.TextWatcher {
                   override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                   override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                       filterApps(s.toString())
                   }
                   override fun afterTextChanged(s: android.text.Editable?) {}
               })
           }
           addView(searchInput)
       }
   }

   private fun loadAllApps() {
       Thread {
           val intent = Intent(Intent.ACTION_MAIN, null).apply {
               addCategory(Intent.CATEGORY_LAUNCHER)
           }
           allApps = pm.queryIntentActivities(intent, 0)
               .map { it.activityInfo.applicationInfo }
               .distinctBy { it.packageName }
               .filter { it.packageName != packageName }
               .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

           runOnUiThread { renderApps(allApps) }
       }.start()
   }

   private fun filterApps(query: String) {
       val filtered = allApps.filter {
           pm.getApplicationLabel(it).toString().contains(query, ignoreCase = true)
       }
       renderApps(filtered)
   }

   private fun renderApps(apps: List<ApplicationInfo>) {
       appListContainer.removeAllViews()
       if (apps.isEmpty()) {
           appListContainer.addView(TextView(this).apply {
               text = "No apps found"
               setTextColor(0xFF6B7280.toInt())
               gravity = Gravity.CENTER
               setPadding(0, dp(100), 0, 0)
           })
           return
       }

       apps.forEach { app ->
           val name = pm.getApplicationLabel(app).toString()
           appListContainer.addView(buildAppCard(app, name))
       }
   }

   private fun buildAppCard(app: ApplicationInfo, name: String): View {
       val pkg = app.packageName
       val existingNote = memory.getFact("appnote_$pkg")
      
       val card = LinearLayout(this).apply {
           orientation = LinearLayout.VERTICAL
           background = GradientDrawable().apply {
               setColor(0xFF111827.toInt())
               cornerRadius = dp(32).toFloat()
           }
           setPadding(dp(32), dp(24), dp(32), dp(24))
           layoutParams = LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT,
               LinearLayout.LayoutParams.WRAP_CONTENT
           ).also { it.setMargins(0, 0, 0, dp(16)) }
       }

       val nameRow = LinearLayout(this).apply {
           orientation = LinearLayout.HORIZONTAL
           gravity = Gravity.CENTER_VERTICAL
       }

       // Icon loading
       try {
           val icon = pm.getApplicationIcon(app.packageName)
           nameRow.addView(ImageView(this).apply {
               setImageDrawable(icon)
               layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).also {
                   it.setMargins(0, 0, dp(14), 0)
               }
               scaleType = ImageView.ScaleType.FIT_CENTER
           })
       } catch (_: Exception) {
           nameRow.addView(TextView(this).apply {
               text = "📱"
               textSize = 20f
               setPadding(0, 0, dp(14), 0)
           })
       }

       nameRow.addView(TextView(this).apply {
           text = name
           setTextColor(Color.WHITE)
           textSize = 14f
           typeface = android.graphics.Typeface.DEFAULT_BOLD
           layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
       })

       val status = TextView(this).apply {
           text = if (existingNote.isNotEmpty()) "Active" else "Empty"
           setTextColor(if (existingNote.isNotEmpty()) 0xFF10B981.toInt() else 0xFF6B7280.toInt())
           textSize = 10f
           setPadding(dp(16), dp(4), dp(16), dp(4))
           background = GradientDrawable().apply {
               setColor(0xFF1A2235.toInt())
               cornerRadius = dp(12).toFloat()
           }
       }
       nameRow.addView(status)
       card.addView(nameRow)

       val noteInput = EditText(this).apply {
           setText(existingNote)
           hint = "Startup note..."
           setHintTextColor(0xFF4B5563.toInt())
           setTextColor(Color.WHITE)
           textSize = 13f
           background = null
           setPadding(0, dp(16), 0, dp(16))
       }
       card.addView(noteInput)

       val actions = LinearLayout(this).apply {
           orientation = LinearLayout.HORIZONTAL
           gravity = Gravity.END
       }

       val saveBtn = TextView(this).apply {
           text = "Save"
           setTextColor(0xFF38BDF8.toInt())
           textSize = 13f
           typeface = android.graphics.Typeface.DEFAULT_BOLD
           setPadding(dp(32), dp(16), dp(32), dp(16))
           isClickable = true
           setOnClickListener {
               val note = noteInput.text.toString().trim()
               if (note.isEmpty()) {
                   memory.deleteFact("appnote_$pkg")
                   status.text = "Empty"
                   status.setTextColor(0xFF6B7280.toInt())
               } else {
                   memory.saveFact("appnote_$pkg", note)
                   status.text = "Active"
                   status.setTextColor(0xFF10B981.toInt())
               }
               Toast.makeText(this@AppNoteActivity, "Saved", Toast.LENGTH_SHORT).show()
           }
       }
       actions.addView(saveBtn)
       card.addView(actions)

       return card
   }

   private fun isAccessibilityServiceEnabled(): Boolean {
       val enabledServices = android.provider.Settings.Secure.getString(
           contentResolver,
           android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
       ) ?: return false
       return enabledServices.contains(packageName)
   }

   private fun showAccessibilityPrompt(root: LinearLayout) {
       val banner = TextView(this).apply {
           text = "⚠️ Enable Breezy in Accessibility Settings for startup notes to appear"
           textSize = 12f
           setTextColor(android.graphics.Color.WHITE)
           setBackgroundColor(0xFF1D4ED8.toInt())
           setPadding(dp(16), dp(12), dp(16), dp(12))
           gravity = android.view.Gravity.CENTER
           setOnClickListener {
               startActivity(android.content.Intent(
                   android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
               ))
           }
       }
       // Insert at position 1 (after header)
       root.addView(banner, 1)
   }
}
