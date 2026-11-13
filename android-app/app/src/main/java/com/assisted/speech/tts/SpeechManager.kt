package com.assisted.speech.tts

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechManager(
    private val appContext: Context,
    private val onStatusChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSpeech: String? = null
    private var currentLocaleTag: String = "en"
    private var speechRate: Float = 0.85f
    var isCurrentLocaleVoiceReady: Boolean = true
        private set

    init {
        tts = TextToSpeech(appContext.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyLocale(currentLocaleTag)
            tts?.setSpeechRate(speechRate)

            pendingSpeech?.let { text ->
                speak(text)
                pendingSpeech = null
            }
        } else {
            Log.e("SpeechManager", "TTS initialization failed with status $status")
            isCurrentLocaleVoiceReady = false
            onStatusChanged(false)
        }
    }

    fun setLocale(localeTag: String) {
        currentLocaleTag = localeTag
        if (isInitialized) {
            mainHandler.post { applyLocale(localeTag) }
        }
    }

    fun setRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 1.5f)
        if (isInitialized) {
            mainHandler.post { tts?.setSpeechRate(speechRate) }
        }
    }

    private fun applyLocale(localeTag: String) {
        val targetLocale = Locale.forLanguageTag(localeTag)
        val availability = tts?.isLanguageAvailable(targetLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("SpeechManager", "Locale $localeTag missing voice pack ($availability), falling back.")
            isCurrentLocaleVoiceReady = false
            onStatusChanged(false)
            val fallback = tts?.setLanguage(Locale.getDefault())
            if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
        } else {
            tts?.setLanguage(targetLocale)
            isCurrentLocaleVoiceReady = true
            onStatusChanged(true)
        }
    }

    fun launchVoiceDataInstaller(context: Context) {
        try {
            val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.w("SpeechManager", "Could not launch TTS data installer", e)
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

    /**
     * Plays an audible two-tone attention bell before or independently of speech
     * so a caregiver across the room knows the user is communicating.
     */
    fun playAttentionBell() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 95)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            mainHandler.postDelayed({
                try {
                    toneGen.release()
                } catch (_: Exception) {}
            }, 450)
        } catch (e: Exception) {
            Log.w("SpeechManager", "Could not play attention bell", e)
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
