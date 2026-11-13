package com.assisted.speech.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assisted.speech.data.*
import com.assisted.speech.tts.SpeechManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppMode {
    PHRASES,
    PREDICTIVE,
    MANAGEMENT
}

data class SpeechUiState(
    val currentMode: AppMode = AppMode.PHRASES,
    // Tree navigation stack for Phrases Mode
    // Empty stack means at Root (Sentence Starters & Quick Chat)
    val navigationStack: List<PhraseTreeData.PhraseNode> = emptyList(),
    val sentenceTokens: List<String> = emptyList(),
    val currentTypingWord: String = "", // Partial word being typed via alphabet keys
    val customPhrases: List<PhraseItem> = emptyList(),
    val predictions: List<String> = emptyList()
) {
    /**
     * The full sentence to display and speak: completed tokens plus any currently typed word.
     */
    val displaySentence: String
        get() {
            val allParts = if (currentTypingWord.isNotBlank()) sentenceTokens + currentTypingWord else sentenceTokens
            return allParts.joinToString(" ").trim()
        }

    /**
     * Currently displayed options in Phrases Mode based on navigation stack.
     */
    val currentTierNodes: List<PhraseTreeData.PhraseNode>
        get() {
            return if (navigationStack.isEmpty()) {
                // Root: Show Starter Branches + Quick Socials
                PhraseTreeData.rootTreeNodes + PhraseTreeData.quickPhrases
            } else {
                navigationStack.last().children
            }
        }

    /**
     * Breadcrumb navigation labels (e.g. ["Can you help me to", "sit up"])
     */
    val breadcrumbLabels: List<String>
        get() = navigationStack.map { it.text }
}

class SpeechViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val phraseDao = database.phraseDao()
    private val speechManager = SpeechManager(application)

    private val _uiState = MutableStateFlow(SpeechUiState())
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()

    init {
        // Observe custom management phrases
        viewModelScope.launch {
            phraseDao.getAllPhrases().collect { list ->
                _uiState.update { it.copy(customPhrases = list) }
            }
        }

        // Initial predictions
        updatePredictions()
    }

    fun setMode(mode: AppMode) {
        _uiState.update { it.copy(currentMode = mode) }
        updatePredictions()
    }

    /**
     * Tapping a node in Phrases tree mode:
     * - Appends the phrase node text to sentence accumulator
     * - If node has children, navigates deeper into that branch (Tier 2, 3, etc.)
     * - If node is a leaf (no children), resets navigation back to root
     */
    fun onTreeNodeSelected(node: PhraseTreeData.PhraseNode) {
        val currentTokens = _uiState.value.sentenceTokens.toMutableList()
        if (_uiState.value.currentTypingWord.isNotBlank()) {
            currentTokens.add(_uiState.value.currentTypingWord)
        }
        currentTokens.add(node.text)

        val nextStack = if (node.children.isNotEmpty()) {
            _uiState.value.navigationStack + node
        } else {
            emptyList() // Leaf reached: return to root
        }

        _uiState.update {
            it.copy(
                sentenceTokens = currentTokens,
                currentTypingWord = "",
                navigationStack = nextStack
            )
        }

        updatePredictions()
    }

    /**
     * Navigates back one tier in the phrase tree hierarchy without deleting the accumulated sentence tokens.
     */
    fun onTreeNavigateUp() {
        val currentStack = _uiState.value.navigationStack
        if (currentStack.isNotEmpty()) {
            _uiState.update { it.copy(navigationStack = currentStack.dropLast(1)) }
        }
    }

    /**
     * Resets phrase tree navigation directly to Root starters.
     */
    fun onTreeResetToRoot() {
        _uiState.update { it.copy(navigationStack = emptyList()) }
    }

    /**
     * Tapping a predicted word replaces whatever prefix was being typed with the chosen word and completes the token with space separation.
     */
    fun onPredictWordSelected(word: String) {
        val newTokens = _uiState.value.sentenceTokens + word
        _uiState.update {
            it.copy(
                sentenceTokens = newTokens,
                currentTypingWord = ""
            )
        }
        updatePredictions()
    }

    /**
     * Typing a letter appends to the current prefix and immediately updates prediction options based on preceding tokens + this prefix.
     */
    fun onLetterTyped(letter: String) {
        val updatedWord = _uiState.value.currentTypingWord + letter
        _uiState.update { it.copy(currentTypingWord = updatedWord) }
        updatePredictions()
    }

    /**
     * Commits the current typing word as a token and advances to next word.
     */
    fun onNextWordSpace() {
        if (_uiState.value.currentTypingWord.isNotBlank()) {
            val newTokens = _uiState.value.sentenceTokens + _uiState.value.currentTypingWord
            _uiState.update {
                it.copy(
                    sentenceTokens = newTokens,
                    currentTypingWord = ""
                )
            }
        }
        updatePredictions()
    }

    /**
     * Backspace deletes one character of the partial word if present, otherwise removes the last full token.
     * Also pops one tier back if navigating.
     */
    fun backspaceSentence() {
        val state = _uiState.value
        if (state.currentTypingWord.isNotEmpty()) {
            val shorter = state.currentTypingWord.dropLast(1)
            _uiState.update { it.copy(currentTypingWord = shorter) }
        } else if (state.sentenceTokens.isNotEmpty()) {
            val shorterTokens = state.sentenceTokens.dropLast(1)
            val shorterStack = if (state.navigationStack.isNotEmpty()) state.navigationStack.dropLast(1) else emptyList()
            _uiState.update {
                it.copy(
                    sentenceTokens = shorterTokens,
                    navigationStack = shorterStack
                )
            }
        }
        updatePredictions()
    }

    fun clearSentence() {
        _uiState.update {
            it.copy(
                sentenceTokens = emptyList(),
                currentTypingWord = "",
                navigationStack = emptyList()
            )
        }
        updatePredictions()
    }

    fun speakCurrentSentence() {
        val sentence = _uiState.value.displaySentence
        if (sentence.isNotBlank()) {
            speechManager.speak(sentence)
        }
    }

    private fun updatePredictions() {
        val state = _uiState.value
        val preds = PredictionEngine.predict(state.sentenceTokens, state.currentTypingWord)
        _uiState.update { it.copy(predictions = preds) }
    }

    fun addCustomPhrase(text: String, category: String, type: PhraseType) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val item = PhraseItem(
                text = text.trim(),
                category = category.trim(),
                type = type,
                iconEmoji = if (type == PhraseType.FRAGMENT) "🧩" else "💬"
            )
            phraseDao.insertPhrase(item)
        }
    }

    fun deletePhrase(phrase: PhraseItem) {
        viewModelScope.launch {
            phraseDao.deletePhrase(phrase)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
