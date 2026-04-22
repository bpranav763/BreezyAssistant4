package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import net.sqlcipher.database.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*

class NotesActivity : BaseActivity() {

    private var db: SQLiteDatabase? = null
    private lateinit var notesContainer: LinearLayout
    private val PASSPHRASE = "breezy_notes_secure_2024"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SQLiteDatabase.loadLibs(this)
        initDatabase()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@NotesActivity))
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        root.addView(buildHeader("Notes") { finish() })

        // Input area
        val inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@NotesActivity)); setCornerRadius(dp(14).toFloat())
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also {
                it.setMargins(dp(20), dp(8), dp(20), dp(12))
            }
        }

        val noteInput = EditText(this).apply {
            hint = "Write something…"
            setHintTextColor(ThemeManager.getTextSecondary(this@NotesActivity)); setTextColor(ThemeManager.getTextPrimary(this@NotesActivity))
            textSize = 15f; background = null; minLines = 2
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        inputCard.addView(noteInput)

        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
            setPadding(0, dp(8), 0, 0)
        }
        addRow.addView(TextView(this).apply {
            text = "Save note"
            textSize = 13f; setTextColor(ThemeManager.getAccentColor(this@NotesActivity))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(8), 0, dp(4))
            setOnClickListener {
                val text = noteInput.text.toString().trim()
                if (text.isNotEmpty()) {
                    saveNote(text)
                    noteInput.setText("")
                    refreshNotes()
                }
            }
        })
        inputCard.addView(addRow)
        root.addView(inputCard)

        // Notes list
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        notesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), dp(40))
        }
        scroll.addView(notesContainer)
        root.addView(scroll)

        setContentView(root)
        applySystemBarInsets(root)
        refreshNotes()
    }

    private fun initDatabase() {
        try {
            val dbFile = getDatabasePath("breezy_notes.db")
            dbFile.parentFile?.mkdirs()
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, PASSPHRASE, null)
            db?.execSQL("""
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    content TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """)
        } catch (e: Exception) {
            Toast.makeText(this, "Notes DB error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveNote(content: String) {
        val now = System.currentTimeMillis()
        db?.execSQL("INSERT INTO notes (content, created_at, updated_at) VALUES (?, ?, ?)",
            arrayOf(content, now, now))
    }

    private fun deleteNote(id: Long) {
        db?.delete("notes", "id=?", arrayOf(id.toString()))
    }

    private fun refreshNotes() {
        notesContainer.removeAllViews()
        val safeDb = db ?: return

        val cursor = safeDb.rawQuery(
            "SELECT id, content, created_at FROM notes ORDER BY created_at DESC", null
        )

        if (cursor.count == 0) {
            cursor.close()
            notesContainer.addView(TextView(this).apply {
                text = "No notes yet."
                setTextColor(0xFF374151.toInt()); textSize = 14f; gravity = Gravity.CENTER
                setPadding(0, dp(60), 0, 0)
            })
            return
        }

        val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        while (cursor.moveToNext()) {
            val id      = cursor.getLong(0)
            val content = cursor.getString(1)
            val date    = cursor.getLong(2)
            notesContainer.addView(buildNoteCard(id, content, fmt.format(Date(date))))
        }
        cursor.close()
    }

    private fun buildNoteCard(id: Long, content: String, dateStr: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@NotesActivity)); setCornerRadius(dp(12).toFloat())
            }
            setPadding(dp(18), dp(16), dp(18), dp(14))
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(10)) }

            addView(TextView(this@NotesActivity).apply {
                text = content; setTextColor(ThemeManager.getTextPrimary(this@NotesActivity)); textSize = 14f
                setLineSpacing(0f, 1.5f)
            })

            val footer = LinearLayout(this@NotesActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, 0)
            }
            footer.addView(TextView(this@NotesActivity).apply {
                text = dateStr; textSize = 11f; setTextColor(ThemeManager.getTextSecondary(this@NotesActivity))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            footer.addView(TextView(this@NotesActivity).apply {
                text = "Delete"; textSize = 12f; setTextColor(ThemeManager.getAccentColor(this@NotesActivity))
                setPadding(dp(12), dp(4), 0, dp(4))
                setOnClickListener { deleteNote(id); refreshNotes() }
            })
            addView(footer)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        db?.close()
    }
}
