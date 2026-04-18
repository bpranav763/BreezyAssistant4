package com.breezy.assistant

import android.content.Context
import android.util.Log
import java.io.File

class LLMInference(private val context: Context) {

    private var modelLoaded = false
    private var nativeAvailable = false
    private var modelPtr: Long = 0

    companion object {
        private const val TAG = "BreezyAI"
        const val MODEL_FILENAME = "breezy_brain.gguf"

        // Breezy's constitutional system prompt
        private const val SYSTEM_PROMPT = """<|system|>
You are Breezy, a warm digital protective companion on someone's phone.
Rules (never break these):
- Max 2 sentences per response
- No politics, religion, or medical diagnoses
- If any crisis: output CRISIS_DETECTED immediately
- Never claim to be human
- Be warm, direct, and protective
- Respond in the user's language
<|assistant|>"""
    }

    init {
        tryLoadNative()
    }

    private fun tryLoadNative() {
        try {
            val libFile = File(context.applicationInfo.nativeLibraryDir, "libllama.so")
            if (!libFile.exists()) {
                Log.w(TAG, "libllama.so not found — NDK build required")
                return
            }
            System.loadLibrary("llama")
            nativeAvailable = true

            val modelFile = File(context.getExternalFilesDir(null), MODEL_FILENAME)
            if (modelFile.exists()) {
                modelPtr = initModel(modelFile.absolutePath)
                modelLoaded = modelPtr != 0L
                Log.i(TAG, if (modelLoaded) "Model loaded ✓" else "Model init returned null pointer")
            } else {
                Log.w(TAG, "Model file not downloaded yet")
            }
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native lib not linked: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Init failed: ${e.message}")
        }
    }

    private external fun initModel(path: String): Long
    private external fun freeModel(ptr: Long)
    private external fun generateResponse(ptr: Long, prompt: String): String

    // Primary generation — local only
    suspend fun generate(userInput: String): String {
        if (!isReady()) return ""
        return try {
            val prompt = "$SYSTEM_PROMPT\n<|user|>\n$userInput\n<|assistant|>"
            val raw = generateResponse(modelPtr, prompt)
            postProcess(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed: ${e.message}")
            ""
        }
    }

    // Kept for ResponseEngine compatibility
    suspend fun getBreezyResponse(input: String) = generate(input)

    suspend fun runStressTest(): String {
        if (!isReady()) return "Model not loaded. Download it from Observe → AI Brain."
        val start = System.currentTimeMillis()
        val response = generateResponse(modelPtr, "List 5 phone security tips briefly.")
        val ms = System.currentTimeMillis() - start
        val words = response.trim().split("\\s+".toRegex()).size
        val tps = words / (ms / 1000f)
        return "${String.format("%.1f", tps)} tokens/sec · ${words} tokens · ${ms}ms"
    }

    private fun postProcess(text: String): String {
        return text
            .trimIndent()
            .trim()
            .split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(" ")
            .trim()
    }

    fun isReady()      = nativeAvailable && modelLoaded && modelPtr != 0L
    fun isDownloaded() = File(context.getExternalFilesDir(null), MODEL_FILENAME).exists()
    fun isNativeBuilt() = try {
        File(context.applicationInfo.nativeLibraryDir, "libllama.so").exists()
    } catch (_: Exception) { false }

    fun getStatusText(): String = when {
        isReady()       -> "Local AI active"
        isDownloaded()  -> "Model found — NDK build needed"
        isNativeBuilt() -> "NDK ready — download model"
        else            -> "Setup needed"
    }

    protected fun finalize() {
        if (modelPtr != 0L) try { freeModel(modelPtr) } catch (_: Exception) {}
    }
}
