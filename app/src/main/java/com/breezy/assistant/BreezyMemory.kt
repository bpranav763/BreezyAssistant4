package com.breezy.assistant

import android.content.Context
import androidx.core.content.edit

class BreezyMemory(private val context: Context) {

    private val prefs = context.getSharedPreferences("breezy_memory", Context.MODE_PRIVATE)

    fun saveBubbleEnabled(enabled: Boolean) = prefs.edit { putBoolean("bubble_enabled", enabled) }
    fun isBubbleEnabled(): Boolean = prefs.getBoolean("bubble_enabled", true)

    fun saveBubbleSize(size: Int) = prefs.edit { putInt("bubble_size", size) }
    fun getBubbleSize(): Int = prefs.getInt("bubble_size", 62)

    fun saveUserName(name: String) = prefs.edit { putString("user_name", name) }
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""

    fun saveUserDOB(dob: String) = prefs.edit { putString("user_dob", dob) }
    fun getUserDOB(): String = prefs.getString("user_dob", "") ?: ""

    fun saveUserProfession(prof: String) = prefs.edit { putString("user_profession", prof) }
    fun getUserProfession(): String = prefs.getString("user_profession", "") ?: ""

    fun saveTone(tone: String) = prefs.edit { putString("personality_tone", tone) }
    fun getTone(): String = prefs.getString("personality_tone", "friendly") ?: "friendly"

    fun saveBubbleColor(color: Int) = prefs.edit { putInt("bubble_color", color) }
    fun getBubbleColor(): Int = prefs.getInt("bubble_color", 0xFF1D4ED8.toInt())

    fun saveBubbleIdleTime(seconds: Int) = prefs.edit { putInt("bubble_idle_time", seconds) }
    fun getBubbleIdleTime(): Int = prefs.getInt("bubble_idle_time", 12)

    fun saveBubbleIdleSize(size: Int) = prefs.edit { putInt("bubble_idle_size", size) }
    fun getBubbleIdleSize(): Int = prefs.getInt("bubble_idle_size", 14)

    fun saveRegion(region: String) = prefs.edit { putString("user_region", region) }
    fun getRegion(): String = prefs.getString("user_region", "IN") ?: "IN"

    fun saveStressCooldown(timestamp: Long) = prefs.edit { putLong("stress_cooldown", timestamp) }
    fun getStressCooldown(): Long = prefs.getLong("stress_cooldown", 0L)

    fun saveFact(key: String, value: String) = prefs.edit { putString("fact_$key", value) }
    fun getFact(key: String): String = prefs.getString("fact_$key", "") ?: ""

    fun getAllFacts(): Map<String, String> {
        return prefs.all
            .filter { it.key.startsWith("fact_") }
            .mapKeys { it.key.removePrefix("fact_") }
            .mapValues { it.value.toString() }
    }

    fun deleteFact(key: String) = prefs.edit { remove("fact_$key") }
    fun clearAll() = prefs.edit { clear() }

    fun isOnboardingDone(): Boolean = prefs.getBoolean("onboarding_done", false)
    fun setOnboardingDone() = prefs.edit { putBoolean("onboarding_done", true) }

    fun saveGeminiApiKey(key: String) = prefs.edit { putString("gemini_api_key", key) }
    fun getGeminiApiKey(): String = prefs.getString("gemini_api_key", "") ?: ""

    fun saveGroqApiKey(key: String) = prefs.edit { putString("groq_api_key", key) }
    fun getGroqApiKey(): String = prefs.getString("groq_api_key", "") ?: ""

    fun saveJoystickConfig(config: String) = prefs.edit { putString("joystick_config", config) }
    fun getJoystickConfig(): String = prefs.getString("joystick_config", "security,notes,observe,vault,main") ?: "security,notes,observe,vault,main"

    fun saveBubbleAiMode(mode: String) = prefs.edit { putString("bubble_ai_mode", mode) }
    fun getBubbleAiMode(): String = prefs.getString("bubble_ai_mode", "hybrid") ?: "hybrid" // hybrid, llm, groq, pool_only

    fun saveAutoDownloadEnabled(enabled: Boolean) = prefs.edit { putBoolean("auto_download_llm", enabled) }
    fun isAutoDownloadEnabled(): Boolean = prefs.getBoolean("auto_download_llm", true)
}
