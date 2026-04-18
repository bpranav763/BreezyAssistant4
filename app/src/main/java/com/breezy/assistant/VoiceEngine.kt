package com.breezy.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceEngine(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var onResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    var isListening = false
        private set

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device.")
            return
        }

        this.onResult = onResult
        this.onError  = onError

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotEmpty()) onResult(text)
                else onError("Couldn\u0027t hear anything clearly.")
            }
            override fun onError(error: Int) {
                isListening = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO            -> "Audio recording error."
                    SpeechRecognizer.ERROR_CLIENT           -> "Client side error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Needs RECORD_AUDIO permission."
                    SpeechRecognizer.ERROR_NETWORK          -> "Network error."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT  -> "Network timeout."
                    SpeechRecognizer.ERROR_NO_MATCH         -> "No speech match found."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY  -> "Recognizer busy."
                    SpeechRecognizer.ERROR_SERVER           -> "Server error."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT   -> "No speech detected."
                    else -> "Unknown error ($error)."
                }
                onError(msg)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")   // Hinglish-friendly
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            onError("Couldn\u0027t start listening: ${e.message}")
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        isListening = false
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}
