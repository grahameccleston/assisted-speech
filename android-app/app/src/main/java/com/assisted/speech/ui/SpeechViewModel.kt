package com.assisted.speech.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assisted.speech.data.*
import com.assisted.speech.tts.SpeechManager
import com.assisted.speech.tts.TtsVoiceOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class SpeechUiState(
    val locale: String = "en",
    val availableLocales: List<Pair<String, String>> = listOf("en" to "English"),
    val languageMetadata: LanguageMetadata = LanguageMetadata(),
    val isTtsVoiceReady: Boolean = true,
    val availableVoices: List<TtsVoiceOption> = emptyList(),
    val activeVoiceId: String? = null,
    val motorSettings: MotorAccessSettings = MotorAccessSettings(),
    val isCaregiverUnlocked: Boolean = false,
    val isSpellingOpen: Boolean = false,

    // Tree & Paged Grid State
    val allNodes: List<PhraseNodeEntity> = emptyList(),
    val navigationStack: List<PhraseNodeEntity> = emptyList(),
    val rankedTierNodes: List<PhraseNodeEntity> = emptyList(),
    val smartShortcuts: List<SmartShortcut> = emptyList(),
    val currentPageIndex: Int = 0,
    val pageSize: Int = 6,

    // Auto-Scan / 1-Switch Highlight State
    val scanFocusIndex: Int = 0,

    // Sentence Accumulator & Repeat State
    val sentenceTokens: List<String> = emptyList(),
    val selectedConceptIds: List<String> = emptyList(),
    val currentTypingWord: String = "",
    val lastSpokenSentence: String = "",
    val statusBannerMessage: String? = null,

    // Unified Spelling / Predictor State
    val predictions: List<String> = emptyList(),
    val letterHighlights: Map<String, LetterHighlight> = emptyMap(),
    val keyboardPageIndex: Int = 0
) {
    val displaySentence: String
        get() = SentenceComposer.joinTokens(
            tokens = sentenceTokens,
            currentTypingWord = currentTypingWord,
            metadata = languageMetadata,
            gender = motorSettings.speakerGender,
            formality = motorSettings.formality
        )

    val isAtRoot: Boolean
        get() = navigationStack.isEmpty()

    val breadcrumbLabels: List<String>
        get() = navigationStack.map {
            SentenceComposer.resolveTemplate(
                rawText = it.text,
                metadata = languageMetadata,
                gender = motorSettings.speakerGender,
                formality = motorSettings.formality
            )
        }

    val totalPages: Int
        get() = if (rankedTierNodes.isEmpty()) 1 else ((rankedTierNodes.size + pageSize - 1) / pageSize)

    val currentPagedNodes: List<PhraseNodeEntity>
        get() {
            if (rankedTierNodes.isEmpty()) return emptyList()
            val safePage = currentPageIndex.coerceIn(0, totalPages - 1)
            val from = safePage * pageSize
            val to = minOf(from + pageSize, rankedTierNodes.size)
            return rankedTierNodes.subList(from, to)
        }

    val totalKeyboardPages: Int
        get() = languageMetadata.keyboardPages.size.coerceAtLeast(1)

    val currentKeyboardPageLetters: List<String>
        get() {
            val pages = languageMetadata.keyboardPages
            if (pages.isEmpty()) return emptyList()
            val safeIdx = keyboardPageIndex.coerceIn(0, pages.size - 1)
            return pages[safeIdx]
        }

    /**
     * If the last typed character in [currentTypingWord] has diacritic variants in the active
     * language pack (e.g. 'a' -> ['á'], 'e' -> ['é', 'è'], 'n' -> ['ñ']), returns them for 1-tap upgrade.
     */
    val activeDiacriticVariants: List<String>
        get() {
            if (currentTypingWord.isEmpty()) return emptyList()
            val lastChar = currentTypingWord.last()
            val baseUpper = SentenceComposer.normalizeForMatch(lastChar.toString()).uppercase()
            val variants = languageMetadata.diacritics[baseUpper].orEmpty()
            return if (lastChar.isUpperCase()) {
                variants.map { it.uppercase() }
            } else {
                variants.map { it.lowercase() }
            }
        }

    val nodesWithChildrenIds: Set<String>
        get() = allNodes.mapNotNull { it.parentConceptId }.toSet()

    /**
     * Total scannable slots on the current phrase screen:
     * [0 .. smartShortcuts.size-1] = Smart Shortcuts
     * [smartShortcuts.size .. smartShortcuts.size + currentPagedNodes.size - 1] = Paged Grid Tiles
     * [last] = Next Page (if totalPages > 1)
     */
    val totalScannableItems: Int
        get() = smartShortcuts.size + currentPagedNodes.size + (if (totalPages > 1) 1 else 0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SpeechViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val prefs = appContext.getSharedPreferences("assisted_speech_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(appContext, viewModelScope)
    private val phraseDao = database.phraseDao()

    private val availableLocalesList = LanguagePackLoader.getAvailableLocales(appContext)
    private val selectedLocaleFlow = MutableStateFlow(prefs.getString("active_locale", "en") ?: "en")

    private var currentLanguagePack: LanguagePackData =
        LanguagePackLoader.load(appContext, selectedLocaleFlow.value)
    private var cachedUtterances: List<UtteranceLogEntity> = emptyList()
    private var cachedUserNgrams: List<UserNgramEntity> = emptyList()

    private val _uiState = MutableStateFlow(
        SpeechUiState(
            locale = currentLanguagePack.metadata.locale,
            availableLocales = availableLocalesList,
            languageMetadata = currentLanguagePack.metadata,
            motorSettings = loadMotorSettings()
        )
    )
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()

    private val speechManager = SpeechManager(appContext) { isVoiceReady, voices, activeVoice ->
        _uiState.update {
            it.copy(
                isTtsVoiceReady = isVoiceReady,
                availableVoices = voices,
                activeVoiceId = activeVoice
            )
        }
    }

    init {
        val initSettings = _uiState.value.motorSettings
        speechManager.configureVoice(
            localeTag = currentLanguagePack.metadata.locale,
            gender = initSettings.speakerGender,
            specificVoiceId = initSettings.selectedVoiceId,
            rate = initSettings.speechRate,
            pitch = initSettings.speechPitch
        )

        // Reactively observe the active locale's nodes, utterance history, and learned N-grams
        viewModelScope.launch {
            selectedLocaleFlow.flatMapLatest { loc ->
                combine(
                    phraseDao.observeNodesForLocale(loc),
                    phraseDao.observeRecentUtterances(loc),
                    phraseDao.observeUserNgrams(loc)
                ) { nodes, utterances, ngrams ->
                    Triple(nodes, utterances, ngrams)
                }
            }.collect { (nodes, utterances, ngrams) ->
                cachedUtterances = utterances
                cachedUserNgrams = ngrams
                _uiState.update { it.copy(allNodes = nodes) }
                recomputeAdaptiveState()
            }
        }
    }

    fun switchLanguage(localeCode: String) {
        if (localeCode == _uiState.value.locale) return
        prefs.edit().putString("active_locale", localeCode).apply()
        currentLanguagePack = LanguagePackLoader.load(appContext, localeCode)
        val resetVoiceSettings = _uiState.value.motorSettings.copy(selectedVoiceId = null)
        prefs.edit().remove("selected_voice_id").apply()
        speechManager.configureVoice(
            localeTag = localeCode,
            gender = resetVoiceSettings.speakerGender,
            specificVoiceId = null,
            rate = resetVoiceSettings.speechRate,
            pitch = resetVoiceSettings.speechPitch
        )

        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.seedLocaleIfNeeded(appContext, phraseDao, localeCode)
            selectedLocaleFlow.value = localeCode
        }

        _uiState.update {
            it.copy(
                locale = localeCode,
                languageMetadata = currentLanguagePack.metadata,
                motorSettings = resetVoiceSettings,
                navigationStack = emptyList(),
                sentenceTokens = emptyList(),
                selectedConceptIds = emptyList(),
                currentTypingWord = "",
                currentPageIndex = 0,
                keyboardPageIndex = 0
            )
        }
        recomputeAdaptiveState()
    }

    fun launchTtsInstaller() {
        speechManager.launchVoiceDataInstaller(appContext)
    }

    fun previewVoice() {
        val sample = when (_uiState.value.locale) {
            "es" -> "Hola, esta es mi voz"
            "fr" -> "Bonjour, voici ma voix"
            else -> "Hello, this is my voice"
        }
        speechManager.speak(sample)
    }

    private fun loadMotorSettings(): MotorAccessSettings {
        val debounce = prefs.getLong("debounce_ms", 350L)
        val minHold = prefs.getLong("min_hold_ms", 70L)
        val handedness = try {
            Handedness.valueOf(prefs.getString("handedness", Handedness.BOTTOM.name) ?: Handedness.BOTTOM.name)
        } catch (_: Exception) {
            Handedness.BOTTOM
        }
        val autoSpeak = prefs.getBoolean("auto_speak", true)
        val gender = try {
            SpeakerGender.valueOf(prefs.getString("speaker_gender", SpeakerGender.NEUTRAL.name) ?: SpeakerGender.NEUTRAL.name)
        } catch (_: Exception) {
            SpeakerGender.NEUTRAL
        }
        val formality = try {
            Formality.valueOf(prefs.getString("formality", Formality.POLITE.name) ?: Formality.POLITE.name)
        } catch (_: Exception) {
            Formality.POLITE
        }
        val rate = prefs.getFloat("speech_rate", 0.85f)
        val pitch = prefs.getFloat("speech_pitch", 1.0f)
        val voiceId = prefs.getString("selected_voice_id", null)
        val autoScan = prefs.getBoolean("auto_scan", false)
        val scanInterval = prefs.getLong("scan_interval_ms", 2200L)

        return MotorAccessSettings(
            debounceMs = debounce,
            minHoldMs = minHold,
            handedness = handedness,
            autoSpeakComplete = autoSpeak,
            speakerGender = gender,
            formality = formality,
            speechRate = rate,
            speechPitch = pitch,
            selectedVoiceId = voiceId,
            autoScanEnabled = autoScan,
            scanIntervalMs = scanInterval
        )
    }

    fun updateMotorSettings(newSettings: MotorAccessSettings, playVoiceSample: Boolean = false) {
        prefs.edit()
            .putLong("debounce_ms", newSettings.debounceMs)
            .putLong("min_hold_ms", newSettings.minHoldMs)
            .putString("handedness", newSettings.handedness.name)
            .putBoolean("auto_speak", newSettings.autoSpeakComplete)
            .putString("speaker_gender", newSettings.speakerGender.name)
            .putString("formality", newSettings.formality.name)
            .putFloat("speech_rate", newSettings.speechRate)
            .putFloat("speech_pitch", newSettings.speechPitch)
            .putString("selected_voice_id", newSettings.selectedVoiceId)
            .putBoolean("auto_scan", newSettings.autoScanEnabled)
            .putLong("scan_interval_ms", newSettings.scanIntervalMs)
            .apply()

        speechManager.configureVoice(
            localeTag = _uiState.value.locale,
            gender = newSettings.speakerGender,
            specificVoiceId = newSettings.selectedVoiceId,
            rate = newSettings.speechRate,
            pitch = newSettings.speechPitch
        )
        _uiState.update { it.copy(motorSettings = newSettings, scanFocusIndex = 0) }
        recomputeAdaptiveState()

        if (playVoiceSample) {
            previewVoice()
        }
    }

    fun toggleCaregiverMode() {
        _uiState.update {
            it.copy(
                isCaregiverUnlocked = !it.isCaregiverUnlocked,
                isSpellingOpen = false,
                statusBannerMessage = null
            )
        }
    }

    fun toggleSpellingMode() {
        _uiState.update {
            it.copy(
                isSpellingOpen = !it.isSpellingOpen,
                keyboardPageIndex = 0,
                statusBannerMessage = null
            )
        }
        recomputeAdaptiveState()
    }

    fun nextGridPage() {
        _uiState.update { state ->
            val next = if (state.currentPageIndex + 1 < state.totalPages) state.currentPageIndex + 1 else 0
            state.copy(currentPageIndex = next, scanFocusIndex = 0)
        }
    }

    fun prevGridPage() {
        _uiState.update { state ->
            val prev = if (state.currentPageIndex - 1 >= 0) state.currentPageIndex - 1 else (state.totalPages - 1).coerceAtLeast(0)
            state.copy(currentPageIndex = prev, scanFocusIndex = 0)
        }
    }

    fun nextKeyboardPage() {
        _uiState.update { state ->
            val next = (state.keyboardPageIndex + 1) % state.totalKeyboardPages
            state.copy(keyboardPageIndex = next)
        }
    }

    // ==========================================
    // 1-SWITCH / AUTO-SCAN STEP SCANNING
    // ==========================================

    fun advanceScanFocus() {
        _uiState.update { state ->
            val total = state.totalScannableItems
            if (total <= 0) state.copy(scanFocusIndex = 0)
            else state.copy(scanFocusIndex = (state.scanFocusIndex + 1) % total)
        }
    }

    fun triggerScannedItem() {
        val state = _uiState.value
        val idx = state.scanFocusIndex
        val shortcutsCount = state.smartShortcuts.size
        val pagedNodes = state.currentPagedNodes

        when {
            idx < shortcutsCount -> {
                onSmartShortcutSelected(state.smartShortcuts[idx])
            }
            idx < shortcutsCount + pagedNodes.size -> {
                onTreeNodeSelected(pagedNodes[idx - shortcutsCount])
            }
            else -> {
                nextGridPage()
            }
        }
    }

    /**
     * 1-Tap Smart Shortcut activation:
     * Immediately speaks the complete predicted sentence, logs usage across all nodes in its path,
     * updates learned N-grams, and resets to Root for the next utterance.
     */
    fun onSmartShortcutSelected(shortcut: SmartShortcut) {
        val spoken = shortcut.fullText.trim()
        if (spoken.isBlank()) return

        speechManager.speak(spoken)
        recordSpokenUtterance(spoken, shortcut.conceptPath)

        _uiState.update {
            it.copy(
                lastSpokenSentence = spoken,
                sentenceTokens = emptyList(),
                selectedConceptIds = emptyList(),
                currentTypingWord = "",
                navigationStack = emptyList(),
                currentPageIndex = 0,
                scanFocusIndex = 0,
                isSpellingOpen = false
            )
        }
        recomputeAdaptiveState()
    }

    /**
     * Tapping a PhraseNodeEntity in the paged grid:
     * - If the node has children in the tree, appends its text and navigates into its branch.
     * - If the node is a leaf (or standalone quick response):
     *   - When autoSpeakComplete is enabled, immediately speaks the assembled sentence, logs usage,
     *     and resets the tree to root in 1 press!
     */
    fun onTreeNodeSelected(node: PhraseNodeEntity) {
        val state = _uiState.value
        val resolvedText = SentenceComposer.resolveTemplate(
            rawText = node.text,
            metadata = state.languageMetadata,
            gender = state.motorSettings.speakerGender,
            formality = state.motorSettings.formality
        )

        val updatedTokens = state.sentenceTokens.toMutableList()
        if (state.currentTypingWord.isNotBlank()) {
            updatedTokens.add(state.currentTypingWord)
        }
        updatedTokens.add(resolvedText)

        val updatedConcepts = state.selectedConceptIds + node.conceptId
        val hasChildren = state.nodesWithChildrenIds.contains(node.conceptId)

        if (hasChildren) {
            _uiState.update {
                it.copy(
                    sentenceTokens = updatedTokens,
                    selectedConceptIds = updatedConcepts,
                    currentTypingWord = "",
                    navigationStack = state.navigationStack + node,
                    currentPageIndex = 0,
                    scanFocusIndex = 0
                )
            }
            recomputeAdaptiveState()
        } else {
            val fullSpoken = SentenceComposer.joinTokens(
                tokens = updatedTokens,
                currentTypingWord = "",
                metadata = state.languageMetadata,
                gender = state.motorSettings.speakerGender,
                formality = state.motorSettings.formality
            )

            if (state.motorSettings.autoSpeakComplete && fullSpoken.isNotBlank()) {
                speechManager.speak(fullSpoken)
                recordSpokenUtterance(fullSpoken, updatedConcepts)
                _uiState.update {
                    it.copy(
                        lastSpokenSentence = fullSpoken,
                        sentenceTokens = emptyList(),
                        selectedConceptIds = emptyList(),
                        currentTypingWord = "",
                        navigationStack = emptyList(),
                        currentPageIndex = 0,
                        scanFocusIndex = 0
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        sentenceTokens = updatedTokens,
                        selectedConceptIds = updatedConcepts,
                        currentTypingWord = "",
                        navigationStack = emptyList(),
                        currentPageIndex = 0,
                        scanFocusIndex = 0
                    )
                }
            }
            recomputeAdaptiveState()
        }
    }

    fun onTreeNavigateUp() {
        val state = _uiState.value
        if (state.navigationStack.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    navigationStack = state.navigationStack.dropLast(1),
                    currentPageIndex = 0,
                    scanFocusIndex = 0
                )
            }
            recomputeAdaptiveState()
        }
    }

    fun onTreeResetToRoot() {
        _uiState.update {
            it.copy(
                navigationStack = emptyList(),
                currentPageIndex = 0,
                scanFocusIndex = 0,
                isSpellingOpen = false
            )
        }
        recomputeAdaptiveState()
    }

    fun onPredictWordSelected(word: String) {
        val newTokens = _uiState.value.sentenceTokens + word
        _uiState.update {
            it.copy(
                sentenceTokens = newTokens,
                currentTypingWord = "",
                keyboardPageIndex = 0
            )
        }
        recomputeAdaptiveState()
    }

    fun onLetterTyped(letter: String) {
        val current = _uiState.value.currentTypingWord
        val formatted = if (current.isEmpty() && _uiState.value.sentenceTokens.isEmpty()) {
            letter.uppercase()
        } else {
            letter.lowercase()
        }
        _uiState.update { it.copy(currentTypingWord = current + formatted) }
        recomputeAdaptiveState()
    }

    /**
     * Replaces the last typed letter with its accented diacritic variant (e.g. 'e' -> 'é').
     */
    fun onDiacriticSelected(diacritic: String) {
        val current = _uiState.value.currentTypingWord
        if (current.isEmpty()) return
        val updated = current.dropLast(1) + diacritic
        _uiState.update { it.copy(currentTypingWord = updated) }
        recomputeAdaptiveState()
    }

    fun onNextWordSpace() {
        val typing = _uiState.value.currentTypingWord.trim()
        if (typing.isNotBlank()) {
            _uiState.update {
                it.copy(
                    sentenceTokens = it.sentenceTokens + typing,
                    currentTypingWord = "",
                    keyboardPageIndex = 0
                )
            }
            recomputeAdaptiveState()
        }
    }

    /**
     * 1-Tap Quick Save from Spelling Mode:
     * Saves the currently assembled sentence as a pinned 1-tap phrase on the Main Screen AND speaks it.
     */
    fun saveAndSpeakSpelledSentence() {
        val fullText = _uiState.value.displaySentence.trim()
        if (fullText.isBlank()) return
        addCustomPhraseNode(
            text = fullText,
            parentConceptId = null,
            iconEmoji = "⭐",
            isPinned = true
        )
        speakOrRepeatCurrentSentence()
        _uiState.update { it.copy(statusBannerMessage = "Saved \"$fullText\" to Main Screen!") }
    }

    fun backspaceSentence() {
        val state = _uiState.value
        if (state.currentTypingWord.isNotEmpty()) {
            _uiState.update { it.copy(currentTypingWord = state.currentTypingWord.dropLast(1)) }
        } else if (state.sentenceTokens.isNotEmpty()) {
            val newTokens = state.sentenceTokens.dropLast(1)
            val newConcepts = if (state.selectedConceptIds.isNotEmpty()) state.selectedConceptIds.dropLast(1) else emptyList()
            val newStack = if (state.navigationStack.isNotEmpty()) state.navigationStack.dropLast(1) else emptyList()
            _uiState.update {
                it.copy(
                    sentenceTokens = newTokens,
                    selectedConceptIds = newConcepts,
                    navigationStack = newStack,
                    currentPageIndex = 0,
                    scanFocusIndex = 0
                )
            }
        } else if (state.navigationStack.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    navigationStack = state.navigationStack.dropLast(1),
                    currentPageIndex = 0,
                    scanFocusIndex = 0
                )
            }
        }
        recomputeAdaptiveState()
    }

    fun clearSentence() {
        _uiState.update {
            it.copy(
                sentenceTokens = emptyList(),
                selectedConceptIds = emptyList(),
                currentTypingWord = "",
                navigationStack = emptyList(),
                currentPageIndex = 0,
                scanFocusIndex = 0,
                isSpellingOpen = false
            )
        }
        recomputeAdaptiveState()
    }

    fun speakOrRepeatCurrentSentence() {
        val state = _uiState.value
        val activeSentence = state.displaySentence
        if (activeSentence.isNotBlank()) {
            speechManager.speak(activeSentence)
            recordSpokenUtterance(activeSentence, state.selectedConceptIds)
            _uiState.update {
                it.copy(
                    lastSpokenSentence = activeSentence,
                    sentenceTokens = emptyList(),
                    selectedConceptIds = emptyList(),
                    currentTypingWord = "",
                    navigationStack = emptyList(),
                    currentPageIndex = 0,
                    scanFocusIndex = 0,
                    isSpellingOpen = false
                )
            }
            recomputeAdaptiveState()
        } else if (state.lastSpokenSentence.isNotBlank()) {
            speechManager.speak(state.lastSpokenSentence)
        }
    }

    fun ringAttentionBell() {
        speechManager.playAttentionBell()
    }

    private fun recordSpokenUtterance(spokenText: String, conceptIds: List<String>) {
        val now = System.currentTimeMillis()
        val bucket = TimeBucket.current(now).name
        val locale = _uiState.value.locale

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update node usage & time-of-day counters across ALL locales sharing each Concept ID
            conceptIds.distinct().forEach { cid ->
                phraseDao.recordConceptUsageAcrossLocales(cid, now, bucket)
            }

            // 2. Insert Utterance Log
            phraseDao.insertUtteranceLog(
                UtteranceLogEntity(
                    locale = locale,
                    spokenText = spokenText,
                    rootConceptId = conceptIds.firstOrNull(),
                    terminalConceptId = conceptIds.lastOrNull(),
                    conceptPath = conceptIds.joinToString(">"),
                    timestamp = now,
                    timeBucket = bucket
                )
            )

            // 3. Learn personal N-grams from spoken words
            val words = spokenText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.isNotEmpty()) {
                upsertLearnedNgram(locale, "start", words.first(), now)
                for (i in 1 until words.size) {
                    val nextWord = words[i]
                    for (win in 1..minOf(3, i)) {
                        val ctxKey = words.subList(i - win, i).joinToString(" ").lowercase()
                        upsertLearnedNgram(locale, ctxKey, nextWord, now)
                    }
                }
            }
        }
    }

    private suspend fun upsertLearnedNgram(locale: String, contextKey: String, nextWord: String, now: Long) {
        val existing = phraseDao.getUserNgram(locale, contextKey, nextWord)
        if (existing != null) {
            phraseDao.upsertUserNgram(
                existing.copy(
                    usageCount = existing.usageCount + 1,
                    lastUsedAt = now
                )
            )
        } else {
            phraseDao.upsertUserNgram(
                UserNgramEntity(
                    locale = locale,
                    contextKey = contextKey,
                    nextWord = nextWord,
                    usageCount = 1,
                    lastUsedAt = now
                )
            )
        }
    }

    private fun recomputeAdaptiveState() {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val parentConceptId = state.navigationStack.lastOrNull()?.conceptId

        val rankedNodes = PredictionEngine.rankTierNodes(
            allNodes = state.allNodes,
            parentConceptId = parentConceptId,
            recentUtterances = cachedUtterances,
            nowMs = now
        )

        val shortcuts = PredictionEngine.computeSmartShortcuts(
            allNodes = state.allNodes,
            navigationStack = state.navigationStack,
            recentUtterances = cachedUtterances,
            metadata = state.languageMetadata,
            speakerGender = state.motorSettings.speakerGender,
            formality = state.motorSettings.formality,
            nowMs = now
        )

        val predictedWords = PredictionEngine.predictWords(
            sentenceTokens = state.sentenceTokens,
            currentPrefix = state.currentTypingWord,
            pack = currentLanguagePack,
            userNgrams = cachedUserNgrams,
            allNodes = state.allNodes
        )

        val letterHighlights = PredictionEngine.computeLetterHighlights(
            sentenceTokens = state.sentenceTokens,
            currentPrefix = state.currentTypingWord,
            pack = currentLanguagePack,
            userNgrams = cachedUserNgrams,
            allNodes = state.allNodes
        )

        _uiState.update {
            val maxPage = if (rankedNodes.isEmpty()) 0 else ((rankedNodes.size + it.pageSize - 1) / it.pageSize) - 1
            it.copy(
                rankedTierNodes = rankedNodes,
                smartShortcuts = shortcuts,
                predictions = predictedWords,
                letterHighlights = letterHighlights,
                currentPageIndex = it.currentPageIndex.coerceIn(0, maxPage.coerceAtLeast(0))
            )
        }
    }

    // ==========================================
    // CAREGIVER CUSTOMIZATION & BACKUP/RESTORE
    // ==========================================

    fun addCustomPhraseNode(
        text: String,
        parentConceptId: String?,
        iconEmoji: String,
        isPinned: Boolean,
        timeAffinity: String = "ANY"
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val locale = _uiState.value.locale
        val conceptId = "custom_${System.currentTimeMillis()}"

        viewModelScope.launch(Dispatchers.IO) {
            val node = PhraseNodeEntity(
                conceptId = conceptId,
                parentConceptId = parentConceptId,
                locale = locale,
                text = cleanText,
                iconEmoji = iconEmoji.ifBlank { "💬" },
                nodeType = if (parentConceptId == null) NodeType.QUICK_PHRASE else NodeType.LEAF_PHRASE,
                isAutoSpeak = true,
                isPinned = isPinned,
                isCustom = true,
                sortOrder = if (isPinned) 7 else 50,
                defaultTimeAffinity = timeAffinity
            )
            phraseDao.insertOrUpdateNode(node)
        }
    }

    fun toggleNodePinned(node: PhraseNodeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            phraseDao.setConceptPinnedAllLocales(node.conceptId, !node.isPinned)
        }
    }

    fun deleteCustomNode(node: PhraseNodeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            phraseDao.deleteNode(node)
        }
    }

    /**
     * Serializes the user's motor settings, custom vocabulary, and learned concept priorities to JSON
     * so caregivers can back up or transfer a user's personalized setup.
     */
    fun exportProfileToJson(): String {
        val state = _uiState.value
        val s = state.motorSettings
        val root = JSONObject()
        root.put("version", 2)
        root.put("locale", state.locale)

        val settingsJson = JSONObject().apply {
            put("debounceMs", s.debounceMs)
            put("minHoldMs", s.minHoldMs)
            put("handedness", s.handedness.name)
            put("autoSpeakComplete", s.autoSpeakComplete)
            put("speakerGender", s.speakerGender.name)
            put("formality", s.formality.name)
            put("speechRate", s.speechRate.toDouble())
            put("autoScanEnabled", s.autoScanEnabled)
            put("scanIntervalMs", s.scanIntervalMs)
        }
        root.put("motorSettings", settingsJson)

        val nodesArray = JSONArray()
        state.allNodes.filter { it.isCustom || it.usageCount > 0 || it.isPinned }.forEach { n ->
            val obj = JSONObject().apply {
                put("conceptId", n.conceptId)
                put("parentConceptId", n.parentConceptId ?: JSONObject.NULL)
                put("locale", n.locale)
                put("text", n.text)
                put("iconEmoji", n.iconEmoji)
                put("isPinned", n.isPinned)
                put("isCustom", n.isCustom)
                put("usageCount", n.usageCount)
                put("lastUsedAt", n.lastUsedAt)
                put("morningCount", n.morningCount)
                put("afternoonCount", n.afternoonCount)
                put("eveningCount", n.eveningCount)
                put("nightCount", n.nightCount)
            }
            nodesArray.put(obj)
        }
        root.put("nodes", nodesArray)
        return root.toString(2)
    }

    /**
     * Restores motor settings, custom nodes, and learned concept priorities from a JSON backup string.
     */
    fun importProfileFromJson(jsonText: String) {
        try {
            val root = JSONObject(jsonText)
            root.optJSONObject("motorSettings")?.let { ms ->
                val current = _uiState.value.motorSettings
                val restored = current.copy(
                    debounceMs = ms.optLong("debounceMs", current.debounceMs),
                    minHoldMs = ms.optLong("minHoldMs", current.minHoldMs),
                    handedness = try {
                        Handedness.valueOf(ms.optString("handedness", current.handedness.name))
                    } catch (_: Exception) { current.handedness },
                    autoSpeakComplete = ms.optBoolean("autoSpeakComplete", current.autoSpeakComplete),
                    speakerGender = try {
                        SpeakerGender.valueOf(ms.optString("speakerGender", current.speakerGender.name))
                    } catch (_: Exception) { current.speakerGender },
                    formality = try {
                        Formality.valueOf(ms.optString("formality", current.formality.name))
                    } catch (_: Exception) { current.formality },
                    speechRate = ms.optDouble("speechRate", current.speechRate.toDouble()).toFloat(),
                    autoScanEnabled = ms.optBoolean("autoScanEnabled", current.autoScanEnabled),
                    scanIntervalMs = ms.optLong("scanIntervalMs", current.scanIntervalMs)
                )
                updateMotorSettings(restored)
            }

            val nodesArr = root.optJSONArray("nodes")
            if (nodesArr != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (i in 0 until nodesArr.length()) {
                        val n = nodesArr.getJSONObject(i)
                        val cid = n.getString("conceptId")
                        val isCustom = n.optBoolean("isCustom", false)
                        if (isCustom) {
                            val parentVal = if (n.isNull("parentConceptId")) null else n.optString("parentConceptId").takeIf { it.isNotBlank() }
                            phraseDao.insertOrUpdateNode(
                                PhraseNodeEntity(
                                    conceptId = cid,
                                    parentConceptId = parentVal,
                                    locale = n.optString("locale", _uiState.value.locale),
                                    text = n.getString("text"),
                                    iconEmoji = n.optString("iconEmoji", "💬"),
                                    nodeType = if (parentVal == null) NodeType.QUICK_PHRASE else NodeType.LEAF_PHRASE,
                                    isAutoSpeak = true,
                                    isPinned = n.optBoolean("isPinned", true),
                                    isCustom = true,
                                    usageCount = n.optInt("usageCount", 0),
                                    lastUsedAt = n.optLong("lastUsedAt", 0L),
                                    morningCount = n.optInt("morningCount", 0),
                                    afternoonCount = n.optInt("afternoonCount", 0),
                                    eveningCount = n.optInt("eveningCount", 0),
                                    nightCount = n.optInt("nightCount", 0)
                                )
                            )
                        } else {
                            phraseDao.restoreConceptStats(
                                conceptId = cid,
                                isPinned = n.optBoolean("isPinned", false),
                                usageCount = n.optInt("usageCount", 0),
                                lastUsedAt = n.optLong("lastUsedAt", 0L),
                                morningCount = n.optInt("morningCount", 0),
                                afternoonCount = n.optInt("afternoonCount", 0),
                                eveningCount = n.optInt("eveningCount", 0),
                                nightCount = n.optInt("nightCount", 0)
                            )
                        }
                    }
                }
            }
            _uiState.update { it.copy(statusBannerMessage = "Profile restored successfully!") }
        } catch (e: Exception) {
            _uiState.update { it.copy(statusBannerMessage = "Invalid backup JSON format.") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
