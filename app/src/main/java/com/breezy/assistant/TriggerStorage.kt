package com.breezy.assistant

import android.content.Context
import androidx.core.content.edit

class TriggerStorage(context: Context) {

    private val prefs = context.getSharedPreferences("breezy_triggers", Context.MODE_PRIVATE)

    fun saveTrigger(trigger: BreezyTrigger) {
        prefs.edit { putString("trigger_${trigger.id}", trigger.toJson()) }
    }

    fun deleteTrigger(id: String) {
        prefs.edit { remove("trigger_$id") }
    }

    fun getAllTriggers(): List<BreezyTrigger> {
        return prefs.all
            .filter { it.key.startsWith("trigger_") }
            .mapNotNull { BreezyTrigger.fromJson(it.value.toString()) }
            .sortedBy { it.name }
    }

    fun getTriggersForType(type: BreezyTrigger.TriggerType): List<BreezyTrigger> {
        return getAllTriggers().filter { it.triggerType == type && it.enabled }
    }

    fun getTrigger(id: String): BreezyTrigger? {
        val json = prefs.getString("trigger_$id", null) ?: return null
        return BreezyTrigger.fromJson(json)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val trigger = getTrigger(id) ?: return
        saveTrigger(trigger.copy(enabled = enabled))
    }
}
