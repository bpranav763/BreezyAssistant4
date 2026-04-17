package com.breezy.assistant

import android.content.ContentValues
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import net.sqlcipher.database.SQLiteDatabase

class VaultActivity : BaseActivity() {

   private var db: SQLiteDatabase? = null
   private lateinit var listLayout: LinearLayout
   private val PASSPHRASE = "breezy_vault_2024_secure"

   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       SQLiteDatabase.loadLibs(this)

       val root = LinearLayout(this).apply {
           orientation = LinearLayout.VERTICAL
           setBackgroundColor(0xFF0A0F1E.toInt())
           layoutParams = LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT,
               LinearLayout.LayoutParams.MATCH_PARENT
           )
       }

       // Header
       root.addView(LinearLayout(this).apply {
           orientation = LinearLayout.HORIZONTAL
           gravity = Gravity.CENTER_VERTICAL
           setPadding(dp(24), 0, dp(24), dp(16))

           addView(TextView(this@VaultActivity).apply {
               text = "←"
               textSize = 22f
               setTextColor(Color.WHITE)
               setPadding(0, 0, dp(20), 0)
               setOnClickListener { finish() }
           })
           addView(LinearLayout(this@VaultActivity).apply {
               orientation = LinearLayout.VERTICAL
               layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
               addView(TextView(this@VaultActivity).apply {
                   text = "🔐 Security Vault"
                   textSize = 20f
                   setTextColor(Color.WHITE)
                   typeface = android.graphics.Typeface.DEFAULT_BOLD
               })
               addView(TextView(this@VaultActivity).apply {
                   text = "Biometric • AES-256 encrypted"
                   textSize = 11f
                   setTextColor(0xFF6B7280.toInt())
               })
           })
       })

       // Add form
       val form = LinearLayout(this).apply {
           orientation = LinearLayout.VERTICAL
           setBackgroundColor(0xFF111827.toInt())
           setPadding(dp(24), dp(20), dp(24), dp(20))
           layoutParams = LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT,
               LinearLayout.LayoutParams.WRAP_CONTENT
           ).also { it.setMargins(dp(24), 0, dp(24), dp(20)) }
       }

       val serviceInput = buildInput("App / Website name (e.g. Instagram)")
       val urlInput = buildInput("URL for autofill (e.g. instagram.com)")
       val usernameInput = buildInput("Username or Email")
       val passwordInput = buildInput("Password").apply {
           inputType = android.text.InputType.TYPE_CLASS_TEXT or
                   android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
       }

       form.addView(buildLabel("Service"))
       form.addView(serviceInput)
       form.addView(buildLabel("URL (for autofill)"))
       form.addView(urlInput)
       form.addView(buildLabel("Username"))
       form.addView(usernameInput)
       form.addView(buildLabel("Password"))
       form.addView(passwordInput)
       form.addView(TextView(this).apply {
           text = "⚠️ Autofill: Enable Breezy in Settings → Accessibility → Autofill service"
           textSize = 11f
           setTextColor(0xFF6B7280.toInt())
           setPadding(0, dp(8), 0, dp(12))
       })

       val saveBtn = TextView(this).apply {
           text = "+ Save Credentials"
           textSize = 14f
           typeface = android.graphics.Typeface.DEFAULT_BOLD
           setTextColor(Color.WHITE)
           gravity = Gravity.CENTER
           background = android.graphics.drawable.GradientDrawable().apply {
               setColor(0xFF1D4ED8.toInt())
               cornerRadius = dp(8).toFloat()
           }
           setPadding(0, dp(14), 0, dp(14))
           layoutParams = LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT,
               LinearLayout.LayoutParams.WRAP_CONTENT
           )
           setOnClickListener {
               val svc = serviceInput.text.toString().trim()
               val url = urlInput.text.toString().trim()
               val user = usernameInput.text.toString().trim()
               val pass = passwordInput.text.toString().trim()
               if (svc.isEmpty() || pass.isEmpty()) {
                   Toast.makeText(this@VaultActivity, "Service and password required", Toast.LENGTH_SHORT).show()
                   return@setOnClickListener
               }
               db?.let { safeDb ->
                   safeDb.insert("passwords", null, ContentValues().apply {
                       put("service", svc)
                       put("url", url)
                       put("username", user)
                       put("password", pass)
                   })
                   serviceInput.setText(""); urlInput.setText("")
                   usernameInput.setText(""); passwordInput.setText("")
                   refreshList()
                   Toast.makeText(this@VaultActivity, "Saved securely.", Toast.LENGTH_SHORT).show()
               }
           }
       }
       form.addView(saveBtn)
       root.addView(form)

       root.addView(buildSectionLabel("SAVED PASSWORDS"))

       val scroll = ScrollView(this).apply {
           layoutParams = LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
       }
       listLayout = LinearLayout(this).apply {
           orientation = LinearLayout.VERTICAL
           setPadding(dp(24), 0, dp(24), dp(32))
       }
       scroll.addView(listLayout)
       root.addView(scroll)

       setContentView(root)
       applySystemBarInsets(root)
       authenticate()
   }

   private fun buildInput(hint: String) = EditText(this).apply {
       this.hint = hint
       setHintTextColor(0xFF4B5563.toInt())
       setTextColor(Color.WHITE)
       setBackgroundColor(0xFF1A2235.toInt())
       setPadding(dp(16), dp(12), dp(16), dp(12))
       textSize = 14f
       layoutParams = LinearLayout.LayoutParams(
           LinearLayout.LayoutParams.MATCH_PARENT,
           LinearLayout.LayoutParams.WRAP_CONTENT
       ).also { it.setMargins(0, 0, 0, dp(10)) }
   }

   private fun buildLabel(text: String) = TextView(this).apply {
       this.text = text
       textSize = 11f
       setTextColor(0xFF6B7280.toInt())
       setPadding(0, 0, 0, dp(4))
   }

   private fun buildSectionLabel(text: String) = TextView(this).apply {
       this.text = text
       textSize = 11f
       setTextColor(0xFF4B5563.toInt())
       letterSpacing = 0.15f
       setPadding(dp(32), dp(16), dp(32), dp(12))
   }

   private fun authenticate() {
       val executor = ContextCompat.getMainExecutor(this)
       val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
           override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
               openDatabase()
           }
           override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
               Toast.makeText(this@VaultActivity, "Auth error: $errString", Toast.LENGTH_SHORT).show()
               if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                   finish()
               }
           }
           override fun onAuthenticationFailed() {
               Toast.makeText(this@VaultActivity, "Auth failed", Toast.LENGTH_SHORT).show()
           }
       })

       val promptInfo = BiometricPrompt.PromptInfo.Builder()
           .setTitle("Unlock Breezy Vault")
           .setSubtitle("Use fingerprint or face")
           .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
           .build()

       biometricPrompt.authenticate(promptInfo)
   }

   private fun openDatabase() {
       val dbFile = getDatabasePath("breezy_vault.db")
       db = SQLiteDatabase.openOrCreateDatabase(dbFile, PASSPHRASE, null)
       db?.execSQL("""
           CREATE TABLE IF NOT EXISTS passwords (
               id INTEGER PRIMARY KEY AUTOINCREMENT,
               service TEXT, url TEXT,
               username TEXT, password TEXT
           )
       """)
       // Migrate: add url column if upgrading from old schema
       try { db?.execSQL("ALTER TABLE passwords ADD COLUMN url TEXT DEFAULT ''") } catch (_: Exception) {}
       refreshList()
   }

   private fun refreshList() {
       listLayout.removeAllViews()
       val safeDb = db ?: return
       val cursor = safeDb.rawQuery("SELECT * FROM passwords ORDER BY service ASC", null)

       if (cursor.count == 0) {
           listLayout.addView(TextView(this).apply {
               text = "No passwords saved yet."
               setTextColor(0xFF6B7280.toInt())
               gravity = Gravity.CENTER
               setPadding(0, dp(32), 0, 0)
           })
           cursor.close()
           return
       }

       while (cursor.moveToNext()) {
           val id = cursor.getInt(0)
           val service = cursor.getString(1) ?: ""
           val url = cursor.getString(2) ?: ""
           val username = cursor.getString(3) ?: ""
           val password = cursor.getString(4) ?: ""
           var visible = false

           val card = LinearLayout(this).apply {
               orientation = LinearLayout.VERTICAL
               background = android.graphics.drawable.GradientDrawable().apply {
                   setColor(0xFF111827.toInt())
                   cornerRadius = dp(16).toFloat()
               }
               setPadding(dp(20), dp(20), dp(20), dp(20))
               layoutParams = LinearLayout.LayoutParams(
                   LinearLayout.LayoutParams.MATCH_PARENT,
                   LinearLayout.LayoutParams.WRAP_CONTENT
               ).also { it.setMargins(dp(24), 0, dp(24), dp(16)) }
           }

           // Service + URL
           card.addView(TextView(this).apply {
               text = service
               setTextColor(Color.WHITE)
               textSize = 15f
               typeface = android.graphics.Typeface.DEFAULT_BOLD
           })
           if (url.isNotEmpty()) {
               card.addView(TextView(this).apply {
                   text = "🔗 $url"
                   setTextColor(0xFF6B7280.toInt())
                   textSize = 11f
                   setPadding(0, dp(2), 0, dp(6))
               })
           }
           if (username.isNotEmpty()) {
               card.addView(TextView(this).apply {
                   text = username
                   setTextColor(0xFF9CA3AF.toInt())
                   textSize = 12f
               })
           }

           // Password row
           val passRow = LinearLayout(this).apply {
               orientation = LinearLayout.HORIZONTAL
               gravity = Gravity.CENTER_VERTICAL
               setPadding(0, dp(8), 0, 0)
           }
           val passText = TextView(this).apply {
               text = "••••••••"
               setTextColor(0xFF1D4ED8.toInt())
               textSize = 14f
               layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
           }
           val showBtn = TextView(this).apply {
               text = "👁"
               textSize = 16f
               setPadding(dp(8), 0, dp(8), 0)
               setOnClickListener {
                   visible = !visible
                   passText.text = if (visible) password else "••••••••"
               }
           }
           val copyBtn = TextView(this).apply {
               text = "📋"
               textSize = 16f
               setPadding(dp(8), 0, dp(8), 0)
               setOnClickListener {
                   val clip = getSystemService(android.content.ClipboardManager::class.java) as android.content.ClipboardManager
                   clip.setPrimaryClip(android.content.ClipData.newPlainText("password", password))
                   Toast.makeText(this@VaultActivity, "Password copied", Toast.LENGTH_SHORT).show()
               }
           }
           val delBtn = TextView(this).apply {
               text = "✕"
               textSize = 16f
               setTextColor(0xFFEF4444.toInt())
               setPadding(dp(8), 0, 0, 0)
               setOnClickListener {
                   safeDb.delete("passwords", "id=?", arrayOf(id.toString()))
                   refreshList()
               }
           }
           passRow.addView(passText)
           passRow.addView(showBtn)
           passRow.addView(copyBtn)
           passRow.addView(delBtn)
           card.addView(passRow)
           listLayout.addView(card)
       }
       cursor.close()
   }

   override fun onDestroy() {
       super.onDestroy()
       db?.close()
   }
}
