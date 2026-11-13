package com.assisted.speech.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechManager(context: Context, private val onReady: () -> Unit = {}) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSpeech: String? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("SpeechManager", "Default language not supported or missing data, falling back to US English.")
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(0.85f)
            isInitialized = true
            onReady()

            // If a speech request arrived before initialization finished
            pendingSpeech?.let { text ->
                speak(text)
                pendingSpeech = null
            }
        } else {
            Log.e("SpeechManager", "TTS initialization failed with status $status")
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!isInitialized) {
            pendingSpeech = text
            return
        }
        mainHandler.post {
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SpeechId_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        mainHandler.post {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
