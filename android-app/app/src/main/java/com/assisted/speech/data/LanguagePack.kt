package com.assisted.speech.data

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer

enum class SpeakerGender(val label: String) {
    NEUTRAL("Neutral"),
    MALE("Male"),
    FEMALE("Female")
}

enum class Formality(val label: String) {
    POLITE("Polite / Formal"),
    CASUAL("Casual / Family")
}

data class LocalizedUiStrings(
    val speakNow: String = "SPEAK NOW",
    val repeat: String = "REPEAT",
    val back: String = "BACK",
    val undo: String = "UNDO",
    val more: String = "MORE",
    val spell: String = "SPELL",
    val tree: String = "PHRASES",
    val addWordSpace: String = "ADD WORD / SPACE",
    val savePhrase: String = "SAVE PHRASE",
    val tapShortcutPrompt: String = "Tap a shortcut or starter below…",
    val buildingSentence: String = "BUILDING SENTENCE",
    val lastSpokenPrompt: String = "LAST SPOKEN (TAP REPEAT TO SPEAK AGAIN)",
    val readyPrompt: String = "READY — TAP ANY PHRASE TO SPEAK",
    val oneTapSpeak: String = "1-TAP",
    val scanTrigger: String = "TAP ANYWHERE TO SELECT HIGHLIGHTED"
)

data class LanguageMetadata(
    val locale: String = "en",
    val displayName: String = "English",
    val isRtl: Boolean = false,
    val wordSeparator: String = " ",
    val keyboardPages: List<List<String>> = listOf(
        listOf("E", "T", "A", "O", "I", "N", "S", "H", "R", "L", "D", "C"),
        listOf("U", "M", "W", "F", "G", "Y", "P", "B", "V", "K", "J", "X"),
        listOf("Q", "Z", "'", "?")
    ),
    val diacritics: Map<String, List<String>> = emptyMap(),
    val grammarForms: Map<String, Map<String, String>> = emptyMap(),
    val uiStrings: LocalizedUiStrings = LocalizedUiStrings()
)

data class LanguagePackData(
    val metadata: LanguageMetadata,
    val concepts: List<PhraseNodeEntity>,
    val ngrams: Map<String, List<String>>,
    val coreVocabulary: List<String>
)

object LanguagePackLoader {
    private val cachedPacks = mutableMapOf<String, LanguagePackData>()

    fun getAvailableLocales(context: Context): List<Pair<String, String>> {
        val dirs = try {
            context.assets.list("lang")?.toList().orEmpty()
        } catch (_: Exception) {
            listOf("en")
        }
        return dirs.map { code ->
            val pack = load(context, code)
            code to pack.metadata.displayName
        }
    }

