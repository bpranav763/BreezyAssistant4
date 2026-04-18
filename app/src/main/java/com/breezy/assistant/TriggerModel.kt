package com.breezy.assistant

import org.json.JSONObject

data class BreezyTrigger(
    val id: String,
    val name: String,
    val triggerType: TriggerType,
    val triggerParam: String,
    val actionType: ActionType,
    val actionParam: String,
    val enabled: Boolean = true
) {
    enum class TriggerType(val label: String, val description: String) {
        VOLUME_DOUBLE_DOWN("Vol↓ ×2", "Double-press volume down"),
        VOLUME_DOUBLE_UP("Vol↑ ×2", "Double-press volume up"),
        VOLUME_LONG_DOWN("Vol↓ hold", "Long-press volume down"),
        SMS_KEYWORD("SMS keyword", "Receive SMS containing keyword"),
        BATTERY_BELOW("Battery below %", "When battery drops below threshold"),
        TEMP_ABOVE("Temp above °C", "When phone gets hot"),
        STORAGE_BELOW("Storage below %", "When storage gets full"),
        TIME_DAILY("Daily at time", "Every day at HH:MM"),
        CHARGING_START("Charger plugged", "When charging begins"),
        CHARGING_STOP("Charger unplugged", "When charging stops"),
        SCREEN_ON("Screen turns on", "Every time screen wakes"),
        SHAKE("Phone shake", "When phone is shaken"),
        APP_OPEN("App opened", "When specific app is launched")
    }

    enum class ActionType(val label: String) {
        OPEN_BREEZY("Open Breezy chat"),
        OPEN_APP("Open an app"),
        TOGGLE_WIFI("Toggle WiFi"),
        TOGGLE_DND("Toggle DND"),
        SET_VOLUME("Set volume"),
        SET_BRIGHTNESS("Set brightness"),
        SECURITY_SCAN("Run security scan"),
        RING_PHONE("Ring at max volume"),
        SEND_SMS("Send SMS"),
        SHOW_NOTIFICATION("Show notification"),
        VOICE_INPUT("Open voice input")
    }

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("triggerType", triggerType.name)
        obj.put("triggerParam", triggerParam)
        obj.put("actionType", actionType.name)
        obj.put("actionParam", actionParam)
        obj.put("enabled", enabled)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): BreezyTrigger? {
            return try {
                val obj = JSONObject(json)
                BreezyTrigger(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    triggerType = TriggerType.valueOf(obj.getString("triggerType")),
                    triggerParam = obj.optString("triggerParam", ""),
                    actionType = ActionType.valueOf(obj.getString("actionType")),
                    actionParam = obj.optString("actionParam", ""),
                    enabled = obj.optBoolean("enabled", true)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
