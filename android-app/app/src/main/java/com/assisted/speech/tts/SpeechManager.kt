package com.assisted.speech.tts

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.assisted.speech.data.SpeakerGender
import java.util.Locale

data class TtsVoiceOption(
    val id: String,
    val label: String,
    val detectedGender: SpeakerGender,
    val country: String,
    val isOffline: Boolean
)

class SpeechManager(
    private val appContext: Context,
    private val onStatusChanged: (Boolean, List<TtsVoiceOption>, String?) -> Unit = { _, _, _ -> }
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSpeech: String? = null

    private var currentLocaleTag: String = "en"
    private var speechRate: Float = 0.85f
    private var speechPitch: Float = 1.0f
    private var preferredGender: SpeakerGender = SpeakerGender.NEUTRAL
    private var selectedVoiceId: String? = null

    var isCurrentLocaleVoiceReady: Boolean = true
        private set

    // Known Google & Samsung TTS male voice token identifiers when features set doesn't explicitly state "male"
    private val knownMaleVoiceTokens = setOf(
        "iom", "iob", "iol", "tpd", "rjs", "gbd", "fis", "aub", "aud",
        "eed", "esd", "esf", "frb", "frd", "deb", "ded", "itd", "ptd",
        "male", "man", "guy", "masculine", "m0", "m1", "m2"
    )
    private val knownFemaleVoiceTokens = setOf(
        "sfg", "iog", "tpf", "tpc", "gba", "gbc", "aua", "auc",
        "eea", "ees", "fra", "frc", "dea", "dec",
        "female", "woman", "feminine", "f0", "f1", "f2"
    )

    init {
        tts = TextToSpeech(appContext.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyLocaleAndVoice()

            pendingSpeech?.let { text ->
                speak(text)
                pendingSpeech = null
            }
        } else {
            Log.e("SpeechManager", "TTS initialization failed with status $status")
            isCurrentLocaleVoiceReady = false
            onStatusChanged(false, emptyList(), null)
        }
    }

    fun configureVoice(
        localeTag: String,
        gender: SpeakerGender,
        specificVoiceId: String?,
        rate: Float,
        pitch: Float
    ) {
        currentLocaleTag = localeTag
        preferredGender = gender
        selectedVoiceId = specificVoiceId
        speechRate = rate.coerceIn(0.5f, 1.5f)
        speechPitch = pitch.coerceIn(0.5f, 1.5f)

        if (isInitialized) {
            mainHandler.post {
                applyLocaleAndVoice()
            }
        }
    }

    private fun detectVoiceGender(voice: Voice): SpeakerGender {
        val nameLower = voice.name.lowercase()
        val featuresLower = voice.features?.map { it.lowercase() }?.toSet().orEmpty()

        if (featuresLower.any { it.contains("female") || it == "gender=female" }) {
            return SpeakerGender.FEMALE
        }
        if (featuresLower.any { it.contains("male") || it == "gender=male" }) {
            return SpeakerGender.MALE
        }

        // Check tokens separated by '-' or '_' in voice name (e.g. "en-us-x-iom-local")
        val parts = nameLower.split("-", "_").toSet()
        if (parts.intersect(knownMaleVoiceTokens).isNotEmpty()) {
            return SpeakerGender.MALE
        }
        if (parts.intersect(knownFemaleVoiceTokens).isNotEmpty()) {
            return SpeakerGender.FEMALE
        }

        return SpeakerGender.NEUTRAL
    }

    private fun applyLocaleAndVoice() {
        val engine = tts ?: return
        val targetLocale = Locale.forLanguageTag(currentLocaleTag)
        val availability = engine.isLanguageAvailable(targetLocale)

        if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("SpeechManager", "Locale $currentLocaleTag missing voice pack ($availability), falling back.")
            isCurrentLocaleVoiceReady = false
            val fallback = engine.setLanguage(Locale.getDefault())
            if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.US)
            }
        } else {
            engine.setLanguage(targetLocale)
            isCurrentLocaleVoiceReady = true
        }

        // Discover all installed/available Voices for this language
        val allMatchingVoices = try {
            engine.voices
                ?.filter { it.locale.language.equals(targetLocale.language, ignoreCase = true) }
                // Prefer offline voices first, then sort deterministically by name
                ?.sortedWith(
                    compareBy<Voice> { it.isNetworkConnectionRequired }
                        .thenBy { it.locale.country }
                        .thenBy { it.name }
                )
                .orEmpty()
        } catch (e: Exception) {
            emptyList()
        }

        val voiceOptions = allMatchingVoices.mapIndexed { idx, v ->
            val gender = detectVoiceGender(v)
            val genderBadge = when (gender) {
                SpeakerGender.MALE -> "Male"
                SpeakerGender.FEMALE -> "Female"
                SpeakerGender.NEUTRAL -> "Voice"
            }
            val region = v.locale.country.takeIf { it.isNotBlank() } ?: v.locale.language.uppercase()
            val netBadge = if (v.isNetworkConnectionRequired) "Online" else "Offline"
            TtsVoiceOption(
                id = v.name,
                label = "$genderBadge ${idx + 1} ($region • $netBadge)",
                detectedGender = gender,
                country = region,
                isOffline = !v.isNetworkConnectionRequired
            )
        }

        // Choose which Voice to activate:
        // 1. Explicit voice chosen by user in Caregiver mode (if it belongs to this language)
        // 2. Best voice matching preferredGender (MALE / FEMALE)
        // 3. Default engine voice
        val chosenVoice: Voice? = when {
            !selectedVoiceId.isNullOrBlank() -> {
                allMatchingVoices.firstOrNull { it.name == selectedVoiceId }
            }
            preferredGender == SpeakerGender.MALE -> {
                allMatchingVoices.firstOrNull { !it.isNetworkConnectionRequired && detectVoiceGender(it) == SpeakerGender.MALE }
                    ?: allMatchingVoices.firstOrNull { detectVoiceGender(it) == SpeakerGender.MALE }
                    ?: allMatchingVoices.getOrNull(1) // Fallback to 2nd voice if gender tags are absent
            }
            preferredGender == SpeakerGender.FEMALE -> {
                allMatchingVoices.firstOrNull { !it.isNetworkConnectionRequired && detectVoiceGender(it) == SpeakerGender.FEMALE }
                    ?: allMatchingVoices.firstOrNull { detectVoiceGender(it) == SpeakerGender.FEMALE }
                    ?: allMatchingVoices.firstOrNull()
            }
            else -> allMatchingVoices.firstOrNull { !it.isNetworkConnectionRequired } ?: allMatchingVoices.firstOrNull()
        }

        var usedDedicatedMaleVoice = false
        if (chosenVoice != null) {
            try {
                engine.voice = chosenVoice
                if (detectVoiceGender(chosenVoice) == SpeakerGender.MALE) {
                    usedDedicatedMaleVoice = true
                }
            } catch (e: Exception) {
                Log.w("SpeechManager", "Failed to set voice ${chosenVoice.name}", e)
            }
        }

        // Apply speech rate & pitch:
        // If user selected MALE gender and the device only has a single Female/Neutral voice installed
        // (and user left pitch at default 1.0f), automatically lower pitch to 0.76f so it sounds male!
        val effectivePitch = if (preferredGender == SpeakerGender.MALE && !usedDedicatedMaleVoice && kotlin.math.abs(speechPitch - 1.0f) < 0.05f) {
            0.76f
        } else {
            speechPitch
        }

        engine.setSpeechRate(speechRate)
        engine.setPitch(effectivePitch)

        onStatusChanged(isCurrentLocaleVoiceReady, voiceOptions, chosenVoice?.name)
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
            // Re-ensure voice & pitch before speaking because some OEM TTS engines reset voice after idle
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SpeechId_${System.currentTimeMillis()}")
        }
    }

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
