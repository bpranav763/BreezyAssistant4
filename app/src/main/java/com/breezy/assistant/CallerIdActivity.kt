package com.breezy.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.CallLog
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*

class CallerIdActivity : BaseActivity() {

    private val spamDb by lazy { SpamDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        root.addView(buildHeader("📞 Call Protection") { finish() })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(32))
        }

        // Status card
        container.addView(buildStatusCard())
        container.addView(buildDivider())

        // Check permission and load logs
        if (hasCallLogPermission()) {
            container.addView(buildSectionLabel("RECENT CALLS"))
            loadCallLog(container)
        } else {
            container.addView(buildPermissionCard())
        }

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
        applySystemBarInsets(root)
    }

    private fun hasCallLogPermission() =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) ==
                PackageManager.PERMISSION_GRANTED

    private fun buildStatusCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF0D1B2A.toInt()); cornerRadius = dp(16).toFloat()
                setStroke(1, 0xFF1D4ED8.toInt())
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(16)) }

            val headerRow = LinearLayout(this@CallerIdActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            }
            headerRow.addView(TextView(this@CallerIdActivity).apply {
                text = "Spam Shield"; textSize = 16f; setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            headerRow.addView(TextView(this@CallerIdActivity).apply {
                text = "Active"
                textSize = 12f; setTextColor(0xFF34D399.toInt())
                background = GradientDrawable().apply {
                    setColor(0xFF064E3B.toInt()); cornerRadius = dp(20).toFloat()
                }
                setPadding(dp(12), dp(4), dp(12), dp(4))
            })
            addView(headerRow)

            addView(TextView(this@CallerIdActivity).apply {
                text = "Offline caller ID — no data leaves your phone. Numbers checked against local database."
                textSize = 12f; setTextColor(0xFF6B7280.toInt())
                setPadding(0, dp(10), 0, 0); setLineSpacing(0f, 1.4f)
            })

            // Quick check row
            val checkRow = LinearLayout(this@CallerIdActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(16), 0, 0)
            }
            val checkInput = EditText(this@CallerIdActivity).apply {
                hint = "Check a number…"
                setHintTextColor(0xFF4B5563.toInt()); setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(0xFF111827.toInt()); cornerRadius = dp(10).toFloat()
                }
                setPadding(dp(14), dp(10), dp(14), dp(10))
                inputType = android.text.InputType.TYPE_CLASS_PHONE
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            val checkBtn = TextView(this@CallerIdActivity).apply {
                text = "Check"
                textSize = 13f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(10).toFloat()
                }
                setPadding(dp(18), dp(10), dp(18), dp(10))
                layoutParams = LinearLayout.LayoutParams(-2, -2).also { it.setMargins(dp(10), 0, 0, 0) }
                setOnClickListener {
                    val num = checkInput.text.toString().trim()
                    if (num.isNotEmpty()) {
                        val isSpam = spamDb.isSpam(num)
                        Toast.makeText(
                            this@CallerIdActivity,
                            if (isSpam) "⚠️ Likely spam/fraud" else "✅ Not in spam database",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            checkRow.addView(checkInput); checkRow.addView(checkBtn)
            addView(checkRow)
        }
    }

    private fun loadCallLog(container: LinearLayout) {
        Thread {
            val calls = mutableListOf<CallEntry>()
            try {
                val cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION
                    ),
                    null, null,
                    "${CallLog.Calls.DATE} DESC"
                )
                cursor?.use { c ->
                    val numIdx  = c.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
                    val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
                    val durIdx  = c.getColumnIndex(CallLog.Calls.DURATION)

                    var count = 0
                    while (c.moveToNext() && count < 20) {
                        calls.add(CallEntry(
                            number   = c.getString(numIdx) ?: "Unknown",
                            name     = c.getString(nameIdx) ?: "",
                            type     = c.getInt(typeIdx),
                            date     = c.getLong(dateIdx),
                            duration = c.getLong(durIdx),
                            isSpam   = spamDb.isSpam(c.getString(numIdx) ?: "")
                        ))
                        count++
                    }
                }
            } catch (_: Exception) {}

            runOnUiThread {
                if (calls.isEmpty()) {
                    container.addView(TextView(this).apply {
                        text = "No recent calls found."
                        setTextColor(0xFF4B5563.toInt()); gravity = Gravity.CENTER
                        setPadding(0, dp(32), 0, 0)
                    })
                    return@runOnUiThread
                }
                calls.forEach { container.addView(buildCallCard(it)) }
            }
        }.start()
    }

    private fun buildCallCard(call: CallEntry): LinearLayout {
        val typeIcon = when (call.type) {
            CallLog.Calls.INCOMING_TYPE  -> "↙"
            CallLog.Calls.OUTGOING_TYPE  -> "↗"
            CallLog.Calls.MISSED_TYPE    -> "↙"
            else -> "?"
        }
        val typeColor = when (call.type) {
            CallLog.Calls.MISSED_TYPE -> 0xFFEF4444.toInt()
            CallLog.Calls.OUTGOING_TYPE -> 0xFF6B7280.toInt()
            else -> 0xFF34D399.toInt()
        }
        val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(call.date))
        val durStr  = if (call.duration > 0) "${call.duration / 60}m ${call.duration % 60}s" else "Missed"

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(if (call.isSpam) 0xFF1F0A0A.toInt() else 0xFF111827.toInt())
                cornerRadius = dp(12).toFloat()
                if (call.isSpam) setStroke(1, 0xFFEF4444.toInt())
            }
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(8)) }

            // Type indicator
            addView(TextView(this@CallerIdActivity).apply {
                text = typeIcon; textSize = 20f; setTextColor(typeColor)
                layoutParams = LinearLayout.LayoutParams(-2, -2).also { it.setMargins(0, 0, dp(14), 0) }
            })

            // Info column
            val col = LinearLayout(this@CallerIdActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            col.addView(TextView(this@CallerIdActivity).apply {
                text = call.name.ifEmpty { call.number }
                textSize = 14f; setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            col.addView(TextView(this@CallerIdActivity).apply {
                text = if (call.name.isNotEmpty()) "${call.number} · $dateStr" else "$dateStr · $durStr"
                textSize = 11f; setTextColor(0xFF6B7280.toInt())
                setPadding(0, dp(2), 0, 0)
            })
            if (call.isSpam) {
                col.addView(TextView(this@CallerIdActivity).apply {
                    text = "⚠️ Spam detected"
                    textSize = 11f; setTextColor(0xFFEF4444.toInt()); setPadding(0, dp(2), 0, 0)
                })
            }
            addView(col)

            // Duration badge
            if (call.type != CallLog.Calls.MISSED_TYPE && call.duration > 0) {
                addView(TextView(this@CallerIdActivity).apply {
                    text = durStr; textSize = 11f; setTextColor(0xFF6B7280.toInt())
                })
            }
        }
    }

    private fun buildPermissionCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF111827.toInt()); cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, dp(16), 0, 0) }

            addView(TextView(this@CallerIdActivity).apply {
                text = "Call Log Access"
                textSize = 16f; setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, dp(8))
            })
            addView(TextView(this@CallerIdActivity).apply {
                text = "Needed to show recent calls and flag spam numbers. Stays on device."
                textSize = 13f; setTextColor(0xFF6B7280.toInt()); setPadding(0, 0, 0, dp(16))
                setLineSpacing(0f, 1.4f)
            })
            addView(TextView(this@CallerIdActivity).apply {
                text = "Grant Access"
                textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(10).toFloat()
                }
                setPadding(0, dp(14), 0, dp(14))
                layoutParams = LinearLayout.LayoutParams(-1, -2)
                setOnClickListener {
                    ActivityCompat.requestPermissions(
                        this@CallerIdActivity,
                        arrayOf(Manifest.permission.READ_CALL_LOG), 102
                    )
                }
            })
        }
    }

    private fun buildSectionLabel(text: String) = TextView(this).apply {
        this.text = text; textSize = 11f; setTextColor(0xFF4B5563.toInt())
        letterSpacing = 0.12f; setPadding(0, dp(4), 0, dp(12))
    }

    private fun buildDivider() = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, 1).also { it.setMargins(0, dp(4), 0, dp(4)) }
        setBackgroundColor(0xFF1F2937.toInt())
    }

    data class CallEntry(
        val number: String, val name: String, val type: Int,
        val date: Long, val duration: Long, val isSpam: Boolean
    )
}
