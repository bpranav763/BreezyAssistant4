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

    fun saveTone(tone: String) = prefs.edit { putString("personality_tone", tone) }
    fun getTone(): String = prefs.getString("personality_tone", "friendly") ?: "friendly"

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
}
