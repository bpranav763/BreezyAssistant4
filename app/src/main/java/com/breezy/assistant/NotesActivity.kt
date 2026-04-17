package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class NotesActivity : BaseActivity() {

    private val memory by lazy { BreezyMemory(this) }
    private lateinit var notesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        root.addView(buildHeader("📝 Notes") { finish() })

        val inputArea = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(24), dp(16), dp(24), dp(16))
            gravity = Gravity.CENTER_VERTICAL
        }

        val noteInput = EditText(this).apply {
            hint = "Quick note..."
            setHintTextColor(0xFF4B5563.toInt())
            setTextColor(Color.WHITE)
            setBackgroundColor(0xFF111827.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val addBtn = TextView(this).apply {
            text = "Add Note"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = GradientDrawable().apply {
                setColor(0xFF1D4ED8.toInt()); cornerRadius = dp(8).toFloat()
            }
            setOnClickListener {
                val text = noteInput.text.toString().trim()
                if (text.isNotEmpty()) {
                    val id = System.currentTimeMillis().toString()
                    memory.saveFact("note_$id", text)
                    noteInput.setText("")
                    refreshNotes()
                }
            }
        }

        inputArea.addView(noteInput)
        inputArea.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(12), 1) })
        inputArea.addView(addBtn)
        root.addView(inputArea)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        notesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), 0, dp(24), dp(32))
        }
        scroll.addView(notesContainer)
        root.addView(scroll)

        setContentView(root)
        applySystemBarInsets(root)
        refreshNotes()
    }

    private fun refreshNotes() {
        notesContainer.removeAllViews()
        val notes = memory.getAllFacts().filter { it.key.startsWith("note_") }
        
        if (notes.isEmpty()) {
            notesContainer.addView(TextView(this).apply {
                text = "No notes yet. Breezy will remember what you type here."
                setTextColor(0xFF6B7280.toInt()); textSize = 13f; gravity = Gravity.CENTER
                setPadding(0, dp(64), 0, 0)
            })
            return
        }

        notes.toList().sortedByDescending { it.first }.forEach { (key, value) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(0xFF111827.toInt()); cornerRadius = dp(16).toFloat()
                }
                setPadding(dp(20), dp(20), dp(20), dp(20))
                layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(16)) }
                
                addView(TextView(this@NotesActivity).apply {
                    text = value; setTextColor(Color.WHITE); textSize = 15f
                    setLineSpacing(0f, 1.2f)
                })

                val bottom = LinearLayout(this@NotesActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                    setPadding(0, dp(12), 0, 0)
                }
                bottom.addView(TextView(this@NotesActivity).apply {
                    text = "Delete"; setTextColor(0xFFEF4444.toInt()); textSize = 12f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        memory.deleteFact(key)
                        refreshNotes()
                    }
                })
                addView(bottom)
            }
            notesContainer.addView(card)
        }
    }
}

