package com.breezy.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ResponseCooldown(private val context: Context) {

    private val prefs = context.getSharedPreferences("breezy_cooldown", Context.MODE_PRIVATE)
    private val COOLDOWN_MS = 48 * 60 * 60 * 1000L // 48 hours

    fun getResponse(pool: List<String>, key: String, name: String = ""): String {
        val usedKey = "used_${key}"
        val now = System.currentTimeMillis()

        // Get list of recently used responses
        val usedJson = prefs.getString(usedKey, "[]") ?: "[]"
        val usedList = parseUsedList(usedJson).toMutableList()

        // Remove expired entries (older than 48hrs)
        val cleanedList = usedList.filter { (_, time) ->
            now - time < COOLDOWN_MS
        }.toMutableList()

        // Get available responses
        val usedIndices = cleanedList.map { it.first }.toSet()
        val available = pool.indices.filter { it !in usedIndices }

        // If all used, reset
        val candidates = if (available.isEmpty()) pool.indices.toList() else available

        val chosenIdx = candidates.random()
        val response = pool[chosenIdx]

        // Save as used
        cleanedList.add(chosenIdx to now)
        prefs.edit().putString(usedKey, serializeUsedList(cleanedList)).apply()

        return response.replace("{name}", name)
    }

    private fun parseUsedList(json: String): List<Pair<Int, Long>> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                obj.getInt("idx") to obj.getLong("time")
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun serializeUsedList(list: List<Pair<Int, Long>>): String {
        val arr = JSONArray()
        list.forEach { (idx, time) ->
            arr.put(JSONObject().apply {
                put("idx", idx); put("time", time)
            })
        }
        return arr.toString()
    }
}
