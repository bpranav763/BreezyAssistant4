package com.breezy.assistant

import android.content.Context
import android.util.Log
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ModelManager
import kotlinx.coroutines.tasks.await
import java.io.File

class LLMInference(private val context: Context) {

    private var modelLoaded = false
    private var nativeAvailable = false
    private var modelPtr: Long = 0
    
    private var geminiAvailable = false
    private val modelManager = ModelManager.getInstance()
    private val geminiModel = GenerativeModel.builder()
        .setModelName("gemini-nano")
        .build()

    init {
        checkGeminiAvailability()
        
        try {
            // Check if library exists before trying to load
            val libFile = File(context.applicationInfo.nativeLibraryDir, "libllama.so")
            if (libFile.exists()) {
                System.loadLibrary("llama")
                nativeAvailable = true
                
                val modelFile = File(context.filesDir, "breezy_brain.gguf")
                if (modelFile.exists()) {
                    modelPtr = initModel(modelFile.absolutePath)
                    modelLoaded = true
                    Log.d("LLMInference", "Native model ready at pointer $modelPtr")
                } else {
                    Log.w("LLMInference", "Native model not downloaded yet")
                }
            }
        } catch (e: Exception) {
            Log.e("LLMInference", "Init error: ${e.message}")
            nativeAvailable = false
        } catch (e: UnsatisfiedLinkError) {
            Log.w("LLMInference", "Native library link error: ${e.message}")
            nativeAvailable = false
        }
    }

    private fun checkGeminiAvailability() {
        modelManager.isModelDownloaded("gemini-nano")
            .addOnSuccessListener { isDownloaded: Boolean ->
                geminiAvailable = isDownloaded
                Log.d("LLMInference", "Gemini Nano availability: $geminiAvailable")
            }
            .addOnFailureListener {
                geminiAvailable = false
            }
    }

    // Native functions to interact with llama.cpp
    private external fun initModel(modelPath: String): Long
    private external fun freeModel(modelPtr: Long)
    private external fun generateResponse(modelPtr: Long, prompt: String): String

    suspend fun runStressTest(): String {
        if (!nativeAvailable || !modelLoaded || modelPtr == 0L) {
            return "Local AI not available"
        }
        
        val start = System.currentTimeMillis()
        val testPrompt = "Generate exactly 50 words about safety."
        val response = generateResponse(modelPtr, testPrompt)
        val end = System.currentTimeMillis()
        
        val words = response.split(" ").size
        val seconds = (end - start) / 1000f
        val tokensPerSec = words / seconds
        
        return String.format(java.util.Locale.US, "%.1f tokens/sec", tokensPerSec)
    }

    suspend fun getBreezyResponse(prompt: String): String {
        val systemPrompt = "System: You are Breezy, a digital protective companion. " +
                "Be warm and protective. Max 3 sentences. No politics/religion/medical. " +
                "Prompt: $prompt"

        // 1. Try Local SmolLM2 (llama.cpp) first for consistency across devices
        if (nativeAvailable && modelLoaded && modelPtr != 0L) {
            try {
                val response = generateResponse(modelPtr, systemPrompt)
                if (response.isNotEmpty()) {
                    return postProcess(response)
                }
            } catch (e: Exception) {
                Log.e("LLMInference", "Native generation failed: ${e.message}")
            }
        }

        // 2. Fallback to Gemini Nano if available on high-end devices
        if (geminiAvailable) {
            try {
                val response = geminiModel.generateContent(systemPrompt)
                val responseText = response.candidates.firstOrNull()?.text
                if (responseText != null && responseText.isNotEmpty()) {
                    return postProcess(responseText)
                }
            } catch (e: Exception) {
                Log.e("LLMInference", "Gemini generation failed: ${e.message}")
            }
        }

        return "I'm here for you, but I'm having trouble thinking right now. Just know you're safe."
    }

    private fun postProcess(response: String): String {
        val trimmed = response.trim()
        val sentences = trimmed.split(Regex("(?<=[.!?])\\s+"))
        return if (sentences.size > 3) {
            sentences.take(3).joinToString(" ").trim()
        } else {
            trimmed
        }
    }

    fun isReady() = geminiAvailable || (nativeAvailable && modelLoaded)

    protected fun finalize() {
        if (modelPtr != 0L) {
            freeModel(modelPtr)
        }
    }
}
