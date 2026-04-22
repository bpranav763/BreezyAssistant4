package com.breezy.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*

class TriggerActivity : BaseActivity() {

    private val storage by lazy { TriggerStorage(this) }
    private lateinit var triggerListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@TriggerActivity))
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        root.addView(buildHeader("⚡ Automation") { finish() })

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(32))
        }

        val addBtn = TextView(this).apply {
            text = "+ Create New Trigger"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(ThemeManager.getAccentColor(this@TriggerActivity))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(48))
            setOnClickListener { showAddTriggerDialog() }
        }
        container.addView(addBtn)
        container.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(24)) })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        triggerListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(triggerListContainer)
        container.addView(scroll)
        root.addView(container)

        setContentView(root)
        applySystemBarInsets(root)
        refreshList()
    }

    private fun refreshList() {
        triggerListContainer.removeAllViews()
        val triggers = storage.getAllTriggers()
        if (triggers.isEmpty()) {
            triggerListContainer.addView(TextView(this).apply {
                text = "No triggers yet. Tap + to create one."
                setTextColor(0xFF6B7280.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(60), 0, 0)
            })
        } else {
            triggers.forEach { trigger ->
                triggerListContainer.addView(buildTriggerItem(trigger))
                triggerListContainer.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(12)) })
            }
        }
    }

    private fun buildTriggerItem(trigger: BreezyTrigger): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@TriggerActivity))
                cornerRadius = ThemeManager.getCornerRadius(this@TriggerActivity).dp(this@TriggerActivity).toFloat()
            }
            gravity = Gravity.CENTER_VERTICAL

            val info = LinearLayout(this@TriggerActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            info.addView(TextView(this@TriggerActivity).apply {
                text = trigger.name
                textSize = 16f
                setTextColor(ThemeManager.getTextPrimary(this@TriggerActivity))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })

            info.addView(TextView(this@TriggerActivity).apply {
                text = "${trigger.triggerType.label} → ${trigger.actionType.label}"
                textSize = 12f
                setTextColor(ThemeManager.getTextSecondary(this@TriggerActivity))
            })

            addView(info)

            val switch = androidx.appcompat.widget.SwitchCompat(this@TriggerActivity).apply {
                isChecked = trigger.enabled
                thumbTintList = android.content.res.ColorStateList.valueOf(ThemeManager.getAccentColor(this@TriggerActivity))
                trackTintList = android.content.res.ColorStateList.valueOf(ThemeManager.getAccentColor(this@TriggerActivity) and 0x33FFFFFF)
                setOnCheckedChangeListener { _, isChecked ->
                    storage.setEnabled(trigger.id, isChecked)
                }
            }
            addView(switch)

            setOnLongClickListener {
                showDeleteConfirm(trigger)
                true
            }
        }
    }

    private fun showDeleteConfirm(trigger: BreezyTrigger) {
        android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Delete Trigger")
            .setMessage("Remove '${trigger.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                storage.deleteTrigger(trigger.id)
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTriggerDialog() {
        val dialog = android.app.Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.getBackgroundColor(this@TriggerActivity))
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "Create New Trigger"
            textSize = 24f
            setTextColor(ThemeManager.getTextPrimary(this@TriggerActivity))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(24))
        })

        val nameInput = createStyledEditText("Trigger Name", "e.g. Morning Routine")
        root.addView(nameInput)
        root.addView(createSpacer(16))

        val triggerParamInput = createStyledEditText("Trigger Parameter", "Value").apply { visibility = View.GONE }
        val actionParamInput = createStyledEditText("Action Parameter", "Value").apply { visibility = View.GONE }

        val triggerTypes = BreezyTrigger.TriggerType.values()
        val actionTypes = BreezyTrigger.ActionType.values()

        root.addView(createLabel("WHEN THIS HAPPENS"))
        val triggerSpinner = createStyledSpinner(triggerTypes.map { it.label }) { position ->
            val type = triggerTypes[position]
            triggerParamInput.visibility = if (needsParam(type)) View.VISIBLE else View.GONE
        }
        root.addView(triggerSpinner)
        root.addView(triggerParamInput)
        root.addView(createSpacer(16))

        root.addView(createLabel("DO THIS ACTION"))
        val actionSpinner = createStyledSpinner(actionTypes.map { it.label }) { position ->
            val type = actionTypes[position]
            actionParamInput.visibility = if (needsActionParam(type)) View.VISIBLE else View.GONE
        }
        root.addView(actionSpinner)
        root.addView(actionParamInput)
        
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(32), 0, 0)
        }

        val cancelBtn = TextView(this).apply {
            text = "CANCEL"
            setTextColor(0xFF9CA3AF.toInt())
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener { dialog.dismiss() }
        }

        val createBtn = TextView(this).apply {
            text = "CREATE TRIGGER"
            setTextColor(ThemeManager.getAccentColor(this@TriggerActivity))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener {
                val name = nameInput.text.toString()
                if (name.isBlank()) {
                    nameInput.error = "Name required"
                    return@setOnClickListener
                }
                val trigger = BreezyTrigger(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    triggerType = triggerTypes[triggerSpinner.selectedItemPosition],
                    triggerParam = triggerParamInput.text.toString(),
                    actionType = actionTypes[actionSpinner.selectedItemPosition],
                    actionParam = actionParamInput.text.toString()
                )
                storage.saveTrigger(trigger)
                refreshList()
                dialog.dismiss()
            }
        }

        bottomBar.addView(cancelBtn)
        bottomBar.addView(createBtn)
        root.addView(bottomBar)

        dialog.setContentView(root)
        dialog.show()
    }

    private fun createStyledEditText(hint: String, floatingHint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            setHintTextColor(ThemeManager.getTextSecondary(this@TriggerActivity))
            setTextColor(ThemeManager.getTextPrimary(this@TriggerActivity))
            background = GradientDrawable().apply {
                setColor(ThemeManager.getCardColor(this@TriggerActivity))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), ThemeManager.getTextSecondary(this@TriggerActivity) and 0x33FFFFFF)
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, -1).apply { setMargins(0, dp(4), 0, dp(4)) }
        }
    }

    private fun createLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 11f
        setTextColor(ThemeManager.getAccentColor(this@TriggerActivity))
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(0, dp(12), 0, dp(4))
    }

    private fun createSpacer(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun createStyledSpinner(items: List<String>, onSelect: (Int) -> Unit): Spinner {
        return Spinner(this).apply {
            adapter = object : ArrayAdapter<String>(this@TriggerActivity, android.R.layout.simple_spinner_dropdown_item, items) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply {
                        setTextColor(ThemeManager.getTextPrimary(this@TriggerActivity))
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                        background = GradientDrawable().apply {
                            setColor(ThemeManager.getCardColor(this@TriggerActivity))
                            cornerRadius = dp(8).toFloat()
                            setStroke(dp(1), ThemeManager.getTextSecondary(this@TriggerActivity) and 0x33FFFFFF)
                        }
                    }
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(ThemeManager.getTextPrimary(this@TriggerActivity))
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                        setBackgroundColor(ThemeManager.getCardColor(this@TriggerActivity))
                    }
                }
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) = onSelect(p2)
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
            layoutParams = LinearLayout.LayoutParams(-1, -1).apply { setMargins(0, dp(4), 0, dp(8)) }
        }
    }

    private fun needsParam(type: BreezyTrigger.TriggerType): Boolean = when(type) {
        BreezyTrigger.TriggerType.SMS_KEYWORD, 
        BreezyTrigger.TriggerType.BATTERY_BELOW, 
        BreezyTrigger.TriggerType.TEMP_ABOVE,
        BreezyTrigger.TriggerType.STORAGE_BELOW, 
        BreezyTrigger.TriggerType.TIME_DAILY,
        BreezyTrigger.TriggerType.APP_OPEN -> true
        else -> false
    }

    private fun needsActionParam(type: BreezyTrigger.ActionType): Boolean = when(type) {
        BreezyTrigger.ActionType.OPEN_APP,
        BreezyTrigger.ActionType.SET_VOLUME, 
        BreezyTrigger.ActionType.SET_BRIGHTNESS,
        BreezyTrigger.ActionType.SEND_SMS, 
        BreezyTrigger.ActionType.SHOW_NOTIFICATION -> true
        else -> false
    }

}
