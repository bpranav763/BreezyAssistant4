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
            setBackgroundColor(0xFF0A0F1E.toInt())
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
                setColor(0xFF1D4ED8.toInt())
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
                setColor(0xFF111827.toInt())
                cornerRadius = dp(12).toFloat()
            }
            gravity = Gravity.CENTER_VERTICAL

            val info = LinearLayout(this@TriggerActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            info.addView(TextView(this@TriggerActivity).apply {
                text = trigger.name
                textSize = 16f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })

            info.addView(TextView(this@TriggerActivity).apply {
                text = "${trigger.triggerType.label} → ${trigger.actionType.label}"
                textSize = 12f
                setTextColor(0xFF9CA3AF.toInt())
            })

            addView(info)

            val switch = androidx.appcompat.widget.SwitchCompat(this@TriggerActivity).apply {
                isChecked = trigger.enabled
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
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(0xFF111827.toInt())
        }

        val nameInput = EditText(this).apply {
            hint = "Trigger Name"
            setHintTextColor(0xFF6B7280.toInt())
            setTextColor(Color.WHITE)
        }
        dialogView.addView(nameInput)

        val triggerParamInput = EditText(this).apply {
            hint = "Trigger Parameter (e.g. SMS keyword)"
            setHintTextColor(0xFF6B7280.toInt())
            setTextColor(Color.WHITE)
            visibility = View.GONE
        }
        val actionParamInput = EditText(this).apply {
            hint = "Action Parameter (e.g. app pkg / number::msg)"
            setHintTextColor(0xFF6B7280.toInt())
            setTextColor(Color.WHITE)
            visibility = View.GONE
        }

        val triggerTypes = BreezyTrigger.TriggerType.values()
        val actionTypes = BreezyTrigger.ActionType.values()

        dialogView.addView(TextView(this).apply { text = "When..."; setTextColor(Color.GRAY); setPadding(0, dp(12), 0, dp(4)) })
        val triggerSpinner = Spinner(this).apply {
            adapter = object : ArrayAdapter<String>(this@TriggerActivity, android.R.layout.simple_spinner_dropdown_item, triggerTypes.map { it.label }) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply { setTextColor(Color.WHITE) }
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(Color.WHITE)
                        setBackgroundColor(0xFF1F2937.toInt())
                    }
                }
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val type = triggerTypes[position]
                    triggerParamInput.visibility = if (type == BreezyTrigger.TriggerType.SMS_KEYWORD || 
                        type == BreezyTrigger.TriggerType.BATTERY_BELOW || type == BreezyTrigger.TriggerType.TEMP_ABOVE ||
                        type == BreezyTrigger.TriggerType.STORAGE_BELOW || type == BreezyTrigger.TriggerType.TIME_DAILY ||
                        type == BreezyTrigger.TriggerType.APP_OPEN) View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        dialogView.addView(triggerSpinner)
        dialogView.addView(triggerParamInput)

        dialogView.addView(TextView(this).apply { text = "Do..."; setTextColor(Color.GRAY); setPadding(0, dp(12), 0, dp(4)) })
        val actionSpinner = Spinner(this).apply {
            adapter = object : ArrayAdapter<String>(this@TriggerActivity, android.R.layout.simple_spinner_dropdown_item, actionTypes.map { it.label }) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply { setTextColor(Color.WHITE) }
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(Color.WHITE)
                        setBackgroundColor(0xFF1F2937.toInt())
                    }
                }
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val type = actionTypes[position]
                    actionParamInput.visibility = if (type == BreezyTrigger.ActionType.OPEN_APP ||
                        type == BreezyTrigger.ActionType.SET_VOLUME || type == BreezyTrigger.ActionType.SET_BRIGHTNESS ||
                        type == BreezyTrigger.ActionType.SEND_SMS || type == BreezyTrigger.ActionType.SHOW_NOTIFICATION) View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        dialogView.addView(actionSpinner)
        dialogView.addView(actionParamInput)

        val dialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(dialogView)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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
}