    fun load(context: Context, locale: String = "en"): LanguagePackData {
        cachedPacks[locale]?.let { return it }

        val assetPath = "lang/$locale/pack.json"
        val jsonText = try {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            context.assets.open("lang/en/pack.json").bufferedReader().use { it.readText() }
        }

        val root = JSONObject(jsonText)
        val metaJson = root.getJSONObject("metadata")

        // Parse keyboard pages
        val pagesArray = metaJson.getJSONArray("keyboardPages")
        val keyboardPages = mutableListOf<List<String>>()
        for (i in 0 until pagesArray.length()) {
            val rowArr = pagesArray.getJSONArray(i)
            val pageLetters = mutableListOf<String>()
            for (j in 0 until rowArr.length()) {
                pageLetters.add(rowArr.getString(j))
            }
            keyboardPages.add(pageLetters)
        }

        // Parse diacritics
        val diacriticsMap = mutableMapOf<String, List<String>>()
        metaJson.optJSONObject("diacritics")?.let { diaObj ->
            diaObj.keys().forEach { baseKey ->
                val arr = diaObj.getJSONArray(baseKey)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                diacriticsMap[baseKey] = list
            }
        }

        // Parse grammar forms
        val grammarMap = mutableMapOf<String, Map<String, String>>()
        metaJson.optJSONObject("grammarForms")?.let { gramObj ->
            gramObj.keys().forEach { tokenKey ->
                val formObj = gramObj.getJSONObject(tokenKey)
                val inner = mutableMapOf<String, String>()
                formObj.keys().forEach { k ->
                    inner[k] = formObj.getString(k)
                }
                grammarMap[tokenKey] = inner
            }
        }

        // Parse localized UI strings
        val uiObj = metaJson.optJSONObject("uiStrings")
        val uiStrings = if (uiObj != null) {
            LocalizedUiStrings(
                speakNow = uiObj.optString("speakNow", "SPEAK NOW"),
                repeat = uiObj.optString("repeat", "REPEAT"),
                back = uiObj.optString("back", "BACK"),
                undo = uiObj.optString("undo", "UNDO"),
                more = uiObj.optString("more", "MORE"),
                spell = uiObj.optString("spell", "SPELL"),
                tree = uiObj.optString("tree", "PHRASES"),
                addWordSpace = uiObj.optString("addWordSpace", "ADD WORD / SPACE"),
                savePhrase = uiObj.optString("savePhrase", "SAVE PHRASE"),
                tapShortcutPrompt = uiObj.optString("tapShortcutPrompt", "Tap a shortcut or starter below…"),
                buildingSentence = uiObj.optString("buildingSentence", "BUILDING SENTENCE"),
                lastSpokenPrompt = uiObj.optString("lastSpokenPrompt", "LAST SPOKEN (TAP REPEAT TO SPEAK AGAIN)"),
                readyPrompt = uiObj.optString("readyPrompt", "READY — TAP ANY PHRASE TO SPEAK"),
                oneTapSpeak = uiObj.optString("oneTapSpeak", "1-TAP"),
                scanTrigger = uiObj.optString("scanTrigger", "TAP ANYWHERE TO SELECT HIGHLIGHTED")
            )
        } else {
            LocalizedUiStrings()
        }

        val metadata = LanguageMetadata(
            locale = metaJson.optString("locale", locale),
            displayName = metaJson.optString("displayName", "English"),
            isRtl = metaJson.optBoolean("isRtl", false),
            wordSeparator = metaJson.optString("wordSeparator", " "),
            keyboardPages = keyboardPages,
            diacritics = diacriticsMap,
            grammarForms = grammarMap,
            uiStrings = uiStrings
        )

        // Parse concepts
        val conceptsArray = root.getJSONArray("concepts")
        val concepts = mutableListOf<PhraseNodeEntity>()
        for (i in 0 until conceptsArray.length()) {
            val c = conceptsArray.getJSONObject(i)
            val followUps = mutableListOf<String>()
            c.optJSONArray("followUpConceptIds")?.let { fArr ->
                for (j in 0 until fArr.length()) {
                    followUps.add(fArr.getString(j))
                }
            }
            val parentVal = if (c.isNull("parentConceptId")) null else c.optString("parentConceptId").takeIf { it.isNotBlank() }

            concepts.add(
                PhraseNodeEntity(
                    conceptId = c.getString("conceptId"),
                    parentConceptId = parentVal,
                    locale = metadata.locale,
                    text = c.getString("text"),
                    iconEmoji = c.optString("iconEmoji", "💬"),
                    nodeType = try {
                        NodeType.valueOf(c.optString("nodeType", "LEAF_PHRASE"))
                    } catch (_: Exception) {
                        NodeType.LEAF_PHRASE
                    },
                    isAutoSpeak = c.optBoolean("isAutoSpeak", false),
                    isPinned = c.optBoolean("isPinned", false),
                    isCustom = false,
                    sortOrder = c.optInt("sortOrder", i),
                    defaultTimeAffinity = c.optString("defaultTimeAffinity", "ANY"),
                    followUpConceptIds = followUps.joinToString(",")
                )
            )
        }

        // Parse ngrams
        val ngramsMap = mutableMapOf<String, List<String>>()
        root.optJSONObject("ngrams")?.let { ngObj ->
            ngObj.keys().forEach { ctxKey ->
                val arr = ngObj.getJSONArray(ctxKey)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                ngramsMap[ctxKey] = list
            }
        }

        // Parse core vocabulary
        val coreVocab = mutableListOf<String>()
        root.optJSONArray("coreVocabulary")?.let { vocabArr ->
            for (i in 0 until vocabArr.length()) {
                coreVocab.add(vocabArr.getString(i))
            }
        }

        val pack = LanguagePackData(
            metadata = metadata,
            concepts = concepts,
            ngrams = ngramsMap,
            coreVocabulary = coreVocab
        )
        cachedPacks[locale] = pack
        return pack
    }
}

