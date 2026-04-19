package com.breezy.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GroqEngine(private val memory: BreezyMemory) {

    private val API_URL = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun generateResponse(prompt: String): String? = withContext(Dispatchers.IO) {
        val apiKey = memory.getGroqApiKey()
        if (apiKey.isEmpty()) return@withContext null

        try {
            val url = URL(API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("messages", JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
                put("max_tokens", 100)
            }

            conn.outputStream.write(body.toString().toByteArray())

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                return@withContext json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
