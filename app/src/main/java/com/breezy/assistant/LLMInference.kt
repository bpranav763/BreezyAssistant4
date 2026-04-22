package com.breezy.assistant

import android.content.Context
import android.util.Log
import com.getkeepsafe.relinker.ReLinker
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LLMInference(private val context: Context) {

    private var llmInference: LlmInference? = null
    private var isInitializing = false

    companion object {
        private const val TAG = "BreezyAI"
        const val MODEL_FILENAME = "breezy_brain.gguf"
        
        const val MODEL_URL = "https://github.com/bpranav763/BreezyAssistant4/releases/download/v1.0.0/breezy_brain.gguf"
    }

    init {
        try {
            ReLinker.loadLibrary(context, "llama_jni")
            ensureLoaded()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load native library", e)
        }
    }

    fun ensureLoaded() {
        if (llmInference != null || isInitializing) return
        
        val modelFile = File(context.getExternalFilesDir(null), MODEL_FILENAME)
        if (!modelFile.exists()) {
            Log.w(TAG, "Model file not found at ${modelFile.absolutePath}")
            return
        }

        isInitializing = true
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(128)
                .setTemperature(0.7f)
                .setRandomSeed(42)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.i(TAG, "MediaPipe LLM Engine Initialized ✓")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe: ${e.message}")
        } finally {
            isInitializing = false
        }
    }

    suspend fun generate(userInput: String): String = withContext(Dispatchers.Default) {
        val engine = llmInference ?: return@withContext ""
        
        return@withContext try {
            val systemPrompt = "You are Breezy, a private assistant. Be brief (1-2 sentences). "
            val fullPrompt = "$systemPrompt User: $userInput Assistant:"
            
            val result = engine.generateResponse(fullPrompt)
            result.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed: ${e.message}")
            "My brain is offline. Try restarting the app."
        }
    }

    suspend fun runStressTest(): String = withContext(Dispatchers.Default) {
        val engine = llmInference ?: return@withContext "Engine not ready"
        try {
            val startTime = System.currentTimeMillis()
            val testPrompt = "Explain quantum physics in one sentence."
            val response = engine.generateResponse(testPrompt)
            val endTime = System.currentTimeMillis()
            
            val durationSeconds = (endTime - startTime) / 1000.0
            val tokenCount = response.split(" ").size // Rough estimate
            val tps = tokenCount / durationSeconds
            
            String.format("%.1f tokens/sec", tps)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun isReady() = llmInference != null
    fun isDownloaded() = File(context.getExternalFilesDir(null), MODEL_FILENAME).exists()

    fun getStatusText(): String = when {
        isReady() -> "Local AI active ✓"
        isDownloaded() -> "Brain found — Preparing..."
        else -> "Setup needed"
    }
}