/**
 * Resolves grammatical slot templates ({grammar:tired}, {gender:male|female|neutral},
 * {formality:polite|casual}), handles elisions, and normalizes diacritics for prefix matching.
 */
object SentenceComposer {
    private val grammarTagRegex = "\\{grammar:([a-zA-Z0-9_]+)\\}".toRegex()
    private val inlineGenderRegex = "\\{gender:([^|}]+)\\|([^|}]+)(?:\\|([^}]+))?\\}".toRegex()
    private val inlineFormalityRegex = "\\{formality:([^|}]+)\\|([^}]+)\\}".toRegex()

    fun resolveTemplate(
        rawText: String,
        metadata: LanguageMetadata,
        gender: SpeakerGender = SpeakerGender.NEUTRAL,
        formality: Formality = Formality.POLITE
    ): String {
        var resolved = grammarTagRegex.replace(rawText) { matchResult ->
            val key = matchResult.groupValues[1]
            val forms = metadata.grammarForms[key]
            forms?.get(gender.name)
                ?: forms?.get(formality.name)
                ?: forms?.get("NEUTRAL")
                ?: forms?.get("POLITE")
                ?: key
        }
        resolved = inlineGenderRegex.replace(resolved) { matchResult ->
            val male = matchResult.groupValues[1]
            val female = matchResult.groupValues[2]
            val neutral = matchResult.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() } ?: male
            when (gender) {
                SpeakerGender.MALE -> male
                SpeakerGender.FEMALE -> female
                SpeakerGender.NEUTRAL -> neutral
            }
        }
        resolved = inlineFormalityRegex.replace(resolved) { matchResult ->
            val polite = matchResult.groupValues[1]
            val casual = matchResult.groupValues[2]
            when (formality) {
                Formality.POLITE -> polite
                Formality.CASUAL -> casual
            }
        }
        return resolved
    }

    fun joinTokens(
        tokens: List<String>,
        currentTypingWord: String,
        metadata: LanguageMetadata,
        gender: SpeakerGender = SpeakerGender.NEUTRAL,
        formality: Formality = Formality.POLITE
    ): String {
        val allParts = if (currentTypingWord.isNotBlank()) tokens + currentTypingWord else tokens
        if (allParts.isEmpty()) return ""
        var joined = allParts.joinToString(metadata.wordSeparator).trim()
        joined = resolveTemplate(joined, metadata, gender, formality)

        // Apply Romance-language elision rules (e.g. French "de aller" -> "d'aller")
        if (metadata.locale == "fr") {
            joined = joined
                .replace("\\bde ([aeiouéèêàâîôûh])".toRegex(RegexOption.IGNORE_CASE), "d'$1")
                .replace("\\bde de ".toRegex(RegexOption.IGNORE_CASE), "de ")
        }
        return joined
    }

    /**
     * Strips diacritical marks so prefix matching works across accented characters
     * (e.g. typing 'E' matches both 'eat' and 'écrire', typing 'N' matches 'niño').
     */
    fun normalizeForMatch(text: String): String {
        val normalized = Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}
