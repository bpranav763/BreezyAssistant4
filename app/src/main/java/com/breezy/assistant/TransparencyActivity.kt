package com.breezy.assistant
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class TransparencyActivity : BaseActivity() {
    private val memory by lazy { BreezyMemory(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF0A0F1E.toInt())
            layoutParams = LinearLayout.LayoutParams(-1,-1)
        }
        root.addView(buildHeader("🔍 What Breezy Knows") { finish() })
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24),dp(8),dp(24),dp(32)) }

        fun addRow(label: String, value: String) {
            container.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF111827.toInt())
                setPadding(dp(20),dp(14),dp(20),dp(14))
                layoutParams = LinearLayout.LayoutParams(-1,-2).also { it.setMargins(0,0,0,dp(8)) }
                addView(TextView(this@TransparencyActivity).apply { text = label; textSize = 11f; setTextColor(0xFF6B7280.toInt()) })
                addView(TextView(this@TransparencyActivity).apply { text = value.ifEmpty { "Not set" }; textSize = 15f; setTextColor(Color.WHITE) })
            })
        }

        addRow("Name", memory.getUserName())
        addRow("Tone", memory.getTone())
        addRow("Region", memory.getRegion())
        memory.getAllFacts().forEach { (k, v) -> addRow(k, v) }

        container.addView(TextView(this).apply {
            text = "Delete Everything"
            textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setBackgroundColor(0xFF7F1D1D.toInt()); setPadding(0, dp(16), 0, dp(16))
            layoutParams = LinearLayout.LayoutParams(-1,-2).also { it.setMargins(0,dp(24),0,0) }
            setOnClickListener { memory.clearAll(); Toast.makeText(this@TransparencyActivity,"All deleted",Toast.LENGTH_SHORT).show(); finish() }
        })

        scroll.addView(container); root.addView(scroll)
        setContentView(root); applySystemBarInsets(root)
    }
}
